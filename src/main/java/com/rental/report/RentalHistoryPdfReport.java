package com.rental.report;

import com.rental.model.RentalHistoryRow;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RentalHistoryPdfReport {

    private static final DateTimeFormatter TH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void export(File outFile,
            List<RentalHistoryRow> rows,
            String filterCustomer,
            String filterStatus,
            LocalDate filterDate) throws IOException {

        try (PDDocument doc = new PDDocument()) {

            // ===== Load Thai fonts from resources (embed) =====
            PDType0Font font = loadFont(doc, "/fonts/THSarabunNew.ttf");
            PDType0Font fontBold = loadFont(doc, "/fonts/THSarabunNew Bold.ttf");

            // ===== Page settings =====
            PDRectangle pageSize = PDRectangle.A4;
            float margin = 40;
            float yStart = pageSize.getHeight() - margin;

            // Table layout
            float tableWidth = pageSize.getWidth() - margin * 2;
            float[] colW = new float[] { 0.36f, 0.22f, 0.18f, 0.24f }; // ชื่อ / วันที่ / สถานะ / รายละเอียด
            float rowH = 22;
            float headerH = 26;

            // Colors
            Color bg = new Color(245, 246, 248);
            Color card = Color.WHITE;
            Color line = new Color(210, 210, 210);
            Color headerFill = new Color(215, 215, 215);
            Color textDark = new Color(35, 35, 35);

            // Page creator helper
            PageState ps = new PageState(doc, pageSize, margin, bg);

            try {
                // ===== Draw first page =====
                ps.newPage();
                float y = yStart;

                // Title
                y = drawTitle(ps, fontBold, "รายงานประวัติการเช่าพื้นที่", margin, y, textDark);

                // Filters card
                y -= 10;
                y = drawFilterCard(ps, font, fontBold, margin, y, tableWidth, card, line,
                        filterCustomer, filterStatus, filterDate);

                // Table card
                y -= 14;
                float tableCardTop = y;
                float tableCardMinHeight = 420; // ให้ “เหมือนกล่อง” หน้าจอ
                float tableCardBottom = Math.max(margin + 70, tableCardTop - tableCardMinHeight);

                // Draw card background
                ps.roundRectFill(margin, tableCardBottom, tableWidth, tableCardTop - tableCardBottom, 10, card);
                ps.roundRectStroke(margin, tableCardBottom, tableWidth, tableCardTop - tableCardBottom, 10, line);

                // Table inside card padding
                float pad = 12;
                float xTable = margin + pad;
                float yTable = tableCardTop - pad;

                // Table header
                float x = xTable;
                float yHeaderTop = yTable;

                ps.rectFill(xTable, yHeaderTop - headerH, tableWidth - pad * 2, headerH, headerFill);
                ps.rectStroke(xTable, yHeaderTop - headerH, tableWidth - pad * 2, headerH, line);

                drawText(ps, fontBold, 16, x + 8, yHeaderTop - 19, "ชื่อลูกค้า", textDark);
                x += (tableWidth - pad * 2) * colW[0];
                drawText(ps, fontBold, 16, x + 8, yHeaderTop - 19, "วัน/เดือน/ปี", textDark);
                x += (tableWidth - pad * 2) * colW[1];
                drawText(ps, fontBold, 16, x + 8, yHeaderTop - 19, "สถานะ", textDark);
                x += (tableWidth - pad * 2) * colW[2];
                drawText(ps, fontBold, 16, x + 8, yHeaderTop - 19, "รายละเอียด", textDark);

                // Column vertical lines + header bottom
                float innerW = tableWidth - pad * 2;
                float x0 = xTable;
                float y0 = yHeaderTop;
                float y1 = yHeaderTop - headerH;

                float x1 = x0 + innerW * colW[0];
                float x2 = x1 + innerW * colW[1];
                float x3 = x2 + innerW * colW[2];
                float x4 = x0 + innerW;

                ps.line(x1, y1, x1, y0, line);
                ps.line(x2, y1, x2, y0, line);
                ps.line(x3, y1, x3, y0, line);

                // Rows
                float yRowTop = yHeaderTop - headerH;
                int pageNo = 1;
                int rowIndex = 0;

                while (rowIndex < rows.size()) {
                    // If next row would go below card bottom -> new page
                    if (yRowTop - rowH < tableCardBottom + pad + 60) {
                        // Footer page number for previous page
                        drawFooter(ps, font, margin, pageNo);

                        // New page
                        pageNo++;
                        ps.newPage();
                        y = yStart;

                        y = drawTitle(ps, fontBold, "รายงานประวัติการเช่าพื้นที่ (ต่อ)", margin, y, textDark);
                        y -= 10;
                        y = drawFilterCard(ps, font, fontBold, margin, y, tableWidth, card, line,
                                filterCustomer, filterStatus, filterDate);

                        y -= 14;
                        tableCardTop = y;
                        tableCardBottom = margin + 70;

                        ps.roundRectFill(margin, tableCardBottom, tableWidth, tableCardTop - tableCardBottom, 10, card);
                        ps.roundRectStroke(margin, tableCardBottom, tableWidth, tableCardTop - tableCardBottom, 10,
                                line);

                        xTable = margin + pad;
                        yTable = tableCardTop - pad;

                        // header again
                        ps.rectFill(xTable, yTable - headerH, tableWidth - pad * 2, headerH, headerFill);
                        ps.rectStroke(xTable, yTable - headerH, tableWidth - pad * 2, headerH, line);

                        drawText(ps, fontBold, 16, xTable + 8, yTable - 19, "ชื่อลูกค้า", textDark);
                        drawText(ps, fontBold, 16, xTable + 8 + innerW * colW[0], yTable - 19, "วัน/เดือน/ปี",
                                textDark);
                        drawText(ps, fontBold, 16, xTable + 8 + innerW * (colW[0] + colW[1]), yTable - 19, "สถานะ",
                                textDark);
                        drawText(ps, fontBold, 16, xTable + 8 + innerW * (colW[0] + colW[1] + colW[2]), yTable - 19,
                                "รายละเอียด", textDark);

                        ps.line(x1 = xTable + innerW * colW[0], yTable - headerH, x1, yTable, line);
                        ps.line(x2 = xTable + innerW * (colW[0] + colW[1]), yTable - headerH, x2, yTable, line);
                        ps.line(x3 = xTable + innerW * (colW[0] + colW[1] + colW[2]), yTable - headerH, x3, yTable,
                                line);

                        yRowTop = yTable - headerH;
                    }

                    RentalHistoryRow r = rows.get(rowIndex);

                    // row border
                    ps.rectStroke(xTable, yRowTop - rowH, innerW, rowH, line);
                    ps.line(x1, yRowTop - rowH, x1, yRowTop, line);
                    ps.line(x2, yRowTop - rowH, x2, yRowTop, line);
                    ps.line(x3, yRowTop - rowH, x3, yRowTop, line);

                    // text
                    float ty = yRowTop - 16;
                    drawText(ps, font, 15, xTable + 8, ty, safe(r.getCustomerName()), textDark);
                    drawText(ps, font, 15, x1 + 8, ty, r.getDate() == null ? "-" : TH_DATE.format(r.getDate()),
                            textDark);

                    String status = safe(r.getStatus());
                    drawText(ps, font, 15, x2 + 8, ty, status, textDark);

                    drawText(ps, font, 15, x3 + 8, ty, "ดูรายละเอียด", new Color(64, 68, 106));

                    yRowTop -= rowH;
                    rowIndex++;
                }

                // Summary (bottom)
                float summaryY = Math.max(margin + 35, yRowTop - 18);
                drawText(ps, fontBold, 16, margin + 12, summaryY,
                        "สรุป: จำนวนรายการทั้งหมด " + rows.size() + " รายการ", textDark);

                // Footer page number last page
                drawFooter(ps, font, margin, pageNo);

            } finally {
                // ✅ สำคัญมาก: ปิด ContentStream ของหน้าสุดท้ายก่อน save
                ps.close();
            }

            // ✅ save หลังปิด writer แล้ว
            doc.save(outFile);
        }
    }

    // ===== helpers =====

    private static PDType0Font loadFont(PDDocument doc, String resPath) throws IOException {
        InputStream is = RentalHistoryPdfReport.class.getResourceAsStream(resPath);
        if (is == null)
            throw new FileNotFoundException("ไม่พบฟอนต์: " + resPath);
        // Java 8 compatible
        try (InputStream in = is) {
            return PDType0Font.load(doc, in, true);
        }
    }

    private static float drawTitle(PageState ps, PDType0Font fontBold, String title, float x, float y, Color color)
            throws IOException {
        drawText(ps, fontBold, 22, x, y - 10, title, color);
        return y - 32;
    }

    private static float drawFilterCard(PageState ps, PDType0Font font, PDType0Font fontBold,
            float x, float yTop, float w, Color card, Color line,
            String customer, String status, LocalDate date) throws IOException {
        float h = 84;
        float y = yTop;
        ps.roundRectFill(x, y - h, w, h, 10, card);
        ps.roundRectStroke(x, y - h, w, h, 10, line);

        float tx = x + 14;
        float ty = y - 18;

        drawText(ps, fontBold, 16, tx, ty, "เงื่อนไขค้นหา", new Color(35, 35, 35));

        ty -= 22;
        drawText(ps, font, 15, tx, ty,
                "ชื่อลูกค้า: " + (customer == null || customer.isBlank() ? "-" : customer),
                new Color(35, 35, 35));
        drawText(ps, font, 15, tx + 280, ty,
                "สถานะ: " + (status == null || status.isBlank() ? "ทั้งหมด" : status),
                new Color(35, 35, 35));

        ty -= 20;
        drawText(ps, font, 15, tx, ty,
                "วัน/เดือน/ปี: " + (date == null ? "-" : TH_DATE.format(date)),
                new Color(35, 35, 35));

        return y - h;
    }

    private static void drawFooter(PageState ps, PDType0Font font, float margin, int pageNo) throws IOException {
        PDRectangle p = ps.page.getMediaBox();
        String s = "หน้า " + pageNo;
        drawText(ps, font, 14, p.getWidth() - margin - 50, margin - 10, s, new Color(120, 120, 120));
    }

    private static void drawText(PageState ps, PDType0Font font, float size, float x, float y, String text, Color color)
            throws IOException {
        ps.cs.beginText();
        ps.cs.setFont(font, size);
        ps.cs.setNonStrokingColor(color);
        ps.cs.newLineAtOffset(x, y);
        ps.cs.showText(text == null ? "" : text);
        ps.cs.endText();
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }

    // ===== page state helper =====
    private static class PageState implements Closeable {
        final PDDocument doc;
        final PDRectangle size;
        final float margin;
        final Color background;
        PDPage page;
        PDPageContentStream cs;

        PageState(PDDocument doc, PDRectangle size, float margin, Color background) {
            this.doc = doc;
            this.size = size;
            this.margin = margin;
            this.background = background;
        }

        void newPage() throws IOException {
            if (cs != null)
                cs.close();
            page = new PDPage(size);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);

            // background
            rectFill(0, 0, size.getWidth(), size.getHeight(), background);
        }

        void rectFill(float x, float y, float w, float h, Color c) throws IOException {
            cs.setNonStrokingColor(c);
            cs.addRect(x, y, w, h);
            cs.fill();
        }

        void rectStroke(float x, float y, float w, float h, Color c) throws IOException {
            cs.setStrokingColor(c);
            cs.addRect(x, y, w, h);
            cs.stroke();
        }

        void line(float x1, float y1, float x2, float y2, Color c) throws IOException {
            cs.setStrokingColor(c);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        void roundRectFill(float x, float y, float w, float h, float r, Color c) throws IOException {
            // ถ้าต้องการมุมโค้งจริง ค่อยปรับเป็น path โค้งได้
            rectFill(x, y, w, h, c);
        }

        void roundRectStroke(float x, float y, float w, float h, float r, Color c) throws IOException {
            rectStroke(x, y, w, h, c);
        }

        @Override
        public void close() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }
    }
}
