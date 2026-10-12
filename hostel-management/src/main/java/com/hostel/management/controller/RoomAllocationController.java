
package com.hostel.management.controller;

import com.hostel.management.dto.RoomAllocationRequest;
import com.hostel.management.dto.RoomAllocationResponse;
import com.hostel.management.service.RoomAllocationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-allocations")
public class RoomAllocationController {

    private final RoomAllocationService allocationService;

    public RoomAllocationController(
            RoomAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    // Get all active room allocations
    @GetMapping
    public ResponseEntity<List<RoomAllocationResponse>> getAllAllocations() {
        return ResponseEntity.ok(
                allocationService.getAllActiveAllocationResponses());
    }

    // Get one allocation by ID
    @GetMapping("/{id}")
    public ResponseEntity<RoomAllocationResponse> getAllocation(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                allocationService.getAllocationResponseById(id));
    }

    // Get all active allocations for a particular room
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<RoomAllocationResponse>> getRoomAllocations(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(
                allocationService.getRoomAllocationResponses(roomId));
    }

    // Allocate a student to a specific bed
    @PostMapping
    public ResponseEntity<RoomAllocationResponse> allocateRoom(
            @Valid @RequestBody RoomAllocationRequest request) {

        RoomAllocationResponse response =
                allocationService.allocateRoomResponse(
                        request.studentId(),
                        request.roomId(),
                        request.bedNumber());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // Vacate a student's bed
    @PatchMapping("/{id}/vacate")
    public ResponseEntity<RoomAllocationResponse> vacateRoom(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                allocationService.vacateRoomResponse(id));
    }
}
