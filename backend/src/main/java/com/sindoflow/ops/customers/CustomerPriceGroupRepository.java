package com.sindoflow.ops.customers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerPriceGroupRepository extends JpaRepository<CustomerPriceGroupEntity, UUID> {

    List<CustomerPriceGroupEntity> findByCustomerId(UUID customerId);
}
