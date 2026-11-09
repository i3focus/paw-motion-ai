package com.pawmotionai.identity.application.port.in;

import com.pawmotionai.identity.application.dto.AuthResponseDto;
import com.pawmotionai.identity.application.dto.LoginCommand;

public interface AuthenticateOwnerUseCase {

    AuthResponseDto authenticate(LoginCommand command);
}
