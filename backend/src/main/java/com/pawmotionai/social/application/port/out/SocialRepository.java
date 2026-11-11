package com.pawmotionai.social.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.pawmotionai.social.application.dto.ActivityCommentDto;
import com.pawmotionai.social.application.dto.ActivityPostDto;
import com.pawmotionai.social.application.dto.MapPointsDto;

public interface SocialRepository {

    List<ActivityPostDto> findRecentPosts(UUID viewerId, int limit);

    Optional<ActivityPostDto> findPostById(UUID activityId, UUID viewerId);

    Optional<String> findViewerReaction(UUID activityId, UUID ownerId);

    void saveReaction(UUID activityId, UUID ownerId, String type);

    void deleteReaction(UUID activityId, UUID ownerId);

    List<ActivityCommentDto> findComments(UUID activityId);

    ActivityCommentDto addComment(UUID activityId, UUID ownerId, String body);

    MapPointsDto findMapPoints();

    boolean activityExists(UUID activityId);

    Map<String, Long> reactionCounts(UUID activityId);

    long commentCount(UUID activityId);
}
