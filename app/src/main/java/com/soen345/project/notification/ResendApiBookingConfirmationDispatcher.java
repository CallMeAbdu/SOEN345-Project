package com.soen345.project.notification;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class ResendApiBookingConfirmationDispatcher implements BookingConfirmationDispatcher {
    private static final String TAG = "ResendEmail";
    private static final String RESEND_SEND_URL = "https://api.resend.com/emails";
    private static final ExecutorService SHARED_EXECUTOR = Executors.newSingleThreadExecutor();

    private final String apiKey;
    private final String fromEmail;
    private final ExecutorService executor;

    public ResendApiBookingConfirmationDispatcher(String apiKey, String fromEmail) {
        this(apiKey, fromEmail, SHARED_EXECUTOR);
    }

    ResendApiBookingConfirmationDispatcher(String apiKey, String fromEmail, ExecutorService executor) {
        this.apiKey = safeString(apiKey);
        this.fromEmail = safeString(fromEmail);
        this.executor = executor;
    }

    @Override
    public void dispatch(BookingConfirmationDetails details) {
        if (details == null) {
            Log.e(TAG, "Cannot send booking confirmation with null details.");
            return;
        }
        if (isBlank(apiKey)) {
            Log.e(TAG, "RESEND_API_KEY is empty. Skipping email send.");
            return;
        }
        if (isBlank(fromEmail)) {
            Log.e(TAG, "RESEND_FROM_EMAIL is empty. Skipping email send.");
            return;
        }
        if (isBlank(details.getRecipientEmail())) {
            Log.e(TAG, "Recipient email is empty. Skipping email send.");
            return;
        }

        executor.execute(() -> sendEmail(details));
    }

    private void sendEmail(BookingConfirmationDetails details) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(RESEND_SEND_URL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            payload.put("from", fromEmail);
            payload.put("to", details.getRecipientEmail());
            payload.put("subject", BookingConfirmationComposer.buildEmailSubject(details));
            payload.put("html", BookingConfirmationComposer.buildEmailHtml(details));
            payload.put("text", BookingConfirmationComposer.buildEmailBody(details));

            byte[] payloadBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payloadBytes);
            }

            int statusCode = connection.getResponseCode();
            if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                String errorBody = readStream(connection.getErrorStream());
                Log.e(TAG, "Resend request failed with status " + statusCode + ": " + errorBody);
                return;
            }

            String responseBody = readStream(connection.getInputStream());
            Log.i(TAG, "Reservation confirmation email accepted by Resend: " + responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send reservation confirmation email: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
