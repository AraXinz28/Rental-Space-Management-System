package com.rental.database;

import com.rental.config.SupabaseConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SupabaseClient {

    private final String url = SupabaseConfig.SUPABASE_URL;
    private final String key = SupabaseConfig.SUPABASE_KEY;

    private final HttpClient client;

    public SupabaseClient() {
        this.client = HttpClient.newHttpClient();
    }

    // 🔹 SELECT *
    public String selectAll(String table) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table + "?select=*"))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // 🔹 SELECT with filter
    public String selectWhere(String table, String column, String value) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table + "?select=*&" + column + "=eq." + value))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // 🔹 INSERT
    public String insert(String table, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // 🔹 UPDATE (ทั่วไป)
    public String update(String table, String column, String value, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table + "?" + column + "=eq." + value))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // 🔹 DELETE
    public String delete(String table, String column, String value) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table + "?" + column + "=eq." + value))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // ✅ NEW: อัปเดตสถานะตาม id โดยเฉพาะ
    public String updateStatusById(String table, int id, String newStatus) throws Exception {
        String jsonBody = "{\"status\":\"" + newStatus + "\"}";
        return update(table, "id", String.valueOf(id), jsonBody);
    }
    // 🔹 UPDATE by id (ใช้กับ zone / edit form)
    public String updateById(String table, String jsonBody, int id) throws Exception {

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + "/rest/v1/" + table + "?id=eq." + id))
            .header("apikey", key)
            .header("Authorization", "Bearer " + key)
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

    return response.body();
}
}