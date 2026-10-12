
package com.hostel.management.repository;

import com.hostel.management.entity.FeeRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {

    List<FeeRecord> findAllByOrderByDueDateAsc();

    List<FeeRecord> findByStudent_IdOrderByDueDateDesc(Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FeeRecord f WHERE f.id = :id")
    Optional<FeeRecord> findByIdForUpdate(@Param("id") Long id);

    boolean existsByStudent_Id(Long studentId);
}
