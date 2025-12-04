package com.rental.config;

import io.github.cdimascio.dotenv.Dotenv;

public class SupabaseConfig {

    private static final Dotenv dotenv = Dotenv.load();

    public static final String SUPABASE_URL = dotenv.get("SUPABASE_URL");
    public static final String SUPABASE_KEY = dotenv.get("SUPABASE_KEY");
}
