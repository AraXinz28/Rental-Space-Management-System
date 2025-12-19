package com.rental.model;

public abstract class User {
    protected final int id;
    protected final String username;
    protected final String email;
    protected final String role; // "admin" | "tenant"

    protected User(int id, String username, String email, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    // polymorphism: แต่ละ role กลับหน้าแรกไม่เหมือนกัน
    public abstract String getHomeFxml();

    public abstract String getFullName();

    public String getPhone() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPhone'");
    }
}
