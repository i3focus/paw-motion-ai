package com.pawmotionai.content

import com.pawmotionai.shared.ownerId
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Posts and stories")
class ContentController(private val service: ContentService) {

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreatePostRequest): PostCreatedDto =
        service.createPost(jwt.ownerId, request)

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(@AuthenticationPrincipal jwt: Jwt, @PathVariable postId: UUID) = service.deletePost(jwt.ownerId, postId)

    @GetMapping("/stories")
    fun stories(@AuthenticationPrincipal jwt: Jwt): List<StoryGroupDto> = service.stories(jwt.ownerId)

    @PostMapping("/stories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createStory(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateStoryRequest): StoryDto =
        service.createStory(jwt.ownerId, request)

    @PostMapping("/stories/{storyId}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun viewStory(@AuthenticationPrincipal jwt: Jwt, @PathVariable storyId: UUID) = service.markStoryViewed(jwt.ownerId, storyId)

    @PutMapping("/pets/{petId}/follow")
    fun follow(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID): FollowStateDto = service.follow(jwt.ownerId, petId, true)

    @DeleteMapping("/pets/{petId}/follow")
    fun unfollow(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID): FollowStateDto = service.follow(jwt.ownerId, petId, false)
}
