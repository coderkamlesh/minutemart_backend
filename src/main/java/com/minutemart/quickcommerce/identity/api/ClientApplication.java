package com.minutemart.quickcommerce.identity.api;

public enum ClientApplication {
    CONSUMER_APP,
    RIDER_APP,
    SELLER_PORTAL,
    OPERATIONS_PORTAL,
    ADMIN_PORTAL;

    public boolean supports(IdentityRole role) {
        return switch (this) {
            case CONSUMER_APP -> role == IdentityRole.CONSUMER;
            case RIDER_APP -> role == IdentityRole.RIDER;
            case SELLER_PORTAL -> role == IdentityRole.SELLER;
            case OPERATIONS_PORTAL -> role == IdentityRole.CATALOG_ADMIN
                    || role == IdentityRole.WAREHOUSE_MANAGER
                    || role == IdentityRole.STORE_MANAGER;
            case ADMIN_PORTAL -> role == IdentityRole.ADMIN;
        };
    }

    public static ClientApplication defaultFor(IdentityRole role) {
        return switch (role) {
            case CONSUMER -> CONSUMER_APP;
            case RIDER -> RIDER_APP;
            case SELLER -> SELLER_PORTAL;
            case ADMIN -> ADMIN_PORTAL;
            case CATALOG_ADMIN, WAREHOUSE_MANAGER, STORE_MANAGER -> OPERATIONS_PORTAL;
        };
    }
}
