package com.pawmotionai.identity.application.port.in;

import com.pawmotionai.identity.application.dto.AuthResponseDto;
import com.pawmotionai.identity.application.dto.RegisterOwnerCommand;

public interface RegisterOwnerUseCase {

    AuthResponseDto register(RegisterOwnerCommand command);
}
