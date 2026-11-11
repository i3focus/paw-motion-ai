package com.pawmotionai.assistant

import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.ownerId
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/api/assistant")
@Tag(name = "AI assistant")
class AssistantController(private val service: AssistantService) {

    @GetMapping("/conversations")
    fun conversations(@AuthenticationPrincipal jwt: Jwt, @RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?): PageResponse<ConversationDto> =
        service.conversations(jwt.ownerId, PageRequest.of(page, size))

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @RequestBody(required = false) request: CreateConversationRequest?): ConversationDto =
        service.createConversation(jwt.ownerId, request?.petId)

    @GetMapping("/conversations/{conversationId}/messages")
    fun messages(@AuthenticationPrincipal jwt: Jwt, @PathVariable conversationId: UUID): List<AiMessageDto> =
        service.messages(jwt.ownerId, conversationId)

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Send a message; the reply may carry a PROPOSED action that only runs after explicit confirmation")
    fun send(@AuthenticationPrincipal jwt: Jwt, @PathVariable conversationId: UUID, @Valid @RequestBody request: SendMessageRequest): SendMessageResponse =
        service.send(jwt.ownerId, conversationId, request.content!!)

    @GetMapping("/usage")
    fun usage(@AuthenticationPrincipal jwt: Jwt): AiUsageDto = service.usage(jwt.ownerId)

    @PostMapping("/actions/{actionId}/confirm")
    fun confirm(@AuthenticationPrincipal jwt: Jwt, @PathVariable actionId: UUID): AiActionDto = service.confirm(jwt.ownerId, actionId)

    @PostMapping("/actions/{actionId}/cancel")
    fun cancel(@AuthenticationPrincipal jwt: Jwt, @PathVariable actionId: UUID): AiActionDto = service.cancel(jwt.ownerId, actionId)
}
