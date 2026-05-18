package com.flowBoard.auth_service.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpStore {

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String otp, int expiryMinutes){
        store.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(expiryMinutes)));
    }

    public boolean verify(String email, String otp){
        OtpEntry entry = store.get(email);
        if(entry==null) return false;
        if(LocalDateTime.now().isAfter(entry.expiry())){
            store.remove(email);
            return false;
        }
        return entry.otp().equals(otp);
    }

    public void delete(String email){
        store.remove(email);
    }

    public boolean exists(String email){
        OtpEntry entry = store.get(email);
        if(entry==null) return false;
        if(LocalDateTime.now().isAfter(entry.expiry())){
            store.remove(email);
            return false;
        }
        return true;
    }

    private record OtpEntry(String otp, LocalDateTime expiry){}
}
