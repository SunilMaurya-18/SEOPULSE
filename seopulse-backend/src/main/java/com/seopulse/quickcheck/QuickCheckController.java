package com.seopulse.quickcheck;

import com.seopulse.abuse.CaptchaVerifier;
import com.seopulse.common.ratelimit.ClientIp;
import com.seopulse.common.ratelimit.RateLimitProperties;
import com.seopulse.common.ratelimit.RateLimiter;
import com.seopulse.website.service.UrlValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/public/quick-check")
@RequiredArgsConstructor
public class QuickCheckController {

    private final QuickCheckService quickCheckService;
    private final UrlValidator urlValidator;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final CaptchaVerifier captchaVerifier;

    @PostMapping
    public QuickCheckResponse check(
            @Valid @RequestBody QuickCheckRequest request,
            HttpServletRequest servletRequest
    ) {
        String ip = ClientIp.of(servletRequest);
        rateLimiter.enforce("quick-check-ip", ip, rateLimitProperties.getQuickCheck());
        captchaVerifier.verify(request.captchaToken(), ip);

        String url = QuickCheckService.normalize(request.url());
        String host = urlValidator.validateStructure(url).getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        rateLimiter.enforce("quick-check-host", host, rateLimitProperties.getQuickCheckHost());

        return quickCheckService.check(url);
    }

    public record QuickCheckRequest(
            @NotBlank @Size(max = 2048) String url,
            @Size(max = 4096) String captchaToken
    ) {
    }
}
