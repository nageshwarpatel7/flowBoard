package com.flowBoard.auth_service.security;

import com.flowBoard.auth_service.exception.CustomException;
import com.flowBoard.auth_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpStore otpStore;
    private final EmailService emailService;

    @Value("${otp.expiry-minutes:10}")
    private int expiryMinutes;

    private String generateOtp(){
        SecureRandom random = new SecureRandom();
        int otp = 100000+ random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void sendVerificationOtp(String email){
        String otp = generateOtp();
        otpStore.save(email, otp, expiryMinutes);
        emailService.sendVerificationOtp(email, otp);
    }

    public void sendForgotPasswordOtp(String email){
        String otp = generateOtp();
        otpStore.save(email, otp, expiryMinutes);
        emailService.sendForgotPasswordOtp(email, otp);
    }

    public void verifyOtp(String email, String otp){
        if(!otpStore.verify(email, otp)){
            throw new CustomException("Invalid or expire OTP", HttpStatus.BAD_REQUEST);
        }
        otpStore.delete(email);
    }

    public boolean hasActiveOtp(String email){
        return otpStore.exists(email);
    }
}
