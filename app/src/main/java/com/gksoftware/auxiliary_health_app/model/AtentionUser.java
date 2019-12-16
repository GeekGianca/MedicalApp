package com.gksoftware.auxiliary_health_app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtentionUser {
    private int identify;
    private String name;
    private int age;
    private String birthdate;
    private String phone;
    private String userDocRef;
    private List<String> heartbeat;
    private List<String> bloodOxygen;
    private List<String> bodyTemperature;
    private List<String> respiratoryRate;

    public AtentionUser(int identify, String name, int age, String birthdate, String phone, String userDocRef) {
        this.identify = identify;
        this.name = name;
        this.age = age;
        this.birthdate = birthdate;
        this.phone = phone;
        this.userDocRef = userDocRef;
        this.heartbeat = new ArrayList<>();
        this.bloodOxygen = new ArrayList<>();
        this.bodyTemperature = new ArrayList<>();
        this.respiratoryRate = new ArrayList<>();
    }

    public AtentionUser() {
    }

    public List<String> getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(List<String> heartbeat) {
        this.heartbeat = heartbeat;
    }

    public List<String> getBloodOxygen() {
        return bloodOxygen;
    }

    public void setBloodOxygen(List<String> bloodOxygen) {
        this.bloodOxygen = bloodOxygen;
    }

    public List<String> getBodyTemperature() {
        return bodyTemperature;
    }

    public void setBodyTemperature(List<String> bodyTemperature) {
        this.bodyTemperature = bodyTemperature;
    }

    public List<String> getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(List<String> respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public int getIdentify() {
        return identify;
    }

    public void setIdentify(int identify) {
        this.identify = identify;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserDocRef() {
        return userDocRef;
    }

    public void setUserDocRef(String userDocRef) {
        this.userDocRef = userDocRef;
    }

    public Map<String, Object> store() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("identify", getIdentify());
        doc.put("name", getName());
        doc.put("age", getAge());
        doc.put("birthdate", getBirthdate());
        doc.put("phone", getPhone());
        doc.put("userDocRef", getUserDocRef());
        doc.put("heartbeat", getHeartbeat());
        doc.put("bloodOxygen", getBloodOxygen());
        doc.put("bodyTemperature", getBodyTemperature());
        doc.put("respiratoryRate", getRespiratoryRate());
        return doc;
    }
}
