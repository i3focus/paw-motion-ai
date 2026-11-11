package com.pawmotionai.social.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.social.application.dto.ActivityCommentDto;
import com.pawmotionai.social.application.dto.ActivityPostDto;
import com.pawmotionai.social.application.dto.MapPointsDto;
import com.pawmotionai.social.application.port.in.AddActivityCommentUseCase;
import com.pawmotionai.social.application.port.in.ListActivityCommentsUseCase;
import com.pawmotionai.social.application.port.in.ListActivityFeedUseCase;
import com.pawmotionai.social.application.port.in.ListMapPointsUseCase;
import com.pawmotionai.social.application.port.in.ToggleReactionUseCase;
import com.pawmotionai.social.adapter.in.rest.SocialRequests.AddCommentRequest;
import com.pawmotionai.social.adapter.in.rest.SocialRequests.ToggleReactionRequest;

@RestController
public class SocialController {

    private final ListActivityFeedUseCase listActivityFeedUseCase;
    private final ToggleReactionUseCase toggleReactionUseCase;
    private final ListActivityCommentsUseCase listActivityCommentsUseCase;
    private final AddActivityCommentUseCase addActivityCommentUseCase;
    private final ListMapPointsUseCase listMapPointsUseCase;

    public SocialController(ListActivityFeedUseCase listActivityFeedUseCase, ToggleReactionUseCase toggleReactionUseCase,
                            ListActivityCommentsUseCase listActivityCommentsUseCase,
                            AddActivityCommentUseCase addActivityCommentUseCase, ListMapPointsUseCase listMapPointsUseCase) {
        this.listActivityFeedUseCase = listActivityFeedUseCase;
        this.toggleReactionUseCase = toggleReactionUseCase;
        this.listActivityCommentsUseCase = listActivityCommentsUseCase;
        this.addActivityCommentUseCase = addActivityCommentUseCase;
        this.listMapPointsUseCase = listMapPointsUseCase;
    }

    @GetMapping("/api/feed")
    public List<ActivityPostDto> feed(@AuthenticationPrincipal Jwt jwt) {
        return listActivityFeedUseCase.listFeed(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/api/activities/{activityId}/reactions")
    public ActivityPostDto toggleReaction(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID activityId,
                                          @Valid @RequestBody ToggleReactionRequest request) {
        return toggleReactionUseCase.toggleReaction(UUID.fromString(jwt.getSubject()), activityId, request.type());
    }

    @GetMapping("/api/activities/{activityId}/comments")
    public List<ActivityCommentDto> comments(@PathVariable UUID activityId) {
        return listActivityCommentsUseCase.listComments(activityId);
    }

    @PostMapping("/api/activities/{activityId}/comments")
    public ResponseEntity<ActivityCommentDto> addComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID activityId,
                                                         @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(addActivityCommentUseCase.addComment(UUID.fromString(jwt.getSubject()), activityId, request.body()));
    }

    @GetMapping("/api/map/points")
    public MapPointsDto mapPoints() {
        return listMapPointsUseCase.listMapPoints();
    }
}
