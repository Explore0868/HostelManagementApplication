package com.hostel.management.service;

import com.hostel.management.dto.BlockResponse;
import com.hostel.management.entity.HostelBlock;
import com.hostel.management.entity.Room;
import com.hostel.management.repository.HostelBlockRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class BlockService {
    private final HostelBlockRepository blocks;
    private final RoomRepository rooms;
    private final SecurityUtils security;

    public BlockService(HostelBlockRepository blocks, RoomRepository rooms, SecurityUtils security) {
        this.blocks = blocks; this.rooms = rooms; this.security = security;
    }

    @Transactional(readOnly = true)
    public List<BlockResponse> list() {
        String hostel = currentHostel();
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        blocks.findByHostelNameIgnoreCaseOrderByNameAsc(hostel).forEach(b -> names.add(b.getName()));
        rooms.findAll().stream().filter(r -> inHostel(r, hostel)).map(Room::getBlockName)
                .filter(n -> n != null && !n.isBlank()).forEach(names::add);
        return names.stream().map(name -> new BlockResponse(name, countRooms(hostel, name))).toList();
    }

    @Transactional
    public BlockResponse create(String requestedName) {
        String hostel = currentHostel();
        String name = validName(requestedName);
        if (blockExists(hostel, name)) throw new ResponseStatusException(HttpStatus.CONFLICT, "This block already exists in your hostel.");
        blocks.save(new HostelBlock(hostel, name));
        return new BlockResponse(name, 0);
    }

    @Transactional
    public BlockResponse rename(String requestedOldName, String requestedNewName) {
        String hostel = currentHostel();
        String oldName = validName(requestedOldName);
        String newName = validName(requestedNewName);
        boolean exists = blockExists(hostel, oldName);
        if (!exists) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found.");
        if (oldName.equalsIgnoreCase(newName)) return new BlockResponse(oldName, countRooms(hostel, oldName));
        if (blockExists(hostel, newName)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A block with this name already exists.");
        List<Room> associated = rooms.findAll().stream().filter(r -> inHostel(r, hostel)
                && r.getBlockName().equalsIgnoreCase(oldName)).toList();
        blocks.findByHostelNameIgnoreCaseAndNameIgnoreCase(hostel, oldName).ifPresent(b -> b.setName(newName));
        associated.forEach(r -> r.setBlockName(newName));
        rooms.saveAll(associated);
        return new BlockResponse(newName, associated.size());
    }

    @Transactional
    public void delete(String requestedName) {
        String hostel = currentHostel();
        String name = validName(requestedName);
        HostelBlock block = blocks.findByHostelNameIgnoreCaseAndNameIgnoreCase(hostel, name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Only empty registered blocks can be removed."));
        long roomCount = countRooms(hostel, name);
        if (roomCount > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Move rooms out of this block before removing it.");
        blocks.delete(block);
    }

    private boolean blockExists(String hostel, String name) {
        return blocks.existsByHostelNameIgnoreCaseAndNameIgnoreCase(hostel, name)
                || rooms.findAll().stream().anyMatch(r -> inHostel(r, hostel)
                    && r.getBlockName() != null && r.getBlockName().equalsIgnoreCase(name));
    }
    private long countRooms(String hostel, String name) {
        return rooms.findAll().stream().filter(r -> inHostel(r, hostel)
                && r.getBlockName() != null && r.getBlockName().equalsIgnoreCase(name)).count();
    }
    private boolean inHostel(Room room, String hostel) {
        return SecurityUtils.normalizeHostel(hostel).equals(SecurityUtils.normalizeHostel(room.getEffectiveHostelName()));
    }
    private String currentHostel() {
        String hostel = security.getCurrentUserHostel();
        if (hostel == null || hostel.isBlank() || !security.isHostelOwner())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a hostel owner can manage blocks.");
        return hostel;
    }
    private String validName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isBlank() || name.length() > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Block name must contain 1 to 50 characters.");
        return name;
    }
}
