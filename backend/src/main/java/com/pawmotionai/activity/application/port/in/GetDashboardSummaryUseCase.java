package com.pawmotionai.activity.application.port.in;

import java.util.UUID;

import com.pawmotionai.activity.application.dto.DashboardSummaryDto;

public interface GetDashboardSummaryUseCase {

    DashboardSummaryDto getSummary(UUID ownerId);
}
