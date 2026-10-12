package com.hostel.management.service;

import com.hostel.management.entity.Announcement;
import com.hostel.management.entity.AnnouncementReaction;
import com.hostel.management.dto.AnnouncementResponse;
import com.hostel.management.repository.AnnouncementRepository;
import com.hostel.management.repository.AnnouncementReactionRepository;
import com.hostel.management.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final SecurityUtils securityUtils;
    private final AnnouncementReactionRepository reactions;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            SecurityUtils securityUtils,
            AnnouncementReactionRepository reactions) {
        this.announcementRepository = announcementRepository;
        this.securityUtils = securityUtils;
        this.reactions = reactions;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAllAnnouncements() {
        List<Announcement> announcements;
        if (securityUtils.isSuperAdmin()) {
            announcements = announcementRepository.findAllByOrderByCreatedAtDesc();
        } else {
            announcements = announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(a -> securityUtils.canAccessHostel(a.getHostelName()))
                    .toList();
        }
        return announcements.stream()
                .map(a -> AnnouncementResponse.from(a,
                        reactions.countByAnnouncement_IdAndReaction(a.getId(), AnnouncementReaction.Reaction.LIKE),
                        reactions.countByAnnouncement_IdAndReaction(a.getId(), AnnouncementReaction.Reaction.DISLIKE),
                        null))
                .toList();
    }

    @Transactional
    public Announcement createAnnouncement(
            String title, String content, String category) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());
        announcement.setCategory(
                category == null || category.isBlank()
                        ? "General"
                        : category.trim());
        announcement.setHostelName(securityUtils.getCurrentUserHostel());

        return announcementRepository.save(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Announcement not found: " + id));

        securityUtils.enforceHostelAccess(announcement.getHostelName());

        reactions.deleteByAnnouncement_Id(id);
        announcementRepository.delete(announcement);
    }
}
