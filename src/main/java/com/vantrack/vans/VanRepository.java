package com.vantrack.vans;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface VanRepository extends JpaRepository<Van, UUID>, JpaSpecificationExecutor<Van> {
    boolean existsByPlate(String plate);
}
