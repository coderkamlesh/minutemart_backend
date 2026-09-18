@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "catalog :: events",
                "store :: events"
        }
)
package com.minutemart.quickcommerce.inventory;
