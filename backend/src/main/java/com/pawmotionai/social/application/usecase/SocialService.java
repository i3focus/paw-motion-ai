package com.pawmotionai.social.application.usecase;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.activity.domain.exception.ActivityNotFoundException;
import com.pawmotionai.social.application.dto.ActivityCommentDto;
import com.pawmotionai.social.application.dto.ActivityPostDto;
import com.pawmotionai.social.application.dto.MapPointsDto;
import com.pawmotionai.social.application.port.in.AddActivityCommentUseCase;
import com.pawmotionai.social.application.port.in.ListActivityCommentsUseCase;
import com.pawmotionai.social.application.port.in.ListActivityFeedUseCase;
import com.pawmotionai.social.application.port.in.ListMapPointsUseCase;
import com.pawmotionai.social.application.port.in.ToggleReactionUseCase;
import com.pawmotionai.social.application.port.out.SocialRepository;

@Service
public class SocialService implements ListActivityFeedUseCase, ToggleReactionUseCase, ListActivityCommentsUseCase,
    AddActivityCommentUseCase, ListMapPointsUseCase {

    private static final Set<String> REACTIONS = Set.of("LIKE", "LOVE", "CHEER");
    private static final int FEED_LIMIT = 50;

    private final SocialRepository repository;

    public SocialService(SocialRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityPostDto> listFeed(UUID viewerId) {
        return repository.findRecentPosts(viewerId, FEED_LIMIT);
    }

    @Override
    @Transactional
    public ActivityPostDto toggleReaction(UUID ownerId, UUID activityId, String type) {
        String normalized = normalizeReaction(type);
        requirePost(activityId, ownerId);
        String current = repository.findViewerReaction(activityId, ownerId).orElse(null);
        if (normalized.equals(current)) {
            repository.deleteReaction(activityId, ownerId);
        } else {
            repository.saveReaction(activityId, ownerId, normalized);
        }
        return repository.findPostById(activityId, ownerId)
            .orElseThrow(() -> new ActivityNotFoundException(activityId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityCommentDto> listComments(UUID activityId) {
        requirePostExists(activityId);
        return repository.findComments(activityId);
    }

    @Override
    @Transactional
    public ActivityCommentDto addComment(UUID ownerId, UUID activityId, String body) {
        requirePostExists(activityId);
        String normalized = body == null ? "" : body.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Comment body is required");
        }
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Comment body must be 500 characters or fewer");
        }
        return repository.addComment(activityId, ownerId, normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public MapPointsDto listMapPoints() {
        return repository.findMapPoints();
    }

    private String normalizeReaction(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Reaction type is required");
        }
        String normalized = type.trim().toUpperCase();
        if (!REACTIONS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported reaction type: " + type);
        }
        return normalized;
    }

    private void requirePost(UUID activityId, UUID viewerId) {
        repository.findPostById(activityId, viewerId)
            .orElseThrow(() -> new ActivityNotFoundException(activityId));
    }

    private void requirePostExists(UUID activityId) {
        if (!repository.activityExists(activityId)) {
            throw new ActivityNotFoundException(activityId);
        }
    }
}
