package com.sindoflow.ops.customers;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CustomerContactRepository contactRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerPriceGroupRepository priceGroupRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerContactRepository contactRepository,
                           CustomerAddressRepository addressRepository,
                           CustomerPriceGroupRepository priceGroupRepository) {
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.addressRepository = addressRepository;
        this.priceGroupRepository = priceGroupRepository;
    }

    // ── Customer CRUD ──

    @Transactional(readOnly = true)
    public Page<CustomerEntity> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public CustomerEntity findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    @Transactional
    public CustomerEntity create(String companyName, String taxId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setCompanyName(companyName);
        customer.setTaxId(taxId);
        customer.setStatus("ACTIVE");

        log.info("Creating customer: {}", companyName);
        return customerRepository.save(customer);
    }

    @Transactional
    public CustomerEntity update(UUID id, String companyName, String taxId) {
        CustomerEntity customer = findById(id);
        if (companyName != null) {
            customer.setCompanyName(companyName);
        }
        if (taxId != null) {
            customer.setTaxId(taxId);
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public void softDelete(UUID id) {
        CustomerEntity customer = findById(id);
        customer.setStatus("INACTIVE");
        customerRepository.save(customer);
        log.info("Soft-deleted customer: {}", id);
    }

    // ── Contacts ──

    @Transactional
    public CustomerContactEntity addContact(UUID customerId, String firstName, String lastName,
                                            String email, String phone, String position, boolean isPrimary) {
        CustomerEntity customer = findById(customerId);

        CustomerContactEntity contact = new CustomerContactEntity();
        contact.setCustomer(customer);
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setEmail(email);
        contact.setPhone(phone);
        contact.setPosition(position);
        contact.setPrimary(isPrimary);

        log.info("Adding contact {} {} to customer {}", firstName, lastName, customerId);
        return contactRepository.save(contact);
    }

    @Transactional
    public CustomerContactEntity updateContact(UUID customerId, UUID contactId, String firstName,
                                               String lastName, String email, String phone,
                                               String position, Boolean isPrimary) {
        // Verify customer exists
        findById(customerId);

        CustomerContactEntity contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new EntityNotFoundException("Contact not found: " + contactId));

        if (firstName != null) contact.setFirstName(firstName);
        if (lastName != null) contact.setLastName(lastName);
        if (email != null) contact.setEmail(email);
        if (phone != null) contact.setPhone(phone);
        if (position != null) contact.setPosition(position);
        if (isPrimary != null) contact.setPrimary(isPrimary);

        return contactRepository.save(contact);
    }

    @Transactional
    public void removeContact(UUID customerId, UUID contactId) {
        findById(customerId);
        contactRepository.deleteById(contactId);
        log.info("Removed contact {} from customer {}", contactId, customerId);
    }

    // ── Addresses ──

    @Transactional
    public CustomerAddressEntity addAddress(UUID customerId, AddressType type, String street,
                                            String zip, String city, String country) {
        CustomerEntity customer = findById(customerId);

        CustomerAddressEntity address = new CustomerAddressEntity();
        address.setCustomer(customer);
        address.setType(type);
        address.setStreet(street);
        address.setZip(zip);
        address.setCity(city);
        address.setCountry(country != null ? country : "DE");

        log.info("Adding address to customer {}", customerId);
        return addressRepository.save(address);
    }

    @Transactional
    public CustomerAddressEntity updateAddress(UUID customerId, UUID addressId, AddressType type,
                                               String street, String zip, String city, String country) {
        findById(customerId);

        CustomerAddressEntity address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + addressId));

        if (type != null) address.setType(type);
        if (street != null) address.setStreet(street);
        if (zip != null) address.setZip(zip);
        if (city != null) address.setCity(city);
        if (country != null) address.setCountry(country);

        return addressRepository.save(address);
    }

    @Transactional
    public void removeAddress(UUID customerId, UUID addressId) {
        findById(customerId);
        addressRepository.deleteById(addressId);
        log.info("Removed address {} from customer {}", addressId, customerId);
    }

    // ── Price Groups ──

    @Transactional
    public CustomerPriceGroupEntity addPriceGroup(UUID customerId, com.sindoflow.ops.customers.dto.PriceGroupRequest request) {
        CustomerEntity customer = findById(customerId);

        CustomerPriceGroupEntity pg = new CustomerPriceGroupEntity();
        pg.setCustomer(customer);
        pg.setName(request.name());
        pg.setDiscountPercent(request.discountPercent());
        pg.setValidFrom(request.validFrom());
        pg.setValidUntil(request.validUntil());

        log.info("Adding price group '{}' to customer {}", request.name(), customerId);
        return priceGroupRepository.save(pg);
    }

    @Transactional
    public CustomerPriceGroupEntity updatePriceGroup(UUID customerId, UUID priceGroupId,
                                                     com.sindoflow.ops.customers.dto.PriceGroupRequest request) {
        findById(customerId);

        CustomerPriceGroupEntity pg = priceGroupRepository.findById(priceGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Price group not found: " + priceGroupId));

        if (request.name() != null) pg.setName(request.name());
        if (request.discountPercent() != null) pg.setDiscountPercent(request.discountPercent());
        if (request.validFrom() != null) pg.setValidFrom(request.validFrom());
        pg.setValidUntil(request.validUntil());

        return priceGroupRepository.save(pg);
    }

    @Transactional
    public void removePriceGroup(UUID customerId, UUID priceGroupId) {
        findById(customerId);
        priceGroupRepository.deleteById(priceGroupId);
        log.info("Removed price group {} from customer {}", priceGroupId, customerId);
    }
}
