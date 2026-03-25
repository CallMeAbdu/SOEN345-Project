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

public class HttpBookingConfirmationDispatcher implements BookingConfirmationDispatcher {
    interface ConnectionFactory {
        HttpURLConnection open(String endpointUrl) throws Exception;
    }

    private static final String TAG = "HttpBookingEmail";
    private static final ExecutorService SHARED_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String RELAY_PATH = "/api/send-booking-confirmation";

    private final String baseUrl;
    private final ExecutorService executor;
    private final ConnectionFactory connectionFactory;

    public HttpBookingConfirmationDispatcher(String baseUrl) {
        this(
                baseUrl,
                SHARED_EXECUTOR,
                endpointUrl -> (HttpURLConnection) new URL(endpointUrl).openConnection()
        );
    }

    HttpBookingConfirmationDispatcher(String baseUrl, ExecutorService executor) {
        this(
                baseUrl,
                executor,
                endpointUrl -> (HttpURLConnection) new URL(endpointUrl).openConnection()
        );
    }

    HttpBookingConfirmationDispatcher(
            String baseUrl,
            ExecutorService executor,
            ConnectionFactory connectionFactory
    ) {
        this.baseUrl = safeString(baseUrl);
        this.executor = executor;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void dispatch(BookingConfirmationDetails details) {
        if (details == null) {
            Log.e(TAG, "Cannot send booking confirmation with null details.");
            return;
        }
        if (isBlank(baseUrl)) {
            Log.e(TAG, "MAIL_RELAY_BASE_URL is empty. Skipping confirmation relay.");
            return;
        }
        if (isBlank(details.getRecipientEmail())) {
            Log.e(TAG, "Recipient email is empty. Skipping confirmation relay.");
            return;
        }

        executor.execute(() -> send(details));
    }

    private void send(BookingConfirmationDetails details) {
        HttpURLConnection connection = null;
        try {
            String endpointUrl = normalizeBaseUrl(baseUrl) + RELAY_PATH;
            connection = connectionFactory.open(endpointUrl);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            JSONObject payload = new JSONObject();
            payload.put("to", details.getRecipientEmail());
            payload.put("subject", BookingConfirmationComposer.buildEmailSubject(details));
            payload.put("text", BookingConfirmationComposer.buildEmailBody(details));

            byte[] payloadBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payloadBytes);
            }

            int code = connection.getResponseCode();
            if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
                String error = readStream(connection.getErrorStream());
                Log.e(TAG, "Mail relay failed with status " + code + ": " + error);
                return;
            }

            Log.i(TAG, "Booking confirmation relay accepted.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to relay booking confirmation: " + e.getMessage(), e);
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

    private String normalizeBaseUrl(String value) {
        String trimmed = safeString(value);
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
