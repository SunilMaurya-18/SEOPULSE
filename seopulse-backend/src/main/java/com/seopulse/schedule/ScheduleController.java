package com.seopulse.schedule;

import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/websites/{websiteId}/schedule")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ScheduleResponse get(
            @PathVariable Long projectId,
            @PathVariable Long websiteId,
            Authentication authentication
    ) {
        return scheduleService.get(projectId, websiteId, currentUserService.getUserId(authentication));
    }

    /** Creates or replaces the website's audit schedule. Pro allows WEEKLY, Agency also DAILY. */
    @PutMapping
    public ScheduleResponse put(
            @PathVariable Long projectId,
            @PathVariable Long websiteId,
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication
    ) {
        return scheduleService.upsert(projectId, websiteId, currentUserService.getUserId(authentication), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long websiteId,
            Authentication authentication
    ) {
        scheduleService.delete(projectId, websiteId, currentUserService.getUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}
