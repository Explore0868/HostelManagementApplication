package com.hostel.management.repository;

import com.hostel.management.entity.Room;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Room r WHERE r.id = :id")
	Optional<Room> findByIdForUpdate(@Param("id") Long id);
    List<Room> findAllByRoomNumberIgnoreCase(String roomNumber);

    boolean existsByRoomNumberIgnoreCase(String roomNumber);

    List<Room> findByActiveTrue();

    long countByActiveTrue();
    @Query("""
    	    SELECT COALESCE(SUM(r.capacity), 0)
    	    FROM Room r
    	    WHERE r.active = true
    	    """)
    	long sumCapacityOfActiveRooms();
}
