package com.hostel.management.service;

import com.hostel.management.dto.CreateHostelRequest;
import com.hostel.management.dto.HostelResponse;
import com.hostel.management.entity.Hostel;
import com.hostel.management.entity.Room;
import com.hostel.management.entity.Student;
import com.hostel.management.entity.Announcement;
import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.*;
import com.hostel.management.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class HostelManagementService {
    private final HostelRepository hostels;
    private final UserRepository users;
    private final RoomRepository rooms;
    private final StudentRepository students;
    private final AnnouncementRepository announcementRepository;
    private final PasswordEncoder encoder;
    private final SecurityUtils security;
    private final AdminNotificationRepository notifications;
    private final HostelBlockRepository hostelBlocks;
    private final OwnerNotificationRepository ownerNotifications;

    public HostelManagementService(HostelRepository hostels, UserRepository users,
            RoomRepository rooms, StudentRepository students, AnnouncementRepository announcementRepository, PasswordEncoder encoder,
            SecurityUtils security, AdminNotificationRepository notifications,
            HostelBlockRepository hostelBlocks, OwnerNotificationRepository ownerNotifications) {
        this.hostels = hostels; this.users = users; this.rooms = rooms;
        this.students = students; this.encoder = encoder; this.security = security;
        this.announcementRepository = announcementRepository;
        this.notifications = notifications;
        this.hostelBlocks = hostelBlocks;
        this.ownerNotifications = ownerNotifications;
    }

    @Transactional
    public HostelResponse create(CreateHostelRequest request) {
        requireSystemAdmin();
        String name = request.hostelName().trim();
        String email = request.ownerEmail().trim().toLowerCase();
        if (hostels.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A hostel with this name already exists.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        User owner = new User(request.ownerName().trim(), email,
                encoder.encode(request.initialPassword()), UserRole.ADMIN, name);
        owner.setEnabled(true);
        owner.setMustChangePassword(true);
        owner = users.saveAndFlush(owner);
        Hostel hostel = hostels.save(new Hostel(name, clean(request.address()), clean(request.contact()), owner));
        return toResponse(hostel);
    }

    @Transactional(readOnly = true)
    public List<HostelResponse> list() {
        requireSystemAdmin();
        return hostels.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public HostelResponse rename(Long id, String name) {
        requireSystemAdmin();
        Hostel hostel = get(id);
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hostel name is required.");
        if (hostels.existsByNameIgnoreCase(normalized) && !hostel.getName().equalsIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A hostel with this name already exists.");
        }
        String previousName = hostel.getName();
        if (previousName.equals(normalized)) return toResponse(hostel);
        hostel.setName(normalized);
        hostel.getOwner().setHostelName(normalized);
        hostelBlocks.findByHostelNameIgnoreCaseOrderByNameAsc(previousName).forEach(b -> b.setHostelName(normalized));
        List<Room> hostelRooms = rooms.findAll().stream()
                .filter(r -> previousName.equalsIgnoreCase(r.getEffectiveHostelName())).toList();
        hostelRooms.forEach(r -> r.setHostelName(normalized));
        rooms.saveAll(hostelRooms);
        List<Student> hostelStudents = students.findAll().stream()
                .filter(s -> previousName.equalsIgnoreCase(s.getHostelName())).toList();
        hostelStudents.forEach(s -> {
            s.setHostelName(normalized);
            if (s.getUser() != null) s.getUser().setHostelName(normalized);
        });
        students.saveAll(hostelStudents);
        List<Announcement> hostelAnnouncements = announcementRepository.findAll().stream()
                .filter(a -> previousName.equalsIgnoreCase(a.getHostelName())).toList();
        hostelAnnouncements.forEach(a -> a.setHostelName(normalized));
        announcementRepository.saveAll(hostelAnnouncements);
        notifications.save(new com.hostel.management.entity.AdminNotification(hostel, previousName, normalized));
        ownerNotifications.save(new com.hostel.management.entity.OwnerNotification(
                hostel.getOwner().getId(), normalized, "HOSTEL_NAME_CHANGED",
                "Your hostel name changed from " + previousName + " to " + normalized + "."));
        return toResponse(hostel);
    }

    @Transactional
    public HostelResponse updateOwner(Long id, String name, String email) {
        requireSystemAdmin();
        Hostel hostel = get(id);
        User owner = hostel.getOwner();
        if (name != null && !name.isBlank()) owner.setFullName(name.trim());
        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase();
            if (!normalized.equalsIgnoreCase(owner.getEmail()) && users.existsByEmailIgnoreCase(normalized)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
            }
            owner.setEmail(normalized);
        }
        return toResponse(hostel);
    }

    @Transactional
    public HostelResponse setOwnerEnabled(Long id, boolean enabled) {
        requireSystemAdmin();
        Hostel hostel = get(id);
        hostel.getOwner().setEnabled(enabled);
        return toResponse(hostel);
    }

    @Transactional
    public void deleteIfEmpty(Long id, boolean confirmed) {
        requireSystemAdmin();
        if (!confirmed) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Explicit confirmation is required.");
        Hostel hostel = get(id);
        boolean hasRooms = rooms.findAll().stream().anyMatch(r -> hostel.getName().equalsIgnoreCase(r.getEffectiveHostelName()));
        boolean hasStudents = students.findAll().stream().anyMatch(s -> hostel.getName().equalsIgnoreCase(s.getHostelName()));
        if (hasRooms || hasStudents) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This hostel has rooms or students. Remove or reassign its records before deleting it.");
        users.delete(hostel.getOwner());
        hostels.delete(hostel);
    }

    private Hostel get(Long id) {
        return hostels.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hostel not found."));
    }
    private void requireSystemAdmin() {
        if (!security.isSuperAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the System Admin can manage hostels.");
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private HostelResponse toResponse(Hostel h) {
        User owner = h.getOwner();
        List<Room> hostelRooms = rooms.findAll().stream().filter(r -> h.getName().equalsIgnoreCase(r.getEffectiveHostelName())).toList();
        List<String> blocks = hostelRooms.stream().map(Room::getBlockName).filter(v -> v != null && !v.isBlank()).distinct().sorted().toList();
        long roomCount = hostelRooms.size();
        long studentCount = students.findAll().stream().filter(s -> h.getName().equalsIgnoreCase(s.getHostelName())).count();
        return new HostelResponse(h.getId(), h.getPublicId(), h.getName(), h.getAddress(), h.getContact(),
                owner.getId(), owner.getFullName(), owner.getEmail(), owner.isEnabled(), blocks, roomCount, studentCount, h.getCreatedAt());
    }
}
