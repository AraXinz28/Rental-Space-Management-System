package com.rental.report;

import com.rental.model.ReceiptData;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReceiptPdfReport {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TH_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void export(File out, ReceiptData d) throws Exception {

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType0Font font;
            try (InputStream is = ReceiptPdfReport.class.getResourceAsStream("/fonts/THSarabunNew.ttf")) {
                if (is == null)
                    throw new RuntimeException("ไม่พบฟอนต์ไทย: /fonts/THSarabunNew.ttf");
                font = PDType0Font.load(doc, is, true);
            }

            float W = page.getMediaBox().getWidth();
            float H = page.getMediaBox().getHeight();
            float M = 44; // margin

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // =========================
                // ✅ Theme colors (modern)
                // =========================
                // Dark header
                int[] C_PRIMARY = { 64, 68, 106 }; // #40446A
                // Light background
                int[] C_BG = { 245, 246, 248 }; // #F5F6F8
                // Card white
                int[] C_WHITE = { 255, 255, 255 };
                // Soft gray
                int[] C_SOFT = { 238, 240, 244 }; // soft panel
                // Text dark
                int[] C_TEXT = { 35, 38, 47 }; // near black
                // Muted
                int[] C_MUTED = { 110, 114, 128 };

                // =========================
                // ✅ Full page background
                // =========================
                fillRect(cs, 0, 0, W, H, C_BG);

                // =========================
                // ✅ Main card
                // =========================
                float cardX = M;
                float cardY = 70;
                float cardW = W - M * 2;
                float cardH = H - 140;

                // Card shadow (fake) - draw a slightly offset gray rounded rect behind
                fillRoundRect(cs, cardX + 2, cardY - 2, cardW, cardH, 16, new int[] { 220, 223, 230 });
                // Card white
                fillRoundRect(cs, cardX, cardY, cardW, cardH, 16, C_WHITE);
                // Card border subtle
                strokeRoundRect(cs, cardX, cardY, cardW, cardH, 16, new int[] { 220, 223, 230 }, 0.8f);

                // =========================
                // ✅ Header bar
                // =========================
                float headerH = 64;
                fillRoundRectTopOnly(cs, cardX, cardY + cardH - headerH, cardW, headerH, 16, C_PRIMARY);

                // Header texts (white)
                float hx = cardX + 18;
                float hy = cardY + cardH - 24;

                text(cs, font, 22, hx, hy, "ใบเสร็จรับเงิน", C_WHITE);
                text(cs, font, 13, hx, hy - 18, "RECEIPT • ระบบจัดการพื้นที่ให้เช่าจำหน่ายสินค้า", C_WHITE);

                // Right meta
                String receiptNo = "RC-" + d.bookingId;
                String issueAt = TH_DT.format(LocalDateTime.now());

                textRight(cs, font, 13, cardX + cardW - 18, hy, "เลขที่: " + receiptNo, C_WHITE);
                textRight(cs, font, 13, cardX + cardW - 18, hy - 18, "ออกเมื่อ: " + issueAt, C_WHITE);

                // =========================
                // ✅ Customer info panel
                // =========================
                float y = cardY + cardH - headerH - 18;

                float infoX = cardX + 18;
                float infoW = cardW - 36;
                float infoH = 92;
                float infoY = y - infoH;

                fillRoundRect(cs, infoX, infoY, infoW, infoH, 12, C_SOFT);
                strokeRoundRect(cs, infoX, infoY, infoW, infoH, 12, new int[] { 225, 228, 235 }, 0.8f);

                float tx = infoX + 14;
                float ty = infoY + infoH - 22;

                text(cs, font, 15, tx, ty, "ข้อมูลผู้เช่า", C_TEXT);
                ty -= 18;

                text(cs, font, 14, tx, ty, "ชื่อ: " + safe(d.fullName), C_TEXT);
                ty -= 16;

                text(cs, font, 14, tx, ty,
                        "โทร: " + safe(d.phone) + "    โซน: " + safe(d.zone) + "    ล็อค: " + safe(d.lockNo),
                        C_TEXT);
                ty -= 16;

                String dateStr = (d.startDate == null ? "-" : TH_DATE.format(d.startDate))
                        + "  ถึง  " + (d.endDate == null ? "-" : TH_DATE.format(d.endDate));
                text(cs, font, 14, tx, ty, "ช่วงเช่า: " + dateStr, C_TEXT);

                y = infoY - 18;

                // =========================
                // ✅ Items table
                // =========================
                text(cs, font, 15, infoX, y, "รายการ", C_TEXT);
                y -= 10;

                float tableX = infoX;
                float tableW = infoW;
                float rowH = 26;

                // header row (filled)
                float headerY = y - rowH;
                fillRect(cs, tableX, headerY, tableW, rowH, new int[] { 245, 246, 250 });
                strokeRect(cs, tableX, headerY, tableW, rowH, new int[] { 225, 228, 235 }, 0.8f);

                text(cs, font, 14, tableX + 10, headerY + 7, "รายละเอียด", C_MUTED);
                textRight(cs, font, 14, tableX + tableW - 10, headerY + 7, "จำนวนเงิน (บาท)", C_MUTED);

                // row1
                float r1Y = headerY - rowH;
                strokeRect(cs, tableX, r1Y, tableW, rowH, new int[] { 225, 228, 235 }, 0.8f);
                text(cs, font, 14, tableX + 10, r1Y + 7,
                        "ค่าเช่าพื้นที่ (" + safe(d.productType) + ")  ล็อค " + safe(d.stallId),
                        C_TEXT);
                textRight(cs, font, 14, tableX + tableW - 10, r1Y + 7, fmt(d.total), C_TEXT);

                // row2
                float r2Y = r1Y - rowH;
                strokeRect(cs, tableX, r2Y, tableW, rowH, new int[] { 225, 228, 235 }, 0.8f);
                text(cs, font, 14, tableX + 10, r2Y + 7, "เงินมัดจำ", C_TEXT);
                textRight(cs, font, 14, tableX + tableW - 10, r2Y + 7, fmt(d.deposit), C_TEXT);

                y = r2Y - 18;

                // =========================
                // ✅ Summary box (highlight)
                // =========================
                float sumW = 280;
                float sumX = tableX + tableW - sumW;
                float sumH = 104;
                float sumY = y - sumH;

                fillRoundRect(cs, sumX, sumY, sumW, sumH, 12, new int[] { 248, 249, 252 });
                strokeRoundRect(cs, sumX, sumY, sumW, sumH, 12, new int[] { 225, 228, 235 }, 0.8f);

                BigDecimal grand = nz(d.total).add(nz(d.deposit));

                float sy = sumY + sumH - 22;
                text(cs, font, 15, sumX + 14, sy, "สรุปยอดชำระ", C_TEXT);
                sy -= 20;

                text(cs, font, 14, sumX + 14, sy, "ค่าเช่า", C_MUTED);
                textRight(cs, font, 14, sumX + sumW - 14, sy, fmt(d.total), C_TEXT);
                sy -= 16;

                text(cs, font, 14, sumX + 14, sy, "มัดจำ", C_MUTED);
                textRight(cs, font, 14, sumX + sumW - 14, sy, fmt(d.deposit), C_TEXT);
                sy -= 14;

                // divider
                strokeLine(cs, sumX + 14, sy + 6, sumX + sumW - 14, sy + 6, new int[] { 210, 214, 224 }, 0.8f);

                text(cs, font, 18, sumX + 14, sy - 10, "รวมทั้งสิ้น", C_TEXT);
                textRight(cs, font, 18, sumX + sumW - 14, sy - 10, fmt(grand), C_TEXT);

                // =========================
                // ✅ Footer
                // =========================
                float footerY = cardY + 26;

                text(cs, font, 13, cardX + 18, footerY + 18, "วิธีชำระเงิน: " + safe(d.paymentMethod), C_MUTED);
                text(cs, font, 12, cardX + 18, footerY,
                        "หมายเหตุ: เอกสารนี้ออกโดยระบบ อาจใช้เป็นหลักฐานการชำระเงินได้",
                        C_MUTED);
            }

            doc.save(out);
        }
    }

    // =========================================================
    // Text helpers (support color)
    // =========================================================
    private static void text(PDPageContentStream cs, PDType0Font font, int size, float x, float y, String s, int[] rgb)
            throws Exception {
        setFill(cs, rgb);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(s == null ? "" : s);
        cs.endText();
    }

    private static void textRight(PDPageContentStream cs, PDType0Font font, int size, float rightX, float y, String s,
            int[] rgb)
            throws Exception {
        String txt = s == null ? "" : s;
        float w = font.getStringWidth(txt) / 1000f * size;
        text(cs, font, size, rightX - w, y, txt, rgb);
    }

    // =========================================================
    // Shapes helpers
    // =========================================================
    private static void setFill(PDPageContentStream cs, int[] rgb) throws Exception {
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    private static void setStroke(PDPageContentStream cs, int[] rgb, float width) throws Exception {
        cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.setLineWidth(width);
    }

    private static void fillRect(PDPageContentStream cs, float x, float y, float w, float h, int[] rgb)
            throws Exception {
        setFill(cs, rgb);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void strokeRect(PDPageContentStream cs, float x, float y, float w, float h, int[] rgb, float width)
            throws Exception {
        setStroke(cs, rgb, width);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void strokeLine(PDPageContentStream cs, float x1, float y1, float x2, float y2, int[] rgb,
            float width) throws Exception {
        setStroke(cs, rgb, width);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static void fillRoundRect(PDPageContentStream cs, float x, float y, float w, float h, float r, int[] rgb)
            throws Exception {
        setFill(cs, rgb);
        roundRectPath(cs, x, y, w, h, r);
        cs.fill();
    }

    private static void strokeRoundRect(PDPageContentStream cs, float x, float y, float w, float h, float r, int[] rgb,
            float width) throws Exception {
        setStroke(cs, rgb, width);
        roundRectPath(cs, x, y, w, h, r);
        cs.stroke();
    }

    // Header bar: round only TOP corners
    private static void fillRoundRectTopOnly(PDPageContentStream cs, float x, float y, float w, float h, float r,
            int[] rgb) throws Exception {
        setFill(cs, rgb);
        roundRectTopOnlyPath(cs, x, y, w, h, r);
        cs.fill();
    }

    // ✅ Rounded rect path (PDFBox compatible)
    private static void roundRectPath(PDPageContentStream cs, float x, float y, float w, float h, float r)
            throws Exception {
        float radius = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float k = 0.552284749831f;
        float c = radius * k;

        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;

        cs.moveTo(x0 + radius, y0);
        cs.lineTo(x1 - radius, y0);
        cs.curveTo(x1 - radius + c, y0, x1, y0 + radius - c, x1, y0 + radius);

        cs.lineTo(x1, y1 - radius);
        cs.curveTo(x1, y1 - radius + c, x1 - radius + c, y1, x1 - radius, y1);

        cs.lineTo(x0 + radius, y1);
        cs.curveTo(x0 + radius - c, y1, x0, y1 - radius + c, x0, y1 - radius);

        cs.lineTo(x0, y0 + radius);
        cs.curveTo(x0, y0 + radius - c, x0 + radius - c, y0, x0 + radius, y0);
        cs.closePath();
    }

    // ✅ Only top corners rounded (for header bar)
    private static void roundRectTopOnlyPath(PDPageContentStream cs, float x, float y, float w, float h, float r)
            throws Exception {
        float radius = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float k = 0.552284749831f;
        float c = radius * k;

        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;

        cs.moveTo(x0, y0);
        cs.lineTo(x1, y0);

        cs.lineTo(x1, y1 - radius);
        cs.curveTo(x1, y1 - radius + c, x1 - radius + c, y1, x1 - radius, y1);

        cs.lineTo(x0 + radius, y1);
        cs.curveTo(x0 + radius - c, y1, x0, y1 - radius + c, x0, y1 - radius);

        cs.lineTo(x0, y0);
        cs.closePath();
    }

    // =========================================================
    // Data helpers
    // =========================================================
    private static String fmt(BigDecimal v) {
        return MONEY.format(nz(v));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}
