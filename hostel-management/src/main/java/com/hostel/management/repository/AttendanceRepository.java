
package com.hostel.management.repository;

import com.hostel.management.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    List<Attendance> findAllByOrderByAttendanceDateDesc();

    List<Attendance> findByAttendanceDateOrderByIdDesc(
        LocalDate attendanceDate
    );

    List<Attendance> findByStudent_IdOrderByAttendanceDateDesc(
        Long studentId
    );

    Optional<Attendance> findByStudent_IdAndAttendanceDate(
        Long studentId,
        LocalDate attendanceDate
    );

    long deleteByStudent_Id(Long studentId);
}
