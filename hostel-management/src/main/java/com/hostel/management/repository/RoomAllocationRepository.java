package com.hostel.management.repository;

import com.hostel.management.entity.RoomAllocation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomAllocationRepository
        extends JpaRepository<RoomAllocation, Long> {
	
	boolean existsByStudent_Id(Long studentId);

    long deleteByStudent_Id(Long studentId);

    List<RoomAllocation> findByActiveTrue();

    List<RoomAllocation> findByRoom_IdAndActiveTrue(Long roomId);

//    List<RoomAllocation> findByStudent_IdAndActiveTrue(Long studentId);

    Optional<RoomAllocation> findByStudent_IdAndActiveTrue(
        Long studentId
    );

    boolean existsByRoom_IdAndBedNumberAndActiveTrue(
        Long roomId,
        Integer bedNumber
    );

    boolean existsByStudent_IdAndActiveTrue(Long studentId);

    long countByActiveTrue();

    long countByRoom_IdAndActiveTrue(Long roomId);
    
   

    long countByStudent_IdAndActiveTrue(Long studentId);
}
