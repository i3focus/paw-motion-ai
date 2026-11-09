package com.pawmotionai.activity.adapter.in.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.activity.application.dto.ActivityResponseDto;
import com.pawmotionai.activity.application.dto.DashboardSummaryDto;
import com.pawmotionai.activity.application.dto.RecordActivityCommand;
import com.pawmotionai.activity.application.port.in.GetDashboardSummaryUseCase;
import com.pawmotionai.activity.application.port.in.ListActivitiesUseCase;
import com.pawmotionai.activity.application.port.in.RecordActivityUseCase;

@RestController
@RequestMapping("/api")
public class ActivityController {

    private final RecordActivityUseCase recordActivityUseCase;
    private final ListActivitiesUseCase listActivitiesUseCase;
    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;

    public ActivityController(RecordActivityUseCase recordActivityUseCase, ListActivitiesUseCase listActivitiesUseCase,
                              GetDashboardSummaryUseCase getDashboardSummaryUseCase) {
        this.recordActivityUseCase = recordActivityUseCase;
        this.listActivitiesUseCase = listActivitiesUseCase;
        this.getDashboardSummaryUseCase = getDashboardSummaryUseCase;
    }

    @PostMapping("/activities")
    public ResponseEntity<ActivityResponseDto> recordActivity(@AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody CreateActivityRequest request) {
        ActivityResponseDto response = recordActivityUseCase.recordActivity(new RecordActivityCommand(
            ownerId(jwt), request.petId(), request.type(), request.startedAt(), request.durationSeconds(),
            request.distanceMeters(), request.notes()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/activities")
    public List<ActivityResponseDto> listActivities(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(required = false) UUID petId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return listActivitiesUseCase.listActivities(ownerId(jwt), petId, from, to);
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryDto dashboardSummary(@AuthenticationPrincipal Jwt jwt) {
        return getDashboardSummaryUseCase.getSummary(ownerId(jwt));
    }

    private UUID ownerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
