package com.vantrack.vans;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VanRepository extends JpaRepository<Van, UUID> {
}
