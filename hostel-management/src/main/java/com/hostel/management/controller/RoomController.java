
package com.hostel.management.controller;

import com.hostel.management.dto.RoomRequest;
import com.hostel.management.dto.RoomResponse;
import com.hostel.management.entity.Room;
import com.hostel.management.service.RoomAllocationService;
import com.hostel.management.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomAllocationService allocationService;

    public RoomController(
            RoomService roomService,
            RoomAllocationService allocationService) {
        this.roomService = roomService;
        this.allocationService = allocationService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRooms(
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        List<Room> rooms = activeOnly
                ? roomService.getActiveRooms()
                : roomService.getAllRooms();

        List<RoomResponse> response = rooms.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(
                toResponse(roomService.getRoomById(id)));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody RoomRequest request) {

        Room room = new Room();
        room.setRoomNumber(request.roomNumber());
        room.setRoomType(request.roomType());
        room.setBlockName(request.blockName());
        room.setCapacity(request.capacity());
        room.setMonthlyRent(request.monthlyRent());
        room.setDescription(request.description());
        room.setHostelName(request.hostelName());

        Room created = roomService.createRoom(room);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {

        Room updated = new Room();
        updated.setRoomNumber(request.roomNumber());
        updated.setRoomType(request.roomType());
        updated.setBlockName(request.blockName());
        updated.setCapacity(request.capacity());
        updated.setMonthlyRent(request.monthlyRent());
        updated.setDescription(request.description());
        updated.setHostelName(request.hostelName());

        return ResponseEntity.ok(
                toResponse(roomService.updateRoom(id, updated)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<RoomResponse> deactivateRoom(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                toResponse(roomService.deactivateRoom(id)));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<RoomResponse> activateRoom(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                toResponse(roomService.activateRoom(id)));
    }

    private RoomResponse toResponse(Room room) {
        long occupied = allocationService.getOccupiedBeds(room.getId());
        long available = Math.max(0L, room.getCapacity() - occupied);

        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getRoomType(),
                room.getBlockName(),
                room.getCapacity(),
                occupied,
                available,
                room.isActive(),
                room.getMonthlyRent(),
                room.getDescription(),
                room.getEffectiveHostelName()
        );
    }
}
