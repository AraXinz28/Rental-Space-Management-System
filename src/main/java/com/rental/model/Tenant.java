package com.rental.model;

public class Tenant extends User {
    private final String phone;

    public Tenant(int id, String username, String email, String phone) {
        super(id, username, email, "tenant");
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String getHomeFxml() {
        return "/views/homepage.fxml";
    }
}
