package com.soen345.project.notification;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HttpBookingConfirmationDispatcherTest {

    @Test
    public void dispatch_withNullDetails_skipsExecution() {
        ExecutorService executor = mock(ExecutorService.class);
        HttpBookingConfirmationDispatcher dispatcher = new HttpBookingConfirmationDispatcher(
                "http://10.0.2.2:8080",
                executor
        );

        dispatcher.dispatch(null);

        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    public void dispatch_withBlankBaseUrl_skipsExecution() {
        ExecutorService executor = mock(ExecutorService.class);
        HttpBookingConfirmationDispatcher dispatcher = new HttpBookingConfirmationDispatcher(
                "   ",
                executor
        );

        dispatcher.dispatch(validDetails());

        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    public void dispatch_withBlankRecipient_skipsExecution() {
        ExecutorService executor = mock(ExecutorService.class);
        HttpBookingConfirmationDispatcher dispatcher = new HttpBookingConfirmationDispatcher(
                "http://10.0.2.2:8080",
                executor
        );
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                " ",
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
    public void dispatch_success_postsExpectedJson() throws Exception {
        ExecutorService executor = immediateExecutor();
        HttpURLConnection connection = mock(HttpURLConnection.class);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        when(connection.getOutputStream()).thenReturn(body);
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        HttpBookingConfirmationDispatcher dispatcher = new HttpBookingConfirmationDispatcher(
                "http://10.0.2.2:8080",
                executor,
                endpointUrl -> {
                    assertTrue(endpointUrl.endsWith("/api/send-booking-confirmation"));
                    return connection;
                }
        );

        dispatcher.dispatch(validDetails());

        verify(connection).setRequestMethod("POST");
        verify(connection).setRequestProperty(contains("Content-Type"), contains("application/json"));
        verify(connection).disconnect();

        String payload = new String(body.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"to\":\"customer@example.com\""));
        assertTrue(payload.contains("\"subject\":\"Booking confirmation - Jazz Night\""));
        assertTrue(payload.contains("\"text\":"));
    }

    @Test
    public void dispatch_withTrailingSlashBaseUrl_normalizesEndpoint() throws Exception {
        ExecutorService executor = immediateExecutor();
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        HttpBookingConfirmationDispatcher dispatcher = new HttpBookingConfirmationDispatcher(
                "http://10.0.2.2:8080/",
                executor,
                endpointUrl -> {
                    assertTrue(endpointUrl.equals("http://10.0.2.2:8080/api/send-booking-confirmation"));
                    return connection;
                }
        );

        dispatcher.dispatch(validDetails());

        verify(connection).disconnect();
    }

    @Test
    public void dispatch_errorResponse_readsErrorStreamAndDisconnects() throws Exception {
        ExecutorService executor = immediateExecutor();
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(connection.getErrorStream()).thenReturn(
                new ByteArrayInputStream("{\"error\":\"invalid\"}".getBytes(StandardCharsets.UTF_8))
        );

        HttpBookingConfirmationDispatcher dispatcher = new HttpBookingConfirmationDispatcher(
                "http://10.0.2.2:8080",
                executor,
                endpointUrl -> connection
        );

        dispatcher.dispatch(validDetails());

        verify(connection).disconnect();
    }

    @Test
    public void dispatch_handlesConnectionFailure() {
        ExecutorService executor = immediateExecutor();
        HttpBookingConfirmationDispatcher dispatcher = new HttpBookingConfirmationDispatcher(
                "http://10.0.2.2:8080",
                executor,
                endpointUrl -> {
                    throw new RuntimeException("connection failed");
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
