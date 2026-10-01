package com.seopulse.organization.service;

import com.seopulse.billing.EntitlementService;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.organization.entity.Organization;
import com.seopulse.organization.entity.OrganizationBranding;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BrandingService {

    static final int MAX_LOGO_BYTES = 200 * 1024;
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern LOGO = Pattern.compile("^data:image/(png|jpeg);base64,([A-Za-z0-9+/=]+)$");

    private final OrganizationRepository organizationRepository;
    private final OrganizationAccessService accessService;
    private final EntitlementService entitlementService;

    public record BrandingResponse(OrganizationBranding branding, boolean whiteLabelAvailable) {
    }

    @Transactional(readOnly = true)
    public BrandingResponse get(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.VIEWER);
        return new BrandingResponse(requireOrganization(organizationId).getBranding(),
                entitlementService.limitsFor(organizationId).whiteLabel());
    }

    @Transactional
    public BrandingResponse update(Long organizationId, Long userId, OrganizationBranding request) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        entitlementService.requireWhiteLabel(organizationId);

        OrganizationBranding branding = new OrganizationBranding(
                text(request.companyName(), 100, "Company name"),
                color(request.brandColor()),
                text(request.coverText(), 500, "Cover text"),
                logo(request.logoDataUrl())
        );
        Organization organization = requireOrganization(organizationId);
        organization.setBranding(branding);
        return new BrandingResponse(branding, true);
    }

    @Transactional
    public void clear(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        requireOrganization(organizationId).setBranding(null);
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private static String text(String value, int max, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(label + " can be at most " + max + " characters");
        }
        return trimmed;
    }

    private static String color(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!HEX_COLOR.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Brand color must look like #1a2b3c");
        }
        return value.trim().toLowerCase();
    }

    static String logo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var matcher = LOGO.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("The logo must be a PNG or JPEG image");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The logo is not valid base64");
        }
        if (bytes.length > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("The logo can be at most 200 KB");
        }
        boolean png = bytes.length > 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
        boolean jpeg = bytes.length > 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
        if (!png && !jpeg) {
            throw new IllegalArgumentException("The logo must be a PNG or JPEG image");
        }
        return value.trim();
    }
}
