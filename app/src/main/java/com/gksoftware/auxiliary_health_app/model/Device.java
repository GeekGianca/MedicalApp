package com.gksoftware.auxiliary_health_app.model;

public class Device {
    private String deviceUid;
    private String name;
    private int status;

    public Device(String device_uid, String device_name, int status) {
        this.deviceUid = device_uid;
        this.name = device_name;
        this.status = status;
    }

    public Device() {
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Device{" +
                "deviceUid='" + deviceUid + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
