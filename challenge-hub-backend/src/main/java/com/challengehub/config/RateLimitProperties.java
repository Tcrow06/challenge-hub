package com.challengehub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private int publicPerMinute = 100;
    private int authenticatedPerMinute = 300;
    private int loginPerMinute = 10;
    private int registerPerMinute = 5;
    private int uploadUrlPerMinute = 10;
    private int messagingSendPerMinute = 30;

    public int getPublicPerMinute() {
        return publicPerMinute;
    }

    public void setPublicPerMinute(int publicPerMinute) {
        this.publicPerMinute = publicPerMinute;
    }

    public int getAuthenticatedPerMinute() {
        return authenticatedPerMinute;
    }

    public void setAuthenticatedPerMinute(int authenticatedPerMinute) {
        this.authenticatedPerMinute = authenticatedPerMinute;
    }

    public int getLoginPerMinute() {
        return loginPerMinute;
    }

    public void setLoginPerMinute(int loginPerMinute) {
        this.loginPerMinute = loginPerMinute;
    }

    public int getRegisterPerMinute() {
        return registerPerMinute;
    }

    public void setRegisterPerMinute(int registerPerMinute) {
        this.registerPerMinute = registerPerMinute;
    }

    public int getUploadUrlPerMinute() {
        return uploadUrlPerMinute;
    }

    public void setUploadUrlPerMinute(int uploadUrlPerMinute) {
        this.uploadUrlPerMinute = uploadUrlPerMinute;
    }

    public int getMessagingSendPerMinute() {
        return messagingSendPerMinute;
    }

    public void setMessagingSendPerMinute(int messagingSendPerMinute) {
        this.messagingSendPerMinute = messagingSendPerMinute;
    }
}
