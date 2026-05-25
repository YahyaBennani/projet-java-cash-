package com.exemple.dto.request;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    // Reçu dans la réponse de /auth/login — prouve que le password a été vérifié
    private String preAuthToken;
    private String otpCode;
}
