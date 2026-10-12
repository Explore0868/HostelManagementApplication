package com.hostel.management.repository;

import com.hostel.management.entity.AnnouncementReaction;
import com.hostel.management.entity.AnnouncementReaction.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AnnouncementReactionRepository extends JpaRepository<AnnouncementReaction, Long> {
    Optional<AnnouncementReaction> findByAnnouncement_IdAndStudent_Id(Long announcementId, Long studentId);
    long countByAnnouncement_IdAndReaction(Long announcementId, Reaction reaction);
    void deleteByAnnouncement_Id(Long announcementId);
    void deleteByStudent_Id(Long studentId);
    List<AnnouncementReaction> findByStudent_Id(Long studentId);
}
