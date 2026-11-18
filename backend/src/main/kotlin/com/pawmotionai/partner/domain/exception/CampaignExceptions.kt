package com.pawmotionai.partner.domain.exception

class CampaignAccessDeniedException : RuntimeException("Campaign does not belong to the authenticated partner")

class CampaignNotAnalyzedException : RuntimeException("Campaign has not been analyzed yet")

class CampaignNotFoundException : RuntimeException("Campaign not found")

class CampaignStateException(message: String) : IllegalStateException(message)
