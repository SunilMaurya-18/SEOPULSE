package com.seopulse.account;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.common.ratelimit.RateLimitProperties;
import com.seopulse.common.ratelimit.RateLimiter;
import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/account")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserService currentUserService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final AuthProperties authProperties;

    @GetMapping("/export")
    public ResponseEntity<AccountExport> export(Authentication authentication) {
        Long userId = currentUserService.getUserId(authentication);
        rateLimiter.enforce("account-export", String.valueOf(userId), rateLimitProperties.getAccountAction());
        String filename = "seopulse-data-" + LocalDate.now(ZoneOffset.UTC) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(accountService.export(userId));
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request, Authentication authentication) {
        Long userId = currentUserService.getUserId(authentication);
        rateLimiter.enforce("account-delete", String.valueOf(userId), rateLimitProperties.getAccountAction());
        accountService.delete(userId, request.password());
        ResponseCookie clearRefresh = ResponseCookie.from(authProperties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(authProperties.isRefreshCookieSecure())
                .sameSite("Strict")
                .path(authProperties.getRefreshCookiePath())
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearRefresh.toString()).build();
    }

    public record DeleteAccountRequest(@NotBlank String password) {
    }
}
