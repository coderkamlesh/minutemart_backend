package com.minutemart.quickcommerce.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "identity.auth")
public class IdentityAuthProperties {

    private Duration otpValidity = Duration.ofMinutes(5);
    private Duration otpResendCooldown = Duration.ofSeconds(30);
    private int otpMaxAttempts = 5;
    private Duration accessTokenValidity = Duration.ofMinutes(15);
    private Duration refreshTokenValidity = Duration.ofDays(30);
    private Duration invitationValidity = Duration.ofDays(2);
    private boolean exposeDevelopmentSecrets;
    private String deliveryMode = "development";
    private String bootstrapAdminPhone;
    private String bootstrapAdminPassword;

    public Duration getOtpValidity() {
        return otpValidity;
    }

    public void setOtpValidity(Duration otpValidity) {
        this.otpValidity = otpValidity;
    }

    public Duration getOtpResendCooldown() {
        return otpResendCooldown;
    }

    public void setOtpResendCooldown(Duration otpResendCooldown) {
        this.otpResendCooldown = otpResendCooldown;
    }

    public int getOtpMaxAttempts() {
        return otpMaxAttempts;
    }

    public void setOtpMaxAttempts(int otpMaxAttempts) {
        this.otpMaxAttempts = otpMaxAttempts;
    }

    public Duration getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(Duration accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    public void setRefreshTokenValidity(Duration refreshTokenValidity) {
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public Duration getInvitationValidity() {
        return invitationValidity;
    }

    public void setInvitationValidity(Duration invitationValidity) {
        this.invitationValidity = invitationValidity;
    }

    public boolean isExposeDevelopmentSecrets() {
        return exposeDevelopmentSecrets;
    }

    public void setExposeDevelopmentSecrets(boolean exposeDevelopmentSecrets) {
        this.exposeDevelopmentSecrets = exposeDevelopmentSecrets;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getBootstrapAdminPhone() {
        return bootstrapAdminPhone;
    }

    public void setBootstrapAdminPhone(String bootstrapAdminPhone) {
        this.bootstrapAdminPhone = bootstrapAdminPhone;
    }

    public String getBootstrapAdminPassword() {
        return bootstrapAdminPassword;
    }

    public void setBootstrapAdminPassword(String bootstrapAdminPassword) {
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }
}
