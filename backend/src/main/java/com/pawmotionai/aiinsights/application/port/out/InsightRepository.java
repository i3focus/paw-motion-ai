package com.pawmotionai.aiinsights.application.port.out;

import com.pawmotionai.aiinsights.domain.model.Insight;

public interface InsightRepository {

    Insight save(Insight insight);
}
