package com.rental.database;

import com.rental.config.SupabaseConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class SupabaseClient {

    private final String url = SupabaseConfig.SUPABASE_URL;
    private final String key = SupabaseConfig.SUPABASE_KEY;

    private final HttpClient client;

    public SupabaseClient() {
        this.client = HttpClient.newHttpClient();
    }

    // ✅ GET: SELECT *
    public String selectAll(String table) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table + "?select=*"))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ✅ GET: SELECT with eq filter
    public String selectWhere(String table, String column, String value) throws Exception {
        String uri = url + "/rest/v1/" + table + "?select=*&" + encode(column) + "=eq." + encode(value);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ✅ POST: INSERT
    public String insert(String table, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ✅ PATCH: UPDATE (ทั่วไป)
    public String update(String table, String column, String value, String jsonBody) throws Exception {
        String uri = url + "/rest/v1/" + table + "?" + encode(column) + "=eq." + encode(value);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ✅ DELETE
    public String delete(String table, String column, String value) throws Exception {
        String uri = url + "/rest/v1/" + table + "?" + encode(column) + "=eq." + encode(value);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .DELETE()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ✅ UPDATE status ตาม id (table ทั่วไปที่ใช้ PK = id)
    public String updateStatusById(String table, long id, String newStatus) throws Exception {
        String jsonBody = "{\"status\":\"" + escapeJson(newStatus) + "\"}";
        return update(table, "id", String.valueOf(id), jsonBody);
    }

    // ✅ UPDATE status ของ bookings ตาม booking_id
    public String updateBookingStatus(long bookingId, String uiStatus) throws Exception {
        String dbStatus = mapStatusToDb(uiStatus);
        String jsonBody = "{\"status\":\"" + escapeJson(dbStatus) + "\"}";
        return update("bookings", "booking_id", String.valueOf(bookingId), jsonBody);
    }

    // ✅ UPDATE by id (table ทั่วไป)
    public String updateById(String table, String jsonBody, long id) throws Exception {
        String uri = url + "/rest/v1/" + table + "?id=eq." + encode(String.valueOf(id));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ✅ UPDATE bookings by booking_id
    public String updateBookingByBookingId(String jsonBody, long bookingId) throws Exception {
        String uri = url + "/rest/v1/bookings?booking_id=eq." + encode(String.valueOf(bookingId));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ✅ UPDATE ด้วย condition อิสระ (ใช้กับ composition)
    public String updateWhere(String table, String condition, String jsonBody) throws Exception {

        String uri = url + "/rest/v1/" + table + "?" + condition;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

         HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

            // debug
            System.out.println("UPDATE WHERE URI = " + uri);
            System.out.println("STATUS = " + response.statusCode());
            System.out.println("BODY = " + response.body());

        return response.body();
}


    // ✅ สำหรับหน้า “ประวัติ” ฝั่งแอดมิน (ของเดิม)
    public String selectBookings(String fullNameLike, String uiStatus, LocalDate date) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(url)
                .append("/rest/v1/bookings")
                .append("?select=booking_id,full_name,created_at,status,payments(status)")
                .append("&order=created_at.desc");

        if (fullNameLike != null && !fullNameLike.trim().isEmpty()) {
            String v = fullNameLike.trim().replace(" ", "%");
            sb.append("&full_name=ilike.*").append(encode(v)).append("*");
        }

        if (uiStatus != null && !"ทั้งหมด".equals(uiStatus)) {
            switch (uiStatus) {
                case "เสร็จสิ้น" -> sb.append("&payments.status=eq.approved");
                case "รอดำเนินการ" -> sb.append("&payments.status=eq.pending");
                case "ยกเลิก" -> sb.append("&or=(status.eq.cancelled,payments.status.eq.rejected)");
                default -> {
                    String dbStatus = mapStatusToDb(uiStatus);
                    sb.append("&status=eq.").append(encode(dbStatus));
                }
            }
        }

        if (date != null) {
            LocalDate next = date.plusDays(1);
            sb.append("&created_at=gte.").append(encode(date.toString())).append("T00:00:00");
            sb.append("&created_at=lt.").append(encode(next.toString())).append("T00:00:00");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sb.toString()))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // =====================================================
    // ✅ ใหม่: สำหรับหน้า “ประวัติ” ฝั่งแอดมิน (ดึงจาก payments เป็นหลัก) - แก้ไขแล้ว
    // =====================================================
    public String selectPaymentsForAdminHistory(String fullNameLike, String uiStatus, LocalDate date) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(url)
                .append("/rest/v1/payments")
                // ✅ แก้ไขตรงนี้: ใช้ default relation (ไม่ระบุ !ชื่อยาว) → ทำงานแน่นอน
                .append("?select=id,booking_id,created_at,status,bookings(full_name)")
                .append("&order=created_at.desc");

        // ---- filter: name (จาก bookings.full_name) ----
        if (fullNameLike != null && !fullNameLike.trim().isEmpty()) {
            String v = fullNameLike.trim().replace(" ", "%");
            sb.append("&bookings.full_name=ilike.*").append(encode(v)).append("*");
        }

        // ---- filter: status (จาก payments.status เท่านั้น) ----
        if (uiStatus != null && !"ทั้งหมด".equals(uiStatus)) {
            switch (uiStatus) {
                case "เสร็จสิ้น" -> sb.append("&status=eq.approved");
                case "รอดำเนินการ" -> sb.append("&status=eq.pending");
                case "ยกเลิก" -> sb.append("&status=eq.rejected");
            }
        }

        // ---- filter: created_at ช่วงวัน “ตามเวลาไทย” (+07:00) ----
        if (date != null) {
            LocalDate next = date.plusDays(1);
            String start = date + "T00:00:00+07:00";
            String end = next + "T00:00:00+07:00";

            sb.append("&created_at=gte.").append(encode(start));
            sb.append("&created_at=lt.").append(encode(end));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sb.toString()))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // debug
        System.out.println("ADMIN PAYMENTS URI=" + sb);
        System.out.println("ADMIN PAYMENTS STATUS=" + response.statusCode());
        System.out.println("ADMIN PAYMENTS BODY=" + response.body());

        return response.body();
    }

    // ✅ ดึงรายละเอียดตาม booking_id (ของเดิม)
    public String selectBookingDetailById(long bookingId) throws Exception {

        String uri = url + "/rest/v1/bookings"
                + "?select=booking_id,stall_id,full_name,phone,product_type,total_price,deposit_price,status,created_at,start_date,end_date,"
                + "payments(id,status,payment_method,payment_date,amount,created_at,reject_reason)"
                + "&booking_id=eq." + bookingId
                + "&limit=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("DETAIL STATUS=" + response.statusCode());
        System.out.println("DETAIL BODY=" + response.body());

        return response.body();
    }

    // ✅ ของเดิม
    public String selectJoinBookingsPayments(long userId) throws Exception {
        String uri = url + "/rest/v1/bookings"
                + "?select=booking_id,product_type,start_date,end_date,"
                + "payments!fk_payments_booking(status,payment_method,amount,payment_date)"
                + "&user_id=eq." + encode(String.valueOf(userId));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // =====================================================
    // ✅ ใหม่: Payments-only (ชัวร์) ไม่ผูกชื่อ FK
    // =====================================================
 public String selectPaymentsJoinBookings(long userId) throws Exception {
    String uri = url + "/rest/v1/payments"
        + "?select=id,booking_id,status,payment_method,amount,payment_date,created_at,"
        + "bookings(booking_id,user_id,product_type,start_date,end_date,full_name,phone,stall_id)"
        + "&bookings.user_id=eq." + encode(String.valueOf(userId))
        + "&order=created_at.desc";

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("apikey", key)
            .header("Authorization", "Bearer " + key)
            .header("Content-Type", "application/json")
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("PAYMENTS+BOOKINGS STATUS=" + response.statusCode());
    System.out.println("PAYMENTS+BOOKINGS BODY=" + response.body());
    return response.body();
}


    /**
     * อัปเดตสถานะเฉพาะในตาราง payments โดยใช้ payment_id
     */
    public String updatePaymentStatus(long paymentId, String newStatus) throws Exception {
    String jsonBody = "{\"status\":\"" + escapeJson(newStatus) + "\"}";

    String uri = url + "/rest/v1/payments?id=eq." 
            + encode(String.valueOf(paymentId));

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("apikey", key)
            .header("Authorization", "Bearer " + key)
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 400) {
        throw new RuntimeException("อัปเดตสถานะ payment ล้มเหลว: " +
                response.statusCode() + " - " + response.body());
    }

    System.out.println("อัปเดต payment id " + paymentId + " เป็น " + newStatus + " สำเร็จ");
    return response.body();
}

    // ===== Helpers =====
    private static String encode(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ✅ UI(ไทย) -> DB
    private static String mapStatusToDb(String uiStatus) {
        if (uiStatus == null) return "";
        return switch (uiStatus) {
            case "เสร็จสิ้น" -> "approved";     // เปลี่ยนให้ตรงกับ payments.status
            case "ยกเลิก" -> "rejected";
            case "รอดำเนินการ" -> "pending";
            default -> uiStatus;
        };
    }
}