package com.gksoftware.auxiliary_health_app.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String keyuser;
    private String name;
    private String address;
    private String phone;
    private String email;

    public User(String keyuser, String name, String address, String phone, String email) {
        this.keyuser = keyuser;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
    }

    public String getKeyuser() {
        return keyuser;
    }

    public void setKeyuser(String keyuser) {
        this.keyuser = keyuser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, Object> store() {
        Map<String, Object> user = new HashMap<>();
        user.put("keyuser", getKeyuser());
        user.put("name", getName());
        user.put("address", getAddress());
        user.put("phone", getPhone());
        user.put("email", getEmail());
        return user;
    }
}
