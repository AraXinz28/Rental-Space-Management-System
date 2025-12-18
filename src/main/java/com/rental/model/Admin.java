package com.rental.model;

public class Admin extends User {
    public Admin(int id, String username, String email) {
        super(id, username, email, "admin");
    }

    @Override
    public String getHomeFxml() {
        return "/views/zone_management.fxml";
    }
}
