package com.hostel.management.repository;

import com.hostel.management.entity.HostelBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HostelBlockRepository extends JpaRepository<HostelBlock, Long> {
    List<HostelBlock> findByHostelNameIgnoreCaseOrderByNameAsc(String hostelName);
    Optional<HostelBlock> findByHostelNameIgnoreCaseAndNameIgnoreCase(String hostelName, String name);
    boolean existsByHostelNameIgnoreCaseAndNameIgnoreCase(String hostelName, String name);
}
