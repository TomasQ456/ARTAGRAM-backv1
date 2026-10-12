package com.artagram.platform.canvas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.canvas")
public class CanvasProperties {

    private int pointBatchMs = 30;
    private int bannerSize = 1000;
    private int bannerRegionSize = 100;
    private int bannerRateLimitPerSecond = 100;
    private int strokeLockTtlSeconds = 30;

    public int getPointBatchMs() {
        return pointBatchMs;
    }

    public void setPointBatchMs(int pointBatchMs) {
        this.pointBatchMs = pointBatchMs;
    }

    public int getBannerSize() {
        return bannerSize;
    }

    public void setBannerSize(int bannerSize) {
        this.bannerSize = bannerSize;
    }

    public int getBannerRegionSize() {
        return bannerRegionSize;
    }

    public void setBannerRegionSize(int bannerRegionSize) {
        this.bannerRegionSize = bannerRegionSize;
    }

    public int getBannerRateLimitPerSecond() {
        return bannerRateLimitPerSecond;
    }

    public void setBannerRateLimitPerSecond(int bannerRateLimitPerSecond) {
        this.bannerRateLimitPerSecond = bannerRateLimitPerSecond;
    }

    public int getStrokeLockTtlSeconds() {
        return strokeLockTtlSeconds;
    }

    public void setStrokeLockTtlSeconds(int strokeLockTtlSeconds) {
        this.strokeLockTtlSeconds = strokeLockTtlSeconds;
    }

    public int regionsPerSide() {
        return bannerSize / bannerRegionSize;
    }
}
