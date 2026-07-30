package com.vantrack;

import com.vantrack.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application context")
class ApplicationContextIT extends IntegrationTestSupport {

    @Test
    @DisplayName("starts up with the migrations applied")
    void shouldStartUpWithMigrationsApplied() {
    }
}
