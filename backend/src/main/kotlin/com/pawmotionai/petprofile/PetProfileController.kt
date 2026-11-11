package com.pawmotionai.petprofile

import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.ownerId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/pets")
@Tag(name = "Pet profile")
class PetProfileController(private val service: PetProfileService) {

    @GetMapping("/{petId}/profile")
    fun profile(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID): PetProfileDto = service.profile(jwt.ownerId, petId)

    @GetMapping("/{petId}/posts")
    fun posts(@PathVariable petId: UUID, @RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?): PageResponse<PetPostDto> =
        service.posts(petId, PageRequest.of(page, size))

    @GetMapping("/{petId}/adventures")
    fun adventures(@PathVariable petId: UUID, @RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?): PageResponse<AdventureDto> =
        service.adventures(petId, PageRequest.of(page, size))

    @GetMapping("/{petId}/friends")
    fun friends(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID, @RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?): PageResponse<PetFriendDto> =
        service.friends(jwt.ownerId, petId, PageRequest.of(page, size))
}
