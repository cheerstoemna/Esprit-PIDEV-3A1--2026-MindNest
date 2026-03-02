package services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import models.SessionFeedback;
import models.TherapySession;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * SessionSummaryPDFService
 * Generates a professional, branded PDF summary for a therapy session.
 *
 * Maven dependency to add to pom.xml:
 * <dependency>
 *     <groupId>com.itextpdf</groupId>
 *     <artifactId>itext7-core</artifactId>
 *     <version>7.2.5</version>
 *     <type>pom</type>
 * </dependency>
 *
 * Usage in controller:
 *   SessionSummaryPDFService pdfService = new SessionSummaryPDFService();
 *   String filePath = "session_" + session.getSessionId() + "_summary.pdf";
 *   pdfService.generateSummary(session, feedback, filePath);
 */
public class SessionSummaryPDFService {

    // ── MindNest brand colors ──────────────────────────────────────────────
    private static final DeviceRgb GREEN_DARK    = new DeviceRgb(0x04, 0x78, 0x57); // #047857
    private static final DeviceRgb GREEN_MID     = new DeviceRgb(0x05, 0x96, 0x69); // #059669
    private static final DeviceRgb GREEN_LIGHT   = new DeviceRgb(0x10, 0xb9, 0x81); // #10b981
    private static final DeviceRgb GREEN_BG      = new DeviceRgb(0xd1, 0xfa, 0xe5); // #d1fae5
    private static final DeviceRgb TEXT_GRAY     = new DeviceRgb(0x5a, 0x75, 0x71); // #5a7571
    private static final DeviceRgb TEXT_DARK     = new DeviceRgb(0x1f, 0x2d, 0x2b); // near black
    private static final DeviceRgb BORDER_GRAY   = new DeviceRgb(0xe2, 0xe8, 0xe6);
    private static final DeviceRgb WHITE         = new DeviceRgb(0xff, 0xff, 0xff);
    private static final DeviceRgb YELLOW_STAR   = new DeviceRgb(0xf5, 0x9e, 0x0b); // #f59e0b

    // ── Therapist lookup (mirrors what's in your controllers) ──────────────
    private static final Map<Integer, String> THERAPIST_NAMES = Map.of(
            1, "Dr. Mark Thompson",
            2, "Dr. Lina Ben Ali",
            3, "Dr. Sami Gharbi"
    );

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a");

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN: Generate session summary PDF
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * @param session   the TherapySession to summarize
     * @param feedback  the SessionFeedback for this session (can be null)
     * @param outputPath full file path for the PDF, e.g. "C:/Downloads/session_42.pdf"
     */
    public void generateSummary(TherapySession session,
                                SessionFeedback feedback,
                                String outputPath) throws Exception {

        PdfWriter  writer  = new PdfWriter(outputPath);
        PdfDocument pdf    = new PdfDocument(writer);
        Document   doc     = new Document(pdf, PageSize.A4);

        doc.setMargins(0, 0, 30, 0); // top margin handled by header block

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        // ── 1. Header Banner ────────────────────────────────────────────────
        addHeader(doc, bold, regular);

        // ── 2. Title block ──────────────────────────────────────────────────
        doc.add(new Paragraph()
                .add(new Text("Session Summary Report\n").setFont(bold).setFontSize(22).setFontColor(TEXT_DARK))
                .add(new Text("MindNest Therapy Management System").setFont(regular).setFontSize(10).setFontColor(TEXT_GRAY))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setMarginBottom(20)
        );

        // ── 3. Session Details Card (with live currency prices) ─────────────
        addSectionTitle(doc, bold, "Session Details");
        // Fetch live currency conversion (synchronous — runs during PDF generation)
        CurrencyConversionService.ConversionResult prices = null;
        try {
            prices = new CurrencyConversionService().convert();
        } catch (Exception e) {
            System.err.println("[PDF] Currency fetch failed: " + e.getMessage());
        }
        addDetailsTable(doc, session, bold, regular, prices);

        // ── 4. Session Notes ────────────────────────────────────────────────
        if (session.getSessionNotes() != null && !session.getSessionNotes().isBlank()) {
            addSectionTitle(doc, bold, "Session Notes");
            addNotesBlock(doc, session.getSessionNotes(), regular, italic);
        }

        // ── 5. Feedback Section ─────────────────────────────────────────────
        addSectionTitle(doc, bold, "Patient Feedback");
        if (feedback != null) {
            // Fetch sentiment badge (synchronous — API call during PDF generation)
            SentimentAnalysisService.SentimentResult sentiment = null;
            if (feedback.getComment() != null && !feedback.getComment().isBlank()) {
                try {
                    sentiment = new SentimentAnalysisService().analyze(feedback.getComment());
                } catch (Exception e) {
                    System.err.println("[PDF] Sentiment fetch failed: " + e.getMessage());
                }
            }
            addFeedbackBlock(doc, feedback, sentiment, bold, regular, italic);
        } else {
            doc.add(new Paragraph("No feedback submitted for this session.")
                    .setFont(italic)
                    .setFontSize(10)
                    .setFontColor(TEXT_GRAY)
                    .setMarginLeft(40)
                    .setMarginRight(40)
            );
        }

        // ── 6. Footer ───────────────────────────────────────────────────────
        addFooter(doc, regular, session);

        doc.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HEADER BANNER
    // ─────────────────────────────────────────────────────────────────────────
    private void addHeader(Document doc, PdfFont bold, PdfFont regular) throws IOException {
        Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(GREEN_DARK)
                .setMarginBottom(0);

        Cell cell = new Cell()
                .setBorder(null)
                .setPadding(24)
                .add(new Paragraph()
                        .add(new Text("🌿 MindNest\n").setFont(bold).setFontSize(26).setFontColor(WHITE))
                        .add(new Text("Confidential Session Record").setFont(regular).setFontSize(11).setFontColor(new DeviceRgb(0xa7, 0xf3, 0xd0)))
                        .setTextAlignment(TextAlignment.CENTER)
                );

        header.addCell(cell);
        doc.add(header);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION TITLE
    // ─────────────────────────────────────────────────────────────────────────
    private void addSectionTitle(Document doc, PdfFont bold, String title) {
        doc.add(new Paragraph(title)
                .setFont(bold)
                .setFontSize(13)
                .setFontColor(GREEN_DARK)
                .setMarginLeft(40)
                .setMarginRight(40)
                .setMarginTop(18)
                .setMarginBottom(6)
                .setBorderBottom(new SolidBorder(GREEN_LIGHT, 1.5f))
                .setPaddingBottom(4)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION DETAILS TABLE
    // ─────────────────────────────────────────────────────────────────────────
    private void addDetailsTable(Document doc, TherapySession session,
                                 PdfFont bold, PdfFont regular,
                                 CurrencyConversionService.ConversionResult prices) {

        String therapistName = THERAPIST_NAMES.getOrDefault(session.getPsychologistId(), "Unknown Therapist");

        String[][] rows = {
                {"Session ID",   "#" + session.getSessionId()},
                {"Therapist",    therapistName},
                {"Date",         session.getSessionDate().format(DATE_FMT)},
                {"Time",         session.getSessionDate().format(TIME_FMT)},
                {"Duration",     session.getDurationMinutes() + " minutes"},
                {"Status",       session.getSessionStatus()},
                {"Session Fee",  "80.00 TND"},
        };

        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .setWidth(UnitValue.createPercentValue(100))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginLeft(40)
                .setMarginRight(40);

        for (int i = 0; i < rows.length; i++) {
            DeviceRgb rowBg = (i % 2 == 0) ? WHITE : GREEN_BG;

            Cell labelCell = new Cell()
                    .add(new Paragraph(rows[i][0]).setFont(bold).setFontSize(10).setFontColor(TEXT_GRAY))
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 0.5f))
                    .setPadding(8);

            // Color-code the status value
            Cell valueCell;
            if (rows[i][0].equals("Status")) {
                DeviceRgb statusColor = getStatusColor(rows[i][1]);
                valueCell = new Cell()
                        .add(new Paragraph(rows[i][1]).setFont(bold).setFontSize(10).setFontColor(statusColor))
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 0.5f))
                        .setPadding(8);
            } else {
                valueCell = new Cell()
                        .add(new Paragraph(rows[i][1]).setFont(regular).setFontSize(10).setFontColor(TEXT_DARK))
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 0.5f))
                        .setPadding(8);
            }

            table.addCell(labelCell);
            table.addCell(valueCell);
        }

        doc.add(table);

        // ── Currency conversion sub-row ──────────────────────────────────────
        if (prices != null && prices.success) {
            String usd = prices.prices.getOrDefault("USD", "—");
            String eur = prices.prices.getOrDefault("EUR", "—");
            String gbp = prices.prices.getOrDefault("GBP", "—");

            Table currencyRow = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginLeft(40)
                    .setMarginRight(40)
                    .setMarginTop(0);

            Cell labelCell = new Cell()
                    .add(new Paragraph("Equivalent (live)").setFont(bold).setFontSize(9).setFontColor(TEXT_GRAY))
                    .setBackgroundColor(GREEN_BG)
                    .setBorder(new SolidBorder(BORDER_GRAY, 0.5f))
                    .setPadding(8);

            Cell valueCell = new Cell()
                    .add(new Paragraph(usd + "   |   " + eur + "   |   " + gbp)
                            .setFont(regular).setFontSize(9).setFontColor(GREEN_DARK))
                    .setBackgroundColor(GREEN_BG)
                    .setBorder(new SolidBorder(BORDER_GRAY, 0.5f))
                    .setPadding(8);

            currencyRow.addCell(labelCell);
            currencyRow.addCell(valueCell);
            doc.add(currencyRow);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION NOTES BLOCK
    // ─────────────────────────────────────────────────────────────────────────
    private void addNotesBlock(Document doc, String notes,
                               PdfFont regular, PdfFont italic) {
        Table noteBox = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginLeft(40)
                .setMarginRight(40);

        Cell cell = new Cell()
                .add(new Paragraph(notes).setFont(regular).setFontSize(10).setFontColor(TEXT_DARK))
                .setBackgroundColor(new DeviceRgb(0xf0, 0xfd, 0xf4))
                .setBorder(new SolidBorder(GREEN_LIGHT, 1f))
                .setBorderLeft(new SolidBorder(GREEN_MID, 4f))
                .setPadding(12);

        noteBox.addCell(cell);
        doc.add(noteBox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEEDBACK BLOCK
    // ─────────────────────────────────────────────────────────────────────────
    private void addFeedbackBlock(Document doc, SessionFeedback feedback,
                                  SentimentAnalysisService.SentimentResult sentiment,
                                  PdfFont bold, PdfFont regular, PdfFont italic) {

        String stars = "★".repeat(feedback.getRating()) + "☆".repeat(5 - feedback.getRating());

        Table feedbackBox = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginLeft(40)
                .setMarginRight(40);

        Cell cell = new Cell()
                .add(new Paragraph("Rating:  " + stars + "  (" + feedback.getRating() + "/5)")
                        .setFont(bold).setFontSize(14).setFontColor(YELLOW_STAR));

        // ── Sentiment badge ───────────────────────────────────────────────
        if (sentiment != null) {
            DeviceRgb badgeColor = getSentimentColor(sentiment.label);
            cell.add(new Paragraph("Sentiment Analysis:  " + sentiment.label)
                    .setFont(bold).setFontSize(10).setFontColor(badgeColor)
                    .setMarginTop(4));
        }

        cell.add(new Paragraph(" "))
                .add(new Paragraph(feedback.getComment() != null && !feedback.getComment().isBlank()
                        ? "\"" + feedback.getComment() + "\""
                        : "No written comment provided.")
                        .setFont(italic).setFontSize(10).setFontColor(TEXT_DARK))
                .setBackgroundColor(new DeviceRgb(0xff, 0xfb, 0xeb))
                .setBorder(new SolidBorder(BORDER_GRAY, 0.5f))
                .setBorderLeft(new SolidBorder(YELLOW_STAR, 4f))
                .setPadding(12);

        feedbackBox.addCell(cell);
        doc.add(feedbackBox);
    }

    private DeviceRgb getSentimentColor(String label) {
        if (label == null) return TEXT_GRAY;
        return switch (label) {
            case "😊 Positive" -> GREEN_MID;
            case "😟 Negative" -> new DeviceRgb(0xd1, 0x43, 0x43);
            case "😐 Neutral"  -> new DeviceRgb(0xf5, 0x9e, 0x0b);
            default            -> TEXT_GRAY;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FOOTER
    // ─────────────────────────────────────────────────────────────────────────
    private void addFooter(Document doc, PdfFont regular, TherapySession session) {
        String generated = "Generated on: " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"));

        doc.add(new Paragraph()
                .add(new Text("MindNest Therapy Management  •  Confidential Patient Record  •  " + generated))
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(TEXT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setBorderTop(new SolidBorder(BORDER_GRAY, 0.5f))
                .setPaddingTop(8)
                .setMarginLeft(40)
                .setMarginRight(40)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Status color mapping
    // ─────────────────────────────────────────────────────────────────────────
    private DeviceRgb getStatusColor(String status) {
        return switch (status) {
            case "Completed"  -> GREEN_MID;
            case "Cancelled"  -> new DeviceRgb(0xd1, 0x43, 0x43); // danger red
            default           -> new DeviceRgb(0x25, 0x63, 0xeb); // scheduled blue
        };
    }
}