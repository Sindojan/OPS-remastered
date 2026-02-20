package com.sindoflow.ops.customers;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity extends BaseEntity {

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerContactEntity> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerAddressEntity> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerPriceGroupEntity> priceGroups = new ArrayList<>();
}
