package com.soen345.project.notification;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.HttpsURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ResendApiBookingConfirmationDispatcherTest {

    @Test
    public void dispatch_withNullDetails_skipsExecution() {
        ExecutorService executor = mock(ExecutorService.class);
        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                "sender@example.com",
                executor,
                () -> mock(HttpsURLConnection.class)
        );

        dispatcher.dispatch(null);

        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    public void dispatch_withBlankApiKey_skipsExecution() {
        ExecutorService executor = mock(ExecutorService.class);
        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "   ",
                "sender@example.com",
                executor,
                () -> mock(HttpsURLConnection.class)
        );

        dispatcher.dispatch(validDetails());

        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    public void dispatch_withBlankFromEmail_skipsExecution() {
        ExecutorService executor = mock(ExecutorService.class);
        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                " ",
                executor,
                () -> mock(HttpsURLConnection.class)
        );

        dispatcher.dispatch(validDetails());

        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    public void dispatch_withBlankRecipient_skipsExecution() {
        ExecutorService executor = mock(ExecutorService.class);
        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                "sender@example.com",
                executor,
                () -> mock(HttpsURLConnection.class)
        );
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                "   ",
                "Jazz Night",
                "Montreal Hall",
                1735792200000L,
                1,
                1735705800000L
        );

        dispatcher.dispatch(details);

        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    public void dispatch_success_postsPayloadAndDisconnects() throws Exception {
        ExecutorService executor = immediateExecutor();
        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

        when(connection.getOutputStream()).thenReturn(bodyStream);
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream()).thenReturn(
                new ByteArrayInputStream("{\"id\":\"email_123\"}".getBytes(StandardCharsets.UTF_8))
        );

        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                "sender@example.com",
                executor,
                () -> connection
        );

        dispatcher.dispatch(validDetails());

        verify(connection).setRequestMethod("POST");
        verify(connection).setRequestProperty("Authorization", "Bearer api-key");
        verify(connection).setRequestProperty("Content-Type", "application/json");
        verify(connection).disconnect();

        String payload = new String(bodyStream.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"from\":\"sender@example.com\""));
        assertTrue(payload.contains("\"to\":\"customer@example.com\""));
        assertTrue(payload.contains("\"subject\":\"Booking confirmation - Jazz Night\""));
        assertTrue(payload.contains("\"text\":"));
        assertTrue(payload.contains("\"html\":"));
    }

    @Test
    public void dispatch_errorResponse_readsErrorStreamAndSkipsInputStream() throws Exception {
        ExecutorService executor = immediateExecutor();
        HttpsURLConnection connection = mock(HttpsURLConnection.class);

        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(connection.getErrorStream()).thenReturn(
                new ByteArrayInputStream("{\"error\":\"invalid payload\"}".getBytes(StandardCharsets.UTF_8))
        );

        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                "sender@example.com",
                executor,
                () -> connection
        );

        dispatcher.dispatch(validDetails());

        verify(connection, never()).getInputStream();
        verify(connection).disconnect();
    }

    @Test
    public void dispatch_handlesNullErrorStream() throws Exception {
        ExecutorService executor = immediateExecutor();
        HttpsURLConnection connection = mock(HttpsURLConnection.class);

        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(connection.getErrorStream()).thenReturn(null);

        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                "sender@example.com",
                executor,
                () -> connection
        );

        dispatcher.dispatch(validDetails());

        verify(connection).disconnect();
    }

    @Test
    public void dispatch_handlesStreamReadFailure() throws Exception {
        ExecutorService executor = immediateExecutor();
        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        InputStream brokenInput = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream read failed");
            }
        };

        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream()).thenReturn(brokenInput);

        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                "sender@example.com",
                executor,
                () -> connection
        );

        dispatcher.dispatch(validDetails());

        verify(connection).disconnect();
    }

    @Test
    public void dispatch_handlesConnectionFactoryFailure() {
        ExecutorService executor = immediateExecutor();
        ResendApiBookingConfirmationDispatcher dispatcher = new ResendApiBookingConfirmationDispatcher(
                "api-key",
                "sender@example.com",
                executor,
                () -> {
                    throw new IOException("connection failed");
                }
        );

        dispatcher.dispatch(validDetails());
    }

    private static ExecutorService immediateExecutor() {
        ExecutorService executor = mock(ExecutorService.class);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        return executor;
    }

    private static BookingConfirmationDetails validDetails() {
        return new BookingConfirmationDetails(
                "customer@example.com",
                "Jazz Night",
                "Montreal Hall",
                1735792200000L,
                2,
                1735705800000L
        );
    }
}
