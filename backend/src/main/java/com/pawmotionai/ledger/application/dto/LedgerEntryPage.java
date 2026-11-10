package com.pawmotionai.ledger.application.dto;

import java.util.List;

public record LedgerEntryPage(List<LedgerEntryResponseDto> content, int page, int size, long totalElements,
                              int totalPages) {
}
