package com.hostel.management.repository;

import com.hostel.management.entity.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HostelRepository extends JpaRepository<Hostel, Long> {
    Optional<Hostel> findByOwner_Id(Long ownerId);
    boolean existsByNameIgnoreCase(String name);
}
