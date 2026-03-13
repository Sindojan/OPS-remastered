package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.common.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SystemApiCredentialService {

    private static final Logger log = LoggerFactory.getLogger(SystemApiCredentialService.class);

    private final SystemApiCredentialRepository credentialRepository;
    private final EncryptionService encryptionService;

    public SystemApiCredentialService(SystemApiCredentialRepository credentialRepository,
                                      EncryptionService encryptionService) {
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public Optional<String> getDecryptedCredential(String serviceName) {
        return credentialRepository.findByServiceName(serviceName)
                .map(c -> encryptionService.decrypt(c.getEncryptedValue()));
    }

    @Transactional
    public SystemApiCredentialEntity saveCredential(String serviceName, String value,
                                                     String description, String metadata) {
        SystemApiCredentialEntity credential = credentialRepository.findByServiceName(serviceName)
                .orElseGet(() -> {
                    SystemApiCredentialEntity c = new SystemApiCredentialEntity();
                    c.setServiceName(serviceName);
                    return c;
                });

        credential.setEncryptedValue(encryptionService.encrypt(value));
        if (description != null) {
            credential.setDescription(description);
        }
        if (metadata != null) {
            credential.setMetadata(metadata);
        }

        log.info("Saved system API credential for service: {}", serviceName);
        return credentialRepository.save(credential);
    }

    @Transactional
    public void deleteCredential(String serviceName) {
        credentialRepository.deleteByServiceName(serviceName);
        log.info("Deleted system API credential for service: {}", serviceName);
    }

    @Transactional(readOnly = true)
    public List<SystemApiCredentialEntity> listAll() {
        return credentialRepository.findAll();
    }
}
