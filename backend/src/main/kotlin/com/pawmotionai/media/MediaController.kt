package com.pawmotionai.media

import com.pawmotionai.feed.api.MediaDto
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.ownerId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media")
class MediaController(private val service: MediaService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a photo or short video (max 10 MB) for posts, stories and alerts")
    fun upload(@AuthenticationPrincipal jwt: Jwt, @RequestPart("file") file: MultipartFile): MediaDto =
        service.upload(jwt.ownerId, file.bytes)

    @GetMapping("/{mediaId}")
    @Operation(summary = "Public media download by unguessable id")
    fun download(@PathVariable mediaId: UUID): ResponseEntity<Resource> {
        val media = service.load(mediaId) ?: throw NotFoundException("Media not found")
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(media.contentType))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
            .header("X-Content-Type-Options", "nosniff")
            .body(media.resource)
    }
}
