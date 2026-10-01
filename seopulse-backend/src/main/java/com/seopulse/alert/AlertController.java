package com.seopulse.alert;

import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs/{orgId}/alerts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<AlertRuleResponse> list(@PathVariable Long orgId, Authentication authentication) {
        return alertService.list(orgId, currentUserService.getUserId(authentication));
    }

    /** The 50 most recent alert deliveries, newest first. */
    @GetMapping("/deliveries")
    public List<AlertDeliveryResponse> deliveries(@PathVariable Long orgId, Authentication authentication) {
        return alertService.deliveries(orgId, currentUserService.getUserId(authentication));
    }

    @PostMapping
    public ResponseEntity<AlertRuleResponse> create(
            @PathVariable Long orgId,
            @Valid @RequestBody AlertRuleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alertService.create(orgId, currentUserService.getUserId(authentication), request));
    }

    @PutMapping("/{ruleId}")
    public AlertRuleResponse update(
            @PathVariable Long orgId,
            @PathVariable Long ruleId,
            @Valid @RequestBody AlertRuleRequest request,
            Authentication authentication
    ) {
        return alertService.update(orgId, ruleId, currentUserService.getUserId(authentication), request);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable Long orgId, @PathVariable Long ruleId, Authentication authentication) {
        alertService.delete(orgId, ruleId, currentUserService.getUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ruleId}/test")
    public AlertService.TestAlertResult test(@PathVariable Long orgId, @PathVariable Long ruleId, Authentication authentication) {
        return alertService.sendTest(orgId, ruleId, currentUserService.getUserId(authentication));
    }

    @PostMapping("/{ruleId}/rotate-secret")
    public AlertRuleResponse rotateSecret(@PathVariable Long orgId, @PathVariable Long ruleId, Authentication authentication) {
        return alertService.rotateSecret(orgId, ruleId, currentUserService.getUserId(authentication));
    }
}
