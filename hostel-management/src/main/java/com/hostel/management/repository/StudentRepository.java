package com.hostel.management.repository;

import com.hostel.management.entity.Student;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository
        extends JpaRepository<Student, Long> {

	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Student s WHERE s.id = :id")
	Optional<Student> findByIdForUpdate(@Param("id") Long id);
    Optional<Student> findByStudentCode(String studentCode);

    List<Student> findAllByStudentCodeIgnoreCase(String studentCode);

    boolean existsByStudentCode(String studentCode);

    Optional<Student> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    List<Student> findByActiveTrue();
    
    boolean existsByStudentCodeAndIdNot(
            String studentCode, Long id);
    
	
	

    long countByActiveTrue();
}
