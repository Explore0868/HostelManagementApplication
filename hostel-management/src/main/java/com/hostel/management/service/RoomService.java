package com.hostel.management.service;

import com.hostel.management.entity.Room;
import com.hostel.management.repository.RoomAllocationRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomAllocationRepository allocationRepository;
    private final SecurityUtils securityUtils;

    public RoomService(
            RoomRepository roomRepository,
            RoomAllocationRepository allocationRepository,
            SecurityUtils securityUtils) {
        this.roomRepository = roomRepository;
        this.allocationRepository = allocationRepository;
        this.securityUtils = securityUtils;
    }

    public List<Room> getAllRooms() {
        if (securityUtils.isSuperAdmin()) {
            return roomRepository.findAll();
        }
        return roomRepository.findAll().stream()
                .filter(r -> securityUtils.canAccessHostel(r.getEffectiveHostelName()))
                .toList();
    }

    public List<Room> getActiveRooms() {
        if (securityUtils.isSuperAdmin()) {
            return roomRepository.findByActiveTrue();
        }
        return roomRepository.findByActiveTrue().stream()
                .filter(r -> securityUtils.canAccessHostel(r.getEffectiveHostelName()))
                .toList();
    }

    public Room getRoomById(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("Invalid room ID.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Room not found with ID: " + roomId));

        securityUtils.enforceHostelAccess(room.getEffectiveHostelName());
        return room;
    }

    @Transactional
    public Room createRoom(Room room) {
        validateRoom(room);

        String roomNumber = room.getRoomNumber().trim();

        String targetHostel = room.getHostelName();
        if ((targetHostel == null || targetHostel.isBlank())
                && !securityUtils.isSuperAdmin()) {
            targetHostel = securityUtils.getCurrentUserHostel();
        }
        if (securityUtils.isSuperAdmin()
                && (targetHostel == null || targetHostel.isBlank())) {
            throw new IllegalArgumentException(
                    "Hostel name is required when a system administrator creates a room.");
        }
        securityUtils.enforceHostelAccess(targetHostel);

        if (roomNumberExistsInHostel(roomNumber, targetHostel, null)) {
            throw new IllegalStateException(
                    "A room with this room number already exists.");
        }

        if (!securityUtils.isSuperAdmin()) {
            room.setHostelName(securityUtils.getCurrentUserHostel());
        } else {
            room.setHostelName(targetHostel.trim());
        }

        room.setRoomNumber(roomNumber);
        room.setActive(true);

        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(Long roomId, Room updatedRoom) {
        validateRoom(updatedRoom);

        Room existingRoom = getRoomById(roomId);
        String existingHostel = existingRoom.getEffectiveHostelName();
        if (updatedRoom.getHostelName() != null && !updatedRoom.getHostelName().isBlank()
                && !SecurityUtils.normalizeHostel(existingHostel).equals(
                        SecurityUtils.normalizeHostel(updatedRoom.getHostelName()))) {
            throw new IllegalArgumentException(
                    "A room cannot be moved to a different hostel through an update.");
        }

        String newRoomNumber = updatedRoom.getRoomNumber().trim();

        if (roomNumberExistsInHostel(
                newRoomNumber, existingHostel, existingRoom.getId())) {
            throw new IllegalStateException(
                    "A room with this room number already exists in this hostel.");
        }

        long occupiedBeds =
                allocationRepository.countByRoom_IdAndActiveTrue(roomId);

        if (updatedRoom.getCapacity() < occupiedBeds) {
            throw new IllegalStateException(
                    "Capacity cannot be less than the number of occupied beds.");
        }

        existingRoom.setRoomNumber(newRoomNumber);
        existingRoom.setRoomType(updatedRoom.getRoomType().trim());
        existingRoom.setBlockName(updatedRoom.getBlockName().trim());
        existingRoom.setMonthlyRent(updatedRoom.getMonthlyRent());
        existingRoom.setDescription(updatedRoom.getDescription());
        existingRoom.setCapacity(updatedRoom.getCapacity());
        if (existingRoom.getHostelName() == null || existingRoom.getHostelName().isBlank()) {
            existingRoom.setHostelName(existingHostel);
        }

        return roomRepository.save(existingRoom);
    }

    @Transactional
    public Room deactivateRoom(Long roomId) {
        Room room = getRoomById(roomId);
        securityUtils.enforceHostelAccess(room.getEffectiveHostelName());

        long occupiedBeds =
                allocationRepository.countByRoom_IdAndActiveTrue(roomId);

        if (occupiedBeds > 0) {
            throw new IllegalStateException(
                    "Vacate all students before deactivating this room.");
        }

        room.setActive(false);
        return roomRepository.save(room);
    }

    @Transactional
    public Room activateRoom(Long roomId) {
        Room room = getRoomById(roomId);
        securityUtils.enforceHostelAccess(room.getEffectiveHostelName());
        room.setActive(true);
        return roomRepository.save(room);
    }

    private void validateRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room details are required.");
        }

        if (room.getRoomNumber() == null
                || room.getRoomNumber().isBlank()) {
            throw new IllegalArgumentException("Room number is required.");
        }

        if (room.getRoomType() == null
                || room.getRoomType().isBlank()) {
            throw new IllegalArgumentException("Room type is required.");
        }

        if (room.getBlockName() == null
                || room.getBlockName().isBlank()) {
            throw new IllegalArgumentException("Block name is required.");
        }

        if (room.getCapacity() == null || room.getCapacity() < 1) {
            throw new IllegalArgumentException(
                    "Room capacity must be at least 1.");
        }
    }

    private boolean roomNumberExistsInHostel(
            String roomNumber, String hostelName, Long excludedRoomId) {
        String requestedHostel = SecurityUtils.normalizeHostel(hostelName);
        return roomRepository.findAllByRoomNumberIgnoreCase(roomNumber).stream()
                .filter(existing -> excludedRoomId == null
                        || !excludedRoomId.equals(existing.getId()))
                .anyMatch(existing -> requestedHostel.equals(
                        SecurityUtils.normalizeHostel(existing.getEffectiveHostelName())));
    }
}
