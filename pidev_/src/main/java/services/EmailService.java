package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;

/**
 * EmailService — Gmail SMTP via SSL (port 465)
 *
 * SETUP:
 * 1. Enable 2-Step Verification: https://myaccount.google.com/security
 * 2. Generate App Password:      https://myaccount.google.com/apppasswords
 * 3. Fill in SENDER_EMAIL and APP_PASSWORD below (no spaces in app password)
 */
public class EmailService {

    // ── Replace these two values ──────────────────────────────────────
    private static final String SENDER_EMAIL = "chebbimohamed119@gmail.com";
    private static final String APP_PASSWORD  = "gaxv fsxu pygr vyti"; // 16 chars, no spaces
    // ─────────────────────────────────────────────────────────────────

    public boolean sendOtp(String toEmail, String otp) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host",            "smtp.gmail.com");
            props.put("mail.smtp.port",            "465");
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.socketFactory.port",   "465");
            props.put("mail.smtp.socketFactory.class",  "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.ssl.enable",      "true");
            props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");
            props.put("mail.debug",                "false");

            final String user = SENDER_EMAIL;
            final String pass = APP_PASSWORD;

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user, "MindNest"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("MindNest — Your Password Reset Code");
            message.setContent(buildEmailBody(otp), "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("[EmailService] OTP sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("[EmailService] Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String buildEmailBody(String otp) {
        return """
            <div style="font-family: 'Segoe UI', sans-serif; max-width: 480px; margin: auto;
                        background: #f9f9f9; border-radius: 12px; padding: 40px;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 36px;">🌿</span>
                    <h2 style="color: #2c3e50; margin: 8px 0 0;">MindNest</h2>
                    <p style="color: #95a5a6; font-size: 13px;">A safe space for your thoughts</p>
                </div>
                <div style="background: white; border-radius: 10px; padding: 30px; text-align: center;
                            box-shadow: 0 2px 12px rgba(0,0,0,0.08);">
                    <p style="color: #2c3e50; font-size: 15px; margin-bottom: 20px;">
                        Use the code below to reset your password.<br>
                        <strong>This code expires in 10 minutes.</strong>
                    </p>
                    <div style="background: #f0f0f8; border-radius: 10px; padding: 20px 40px;
                                display: inline-block; margin: 16px 0;">
                        <span style="font-size: 36px; font-weight: bold; color: #5b4b8a;
                                     letter-spacing: 8px;">%s</span>
                    </div>
                    <p style="color: #95a5a6; font-size: 12px; margin-top: 20px;">
                        If you didn't request this, you can safely ignore this email.
                    </p>
                </div>
                <p style="text-align: center; color: #bdc3c7; font-size: 11px; margin-top: 24px;">
                    © 2026 MindNest. All rights reserved.
                </p>
            </div>
        """.formatted(otp);
    }
}
