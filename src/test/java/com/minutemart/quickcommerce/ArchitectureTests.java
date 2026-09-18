package com.minutemart.quickcommerce;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTests {

    @Test
    void applicationModulesShouldBeValid() {
        ApplicationModules.of(QuickcommerceApplication.class).verify();
    }
}
