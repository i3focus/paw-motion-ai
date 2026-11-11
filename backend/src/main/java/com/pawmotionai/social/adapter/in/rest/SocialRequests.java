package com.pawmotionai.social.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SocialRequests {

    private SocialRequests() {
    }

    public record ToggleReactionRequest(@NotBlank @Size(max = 16) String type) {
    }

    public record AddCommentRequest(@NotBlank @Size(max = 500) String body) {
    }
}
