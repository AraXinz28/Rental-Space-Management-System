package com.rental.model;

public class Tenant extends User {
    private final String phone;

    public Tenant(int id, String username, String email, String fullName, String phone) {
        super(id, username, email, "tenant");
        this.fullName = fullName;
        this.phone = phone;
    }

    private final String fullName;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    @Override
    public String getHomeFxml() {
        return "/views/homepage.fxml";
    }
}
