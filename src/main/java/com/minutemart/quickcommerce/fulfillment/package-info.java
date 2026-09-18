@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "ordering :: events",
                "inventory :: api",
                "store :: api"
        }
)
package com.minutemart.quickcommerce.fulfillment;
