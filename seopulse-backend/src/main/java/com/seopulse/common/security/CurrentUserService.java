package com.seopulse.common.security;

import com.seopulse.common.exception.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    /**
     * Returns the user ID carried in the JWT subject claim.
     */
    public Long getUserId(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialsException("Authentication is required");
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new InvalidCredentialsException("Authentication is required");
        }
    }
}
