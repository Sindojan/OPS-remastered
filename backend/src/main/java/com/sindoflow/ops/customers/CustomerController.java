package com.sindoflow.ops.customers;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.customers.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> list(Pageable pageable) {
        Page<CustomerResponse> page = customerService.findAll(pageable)
                .map(CustomerResponse::fromSummary);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable UUID id) {
        CustomerEntity customer = customerService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponse.from(customer)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerEntity customer = customerService.create(request.companyName(), request.taxId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(CustomerResponse.from(customer), "Customer created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerEntity customer = customerService.update(id, request.companyName(), request.taxId());
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponse.from(customer)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        customerService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Customer deactivated"));
    }

    // ── Contacts ──

    @PostMapping("/{id}/contacts")
    public ResponseEntity<ApiResponse<ContactResponse>> addContact(@PathVariable UUID id,
                                                                   @Valid @RequestBody ContactRequest request) {
        CustomerContactEntity contact = customerService.addContact(
                id, request.firstName(), request.lastName(), request.email(),
                request.phone(), request.position(),
                request.isPrimary() != null ? request.isPrimary() : false);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ContactResponse.from(contact), "Contact added"));
    }

    @PutMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContact(@PathVariable UUID id,
                                                                      @PathVariable UUID contactId,
                                                                      @Valid @RequestBody ContactRequest request) {
        CustomerContactEntity contact = customerService.updateContact(
                id, contactId, request.firstName(), request.lastName(), request.email(),
                request.phone(), request.position(), request.isPrimary());
        return ResponseEntity.ok(ApiResponse.ok(ContactResponse.from(contact)));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<ApiResponse<Void>> removeContact(@PathVariable UUID id,
                                                           @PathVariable UUID contactId) {
        customerService.removeContact(id, contactId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contact removed"));
    }

    // ── Addresses ──

    @PostMapping("/{id}/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@PathVariable UUID id,
                                                                   @Valid @RequestBody AddressRequest request) {
        CustomerAddressEntity address = customerService.addAddress(
                id, request.type(), request.street(), request.zip(), request.city(), request.country());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AddressResponse.from(address), "Address added"));
    }

    @PutMapping("/{id}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@PathVariable UUID id,
                                                                      @PathVariable UUID addressId,
                                                                      @Valid @RequestBody AddressRequest request) {
        CustomerAddressEntity address = customerService.updateAddress(
                id, addressId, request.type(), request.street(), request.zip(), request.city(), request.country());
        return ResponseEntity.ok(ApiResponse.ok(AddressResponse.from(address)));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> removeAddress(@PathVariable UUID id,
                                                           @PathVariable UUID addressId) {
        customerService.removeAddress(id, addressId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Address removed"));
    }
}
