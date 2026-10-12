package com.hostel.management.dto;

import com.hostel.management.entity.Announcement;
import com.hostel.management.entity.AnnouncementReaction;
import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        String category,
        String hostelName,
        LocalDateTime createdAt,
        long thumbsUpCount,
        long thumbsDownCount,
        AnnouncementReaction.Reaction myReaction
) {
    public static AnnouncementResponse from(
            Announcement announcement,
            long thumbsUpCount,
            long thumbsDownCount,
            AnnouncementReaction.Reaction myReaction) {
        return new AnnouncementResponse(
                announcement.getId(), announcement.getTitle(), announcement.getContent(),
                announcement.getCategory(), announcement.getHostelName(),
                announcement.getCreatedAt(), thumbsUpCount, thumbsDownCount, myReaction);
    }
}
