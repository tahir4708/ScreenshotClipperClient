package com.googleupdaterunner;

import java.util.Date;

public class Base64Model {
    private long id;
    private String base64String;
    private String guid;
    private String  timestamp;
    private String device_info;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBase64String() {
        return base64String;
    }

    public void setBase64String(String base64String) {
        this.base64String = base64String;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceInfo() {
        return device_info;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.device_info = deviceInfo;
    }
}
