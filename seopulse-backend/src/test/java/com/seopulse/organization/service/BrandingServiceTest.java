package com.seopulse.organization.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandingServiceTest {

    @Test
    void logoMustBeASmallPngOrJpegDataUrl() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0};
        String pngUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);

        assertThat(BrandingService.logo(pngUrl)).isEqualTo(pngUrl);
        assertThat(BrandingService.logo(" ")).isNull();
        assertThatThrownBy(() -> BrandingService.logo("data:image/svg+xml;base64,PHN2Zz4="))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BrandingService.logo("data:image/png;base64,"
                + Base64.getEncoder().encodeToString("not an image".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BrandingService.logo("https://example.com/logo.png"))
                .isInstanceOf(IllegalArgumentException.class);
        byte[] huge = new byte[BrandingService.MAX_LOGO_BYTES + 1];
        huge[0] = (byte) 0xff;
        huge[1] = (byte) 0xd8;
        assertThatThrownBy(() -> BrandingService.logo("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(huge)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
