package com.mymoney.api.config.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.mymoney.api.config.api.response.VersionResponse;
import org.junit.jupiter.api.Test;

class VersionControllerTest {

    @Test
    void versionReturnsConfiguredVersion() {
        var controller = new VersionController("1.2.3");

        assertThat(controller.version().getBody()).isEqualTo(new VersionResponse("1.2.3"));
    }
}
