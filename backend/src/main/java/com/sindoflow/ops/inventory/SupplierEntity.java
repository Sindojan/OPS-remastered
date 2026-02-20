package com.sindoflow.ops.inventory;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
public class SupplierEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
