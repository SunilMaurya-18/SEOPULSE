package com.seopulse.website.controller;

import com.seopulse.common.dto.PageResponse;
import com.seopulse.common.security.CurrentUserService;
import com.seopulse.website.dto.CreateWebsiteRequest;
import com.seopulse.website.dto.WebsiteResponse;
import com.seopulse.website.service.WebsiteService;


import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/websites")
@RequiredArgsConstructor
public class WebsiteController {

    private final WebsiteService websiteService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<WebsiteResponse> createWebsite(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateWebsiteRequest request,
            Authentication authentication
    ) {

        Long userId = currentUserService.getUserId(authentication);
        ;

        WebsiteResponse response =
                websiteService.createWebsite(
                        projectId,
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public PageResponse<WebsiteResponse> getWebsites(
            @PathVariable Long projectId,
            Pageable pageable,
            Authentication authentication
    ) {

        Long userId = currentUserService.getUserId(authentication);

        return websiteService.getWebsites(
                projectId,
                userId,
                pageable
        );
    }

    @GetMapping("/{websiteId}")
    public WebsiteResponse getWebsite(
            @PathVariable Long projectId,
            @PathVariable Long websiteId,
            Authentication authentication
    ) {

        Long userId = currentUserService.getUserId(authentication);

        return websiteService.getWebsite(
                projectId,
                websiteId,
                userId
        );
    }


}