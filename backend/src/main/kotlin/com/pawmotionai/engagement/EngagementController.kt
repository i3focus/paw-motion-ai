package com.pawmotionai.engagement

import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ownerId
import com.pawmotionai.shared.requireRole
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Gamification")
class EngagementController(private val service: EngagementService) {

    @GetMapping("/pets/{petId}/engagement")
    fun engagement(@PathVariable petId: UUID): PetEngagementDto = service.engagement(petId)

    @GetMapping("/pets/{petId}/badges")
    fun badges(@PathVariable petId: UUID): List<BadgeDto> = service.badges(petId)

    @GetMapping("/engagement/challenges")
    fun challenges(@AuthenticationPrincipal jwt: Jwt): List<ChallengeDto> = service.challenges(jwt.ownerId)

    @PostMapping("/engagement/challenges/{challengeId}/join")
    fun join(@AuthenticationPrincipal jwt: Jwt, @PathVariable challengeId: UUID, @Valid @RequestBody request: JoinChallengeRequest): ChallengeDto =
        service.joinChallenge(jwt.ownerId, challengeId, request.petId!!)

    @GetMapping("/engagement/leaderboard")
    fun leaderboard(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "neighborhood") scope: String): LeaderboardDto {
        if (scope !in setOf("neighborhood", "global")) throw BadRequestException("scope must be neighborhood or global")
        return service.leaderboard(jwt.ownerId, scope)
    }

    @PostMapping("/admin/challenges")
    @ResponseStatus(HttpStatus.CREATED)
    fun createChallenge(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateChallengeRequest): ChallengeDto {
        jwt.requireRole("ADMIN")
        return service.createChallenge(jwt.ownerId, request)
    }
}
