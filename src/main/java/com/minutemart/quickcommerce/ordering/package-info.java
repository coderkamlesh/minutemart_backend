@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "cart :: api",
                "customer :: api",
                "payment :: api"
        }
)
package com.minutemart.quickcommerce.ordering;
