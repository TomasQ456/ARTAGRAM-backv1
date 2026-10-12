package com.artagram.platform.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.quotas")
public class QuotaProperties {

    private int freeMaxCommunities = 3;
    private int freeMaxPartyRooms = 2;
    private int freeHistoryDays = 30;

    public int getFreeMaxCommunities() {
        return freeMaxCommunities;
    }

    public void setFreeMaxCommunities(int freeMaxCommunities) {
        this.freeMaxCommunities = freeMaxCommunities;
    }

    public int getFreeMaxPartyRooms() {
        return freeMaxPartyRooms;
    }

    public void setFreeMaxPartyRooms(int freeMaxPartyRooms) {
        this.freeMaxPartyRooms = freeMaxPartyRooms;
    }

    public int getFreeHistoryDays() {
        return freeHistoryDays;
    }

    public void setFreeHistoryDays(int freeHistoryDays) {
        this.freeHistoryDays = freeHistoryDays;
    }
}
