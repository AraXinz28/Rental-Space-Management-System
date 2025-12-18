package com.rental.util;

import com.rental.model.User;

public class Session {
    private static User currentUser;

    public static void login(User user) { currentUser = user; }
    public static void clear() { currentUser = null; }

    public static boolean isLoggedIn() { return currentUser != null; }
    public static User getCurrentUser() { return currentUser; }

    public static String role() { return currentUser == null ? null : currentUser.getRole(); }
    public static String username() { return currentUser == null ? null : currentUser.getUsername(); }
}
