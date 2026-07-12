package com.mymoney.api.config.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VersionControllerTest {

    @Test
    void versionReturnsConfiguredVersion() {
        VersionController controller = new VersionController("1.2.3");

        assertThat(controller.version().getBody()).isEqualTo("1.2.3");
    }
}
