package com.soen345.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.AuthSession;
import com.soen345.project.auth.UserRole;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventListenerHandle;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.event.EventStatus;
import com.soen345.project.reservation.Reservation;
import com.soen345.project.reservation.ReservationRepository;
import com.soen345.project.reservation.ReservationService;
import com.soen345.project.reservation.ReservationServiceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MyTicketsActivityRobolectricTest {

    private FakeAuthRepository authRepository;
    private FakeEventRepository eventRepository;
    private FakeReservationRepository reservationRepository;

    @Before
    public void setUp() {
        authRepository = new FakeAuthRepository();
        authRepository.signedIn = true;
        authRepository.signedInEmail = "customer@example.com";
        authRepository.signedInRole = UserRole.CUSTOMER;
        
        eventRepository = new FakeEventRepository();
        reservationRepository = new FakeReservationRepository();

        AuthServiceProvider.setAuthServiceForTesting(new AuthService(authRepository));
        EventServiceProvider.setEventServiceForTesting(new EventService(eventRepository));
        ReservationServiceProvider.setReservationService(new ReservationService(reservationRepository, eventRepository));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
        EventServiceProvider.clearEventServiceForTesting();
        ReservationServiceProvider.clearReservationService();
    }

    @Test
    public void loadMyTickets_displaysReservedEvents() {
        Event event = new Event("d1", "e1", "Jazz Night", "Music", "Montreal", System.currentTimeMillis(), EventStatus.ACTIVE, 100, 50);
        eventRepository.add(event);
        Reservation res = new Reservation("r1", "d1", "customer@example.com", System.currentTimeMillis());
        reservationRepository.add(res);

        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.ticketsContainer);
        assertEquals(1, container.getChildCount());
        TextView titleText = container.getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Jazz Night", titleText.getText().toString());
    }

    @Test
    public void emptyReservations_showsEmptyText() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");
        shadowOf(Looper.getMainLooper()).idle();

        TextView emptyText = activity.findViewById(R.id.ticketsEmptyText);
        assertEquals(View.VISIBLE, emptyText.getVisibility());
    }

    @Test
    public void clickCancel_showsConfirmationDialog() {
        Event event = new Event("d1", "e1", "Jazz Night", "Music", "Montreal", System.currentTimeMillis(), EventStatus.ACTIVE, 100, 50);
        eventRepository.add(event);
        Reservation res = new Reservation("r1", "d1", "customer@example.com", System.currentTimeMillis());
        reservationRepository.add(res);

        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");
        shadowOf(Looper.getMainLooper()).idle();

        View ticketView = ((LinearLayout) activity.findViewById(R.id.ticketsContainer)).getChildAt(0);
        ticketView.findViewById(R.id.browseEventReserveButton).performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Cancel Reservation", shadowOf(dialog).getTitle());
    }

    @Test
    public void confirmCancel_removesTicketAndShowsToast() {
        Event event = new Event("d1", "e1", "Jazz Night", "Music", "Montreal", System.currentTimeMillis(), EventStatus.ACTIVE, 100, 50);
        eventRepository.add(event);
        Reservation res = new Reservation("r1", "d1", "customer@example.com", System.currentTimeMillis());
        reservationRepository.add(res);

        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");
        shadowOf(Looper.getMainLooper()).idle();

        View ticketView = ((LinearLayout) activity.findViewById(R.id.ticketsContainer)).getChildAt(0);
        ticketView.findViewById(R.id.browseEventReserveButton).performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Reservation cancelled", ShadowToast.getTextOfLatestToast());
        LinearLayout container = activity.findViewById(R.id.ticketsContainer);
        assertEquals(0, container.getChildCount());
    }

    @Test
    public void toolbar_showsEmailInSubtitle() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        com.google.android.material.appbar.MaterialToolbar toolbar =
                activity.findViewById(R.id.ticketsToolbar);
        assertNotNull(toolbar);
        assertNotNull(toolbar.getSubtitle());
        assertTrue(toolbar.getSubtitle().toString().contains("customer@example.com"));
    }

    @Test
    public void signOut_viaMenu_navigatesToMain() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        shadowOf(Looper.getMainLooper()).idle();
        shadowOf(activity).clickMenuItem(R.id.action_sign_out);
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void onStart_whenNotSignedIn_redirectsToMain() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        authRepository.signedIn = false;
        activity.onStart();
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void findEventById_returnsCorrectEvent() throws Exception {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");
        
        List<Event> events = new ArrayList<>();
        Event target = new Event("d1", "e1", "Target", "C", "L", 0L, EventStatus.ACTIVE, 10, 5);
        events.add(target);
        events.add(new Event("d2", "e2", "Other", "C", "L", 0L, EventStatus.ACTIVE, 10, 5));

        java.lang.reflect.Method method = MyTicketsActivity.class.getDeclaredMethod("findEventById", String.class, List.class);
        method.setAccessible(true);
        
        Event result = (Event) method.invoke(activity, "d1", events);
        assertEquals("Target", result.getTitle());
        
        Event notFound = (Event) method.invoke(activity, "unknown", events);
        assertNull(notFound);
    }

    @Test
    public void renderTickets_skipsUnknownEvents() throws Exception {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");
        
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("r1", "unknown_event", "user@test.com", 0L));
        
        List<Event> events = new ArrayList<>();
        events.add(new Event("d1", "e1", "Known", "C", "L", 0L, EventStatus.ACTIVE, 10, 5));

        java.lang.reflect.Method method = MyTicketsActivity.class.getDeclaredMethod("renderTickets", List.class, List.class);
        method.setAccessible(true);
        method.invoke(activity, reservations, events);
        
        LinearLayout container = activity.findViewById(R.id.ticketsContainer);
        assertEquals(0, container.getChildCount());
    }

    private void assertNull(Object obj) {
        assertTrue(obj == null);
    }

    private MyTicketsActivity launch(String email, String role) {
        Intent intent = MyTicketsActivity.newIntent(
                ApplicationProvider.getApplicationContext(), email, role);
        return Robolectric.buildActivity(MyTicketsActivity.class, intent).setup().get();
    }

    private static final class FakeAuthRepository implements AuthRepository {
        boolean signedIn;
        String signedInEmail;
        UserRole signedInRole;

        @Override public void signIn(String id, String pw, AuthCallback cb) {
            cb.onSuccess(new AuthSession(id, UserRole.CUSTOMER));
        }
        @Override public void register(String e, String p, String pw, AuthCallback cb) {
            cb.onSuccess(new AuthSession(e, UserRole.CUSTOMER));
        }
        @Override public boolean isSignedIn() { return signedIn; }
        @Override public String getSignedInEmail() { return "customer@example.com"; }
        @Override public UserRole getSignedInRole() { return UserRole.CUSTOMER; }
        @Override public void signOut() {
            signedIn = false;
            signedInEmail = null;
            signedInRole = null;
        }
    }

    private static final class FakeEventRepository implements EventRepository {
        private final List<Event> events = new ArrayList<>();
        void add(Event e) { events.add(e); }
        @Override public void loadEvents(EventListCallback cb) { cb.onSuccess(new ArrayList<>(events)); }
        @Override public EventListenerHandle listenToEvents(EventListCallback cb) { return () -> {}; }
        @Override public void createEvent(Event e, EventActionCallback cb) { cb.onSuccess(); }
        @Override public void updateEvent(Event e, EventActionCallback cb) { cb.onSuccess(); }
        @Override public void updateStatus(String id, EventStatus s, EventActionCallback cb) { cb.onSuccess(); }
    }

    private static final class FakeReservationRepository implements ReservationRepository {
        private final List<Reservation> reservations = new ArrayList<>();
        void add(Reservation r) { reservations.add(r); }
        @Override public void createReservation(Reservation r, ReservationActionCallback cb) { cb.onSuccess(); }
        @Override public void cancelReservation(String id, ReservationActionCallback cb) {
            reservations.removeIf(r -> r.getDocumentId().equals(id));
            cb.onSuccess();
        }
        @Override public void getReservationsForUser(String email, ReservationListCallback cb) {
            List<Reservation> userRes = new ArrayList<>();
            for(Reservation r : reservations) if(r.getUserEmail().equals(email)) userRes.add(r);
            cb.onSuccess(userRes);
        }
    }
}
