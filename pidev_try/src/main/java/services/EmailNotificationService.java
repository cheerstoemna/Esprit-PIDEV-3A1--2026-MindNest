package services;

// ═══════════════════════════════════════════════════════════════════════════════
//  FILE: src/main/java/services/EmailNotificationService.java
//  Sends session notification emails via Gmail SMTP (JavaMail API)
//
//  WHEN YOU ADD REAL USERS:
//  ─────────────────────────
//  Replace the RECIPIENT_EMAIL constant with the user's email from DB:
//
//    // TODO: swap this constant for dynamic user email
//    String recipient = UserSession.get().user().getEmail(); // ← uncomment when ready
//
//  Make sure your AppUser model has getEmail() returning the user's email address.
//  If your DB column is called "email", add this to AppUser.java:
//    private String email;
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//  And load it in your user query:
//    user.setEmail(rs.getString("email"));
// ═══════════════════════════════════════════════════════════════════════════════

import models.TherapySession;

import java.time.format.DateTimeFormatter;
import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailNotificationService {

    // ── Sender credentials (Gmail SMTP) ──────────────────────────────────────
    private static final String SENDER_EMAIL = "emnanasraoui7@gmail.com";
    private static final String APP_PASSWORD  = "yiwe hpbk nqno pxcn";  // Gmail app password

    // ── Recipient ─────────────────────────────────────────────────────────────
    // TODO: when you have real users, replace this with:
    //   private String getRecipientEmail() {
    //       return utils.UserSession.get().user().getEmail();
    //   }
    private static final String RECIPIENT_EMAIL = "emna_nasraoui@icloud.com"; // ← hardcoded for now

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC METHODS — called from ClientTherapyController
    // ─────────────────────────────────────────────────────────────────────────

    /** Called after a session is successfully booked */
    public void sendBookingConfirmation(TherapySession session, String therapistName) {
        String subject = " Session Booked — MindNest";
        String body = """
                Hello there and welcome to MindNest,

                Your therapy session has been successfully booked.

                ──────────────────────────────
                 Date & Time : %s
                 Therapist   : Dr. %s
                 Duration    : %d minutes
                 Status      : %s
                ──────────────────────────────

                If you need to reschedule or cancel, please do so at least 24 hours in advance.

                Take care,
                MindNest Therapy Team
                """.formatted(
                session.getSessionDate().format(FMT),
                therapistName,
                session.getDurationMinutes(),
                session.getSessionStatus()
        );
        sendAsync(subject, body);
    }

    /** Called after a session is cancelled */
    public void sendCancellationNotice(TherapySession session, String therapistName) {
        String subject = " Session Cancelled — MindNest";
        String body = """
                Hello there this is MindNest,

                Your therapy session has been cancelled.

                ──────────────────────────────
                 Date & Time : %s
                 Therapist   : Dr. %s
                 Duration    : %d minutes
                ──────────────────────────────

                You can book a new session anytime through MindNest.

                Take care,
                MindNest Therapy Team
                """.formatted(
                session.getSessionDate().format(FMT),
                therapistName,
                session.getDurationMinutes()
        );
        sendAsync(subject, body);
    }

    /** Called after a session is updated */
    public void sendUpdateNotice(TherapySession session, String therapistName) {
        String subject = " Session Updated — MindNest";
        String body = """
                Hello there here is MindNest,

                Your therapy session has been updated. Here are the new details:

                ──────────────────────────────
                 Date & Time : %s
                 Therapist   : Dr. %s
                 Duration    : %d minutes
                 Status      : %s
                ──────────────────────────────

                If this change was not made by you, please contact support.

                Take care,
                MindNest Therapy Team
                """.formatted(
                session.getSessionDate().format(FMT),
                therapistName,
                session.getDurationMinutes(),
                session.getSessionStatus()
        );
        sendAsync(subject, body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL — runs email sending on a background thread so UI never freezes
    // ─────────────────────────────────────────────────────────────────────────
    private void sendAsync(String subject, String body) {
        // Fire and forget on a background thread — same pattern as sentiment analysis
        new Thread(() -> {
            try {
                send(subject, body);
                System.out.println("[Email] Sent: " + subject);
            } catch (Exception e) {
                System.err.println("[Email] Failed: " + e.getMessage());
            }
        }).start();
    }

    private void send(String subject, String body) throws MessagingException {
        // ── Gmail SMTP config ─────────────────────────────────────────────────
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(mailSession);
        try {
            message.setFrom(new InternetAddress(SENDER_EMAIL, "MindNest Therapy"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(SENDER_EMAIL));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(
                // TODO: when you have real users, replace RECIPIENT_EMAIL with:
                //   utils.UserSession.get().user().getEmail()
                RECIPIENT_EMAIL
        ));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}