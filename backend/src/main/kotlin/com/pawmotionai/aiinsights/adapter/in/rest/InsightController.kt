package com.pawmotionai.aiinsights.adapter.`in`.rest

import com.pawmotionai.aiinsights.application.dto.GenerateInsightCommand
import com.pawmotionai.aiinsights.application.dto.InsightResponseDto
import com.pawmotionai.aiinsights.application.port.`in`.GenerateInsightUseCase
import com.pawmotionai.shared.SecurityRoles
import com.pawmotionai.shared.ownerId
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class GenerateInsightRequest(val petId: UUID?, @field:Min(1) @field:Max(90) val periodDays: Int?)

@RestController
@Secured(SecurityRoles.OWNER)
@RequestMapping("/api/insights")
class InsightController(private val generateInsightUseCase: GenerateInsightUseCase) {

    @PostMapping
    fun generateInsight(@AuthenticationPrincipal jwt: Jwt,
                        @Valid @RequestBody(required = false) request: GenerateInsightRequest?): InsightResponseDto {
        val petId = request?.petId
        val periodDays = request?.periodDays ?: DEFAULT_PERIOD_DAYS
        return generateInsightUseCase.generateInsight(GenerateInsightCommand(jwt.ownerId, petId, periodDays))
    }

    private companion object {
        const val DEFAULT_PERIOD_DAYS = 7
    }
}
