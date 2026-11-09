package com.pawmotionai.identity.application.port.in;

import com.pawmotionai.identity.application.dto.AuthResponseDto;
import com.pawmotionai.identity.application.dto.RegisterPartnerCommand;

public interface RegisterPartnerUseCase {

    AuthResponseDto register(RegisterPartnerCommand command);
}
