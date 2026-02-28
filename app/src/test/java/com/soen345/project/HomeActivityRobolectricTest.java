package com.soen345.project;

import android.content.Intent;
import android.content.DialogInterface;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.AuthSession;
import com.soen345.project.auth.UserRole;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.event.EventStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HomeActivityRobolectricTest {
    private FakeAuthRepository authRepository;
    private FakeEventRepository eventRepository;

    @Before
    public void setUp() {
        authRepository = new FakeAuthRepository();
        eventRepository = new FakeEventRepository();
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(authRepository));
        EventServiceProvider.setEventServiceForTesting(new EventService(eventRepository));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
        EventServiceProvider.clearEventServiceForTesting();
    }

    @Test
    public void signOutButton_isTopLeftWrapContent() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        Button signOut = activity.findViewById(R.id.homeSignOutButton);
        assertNotNull(signOut);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) signOut.getLayoutParams();
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, params.width);
        assertTrue((params.gravity & Gravity.START) == Gravity.START);
    }

    @Test
    public void adminFlow_showsAdminSectionAndRendersEventRows() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Jazz Night", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        eventRepository.events.add(new Event("doc-2", "event-2", "Old Event", "Talk", "Room B", 1000L, EventStatus.CANCELLED, 50, 0));

        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        View adminSection = activity.findViewById(R.id.homeAdminSection);
        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(View.VISIBLE, adminSection.getVisibility());
        assertEquals(2, eventContainer.getChildCount());
    }

    @Test
    public void identityFallback_showsUnknownUserAndRoleWhenMissing() {
        authRepository.setSignedIn(null, null);
        HomeActivity activity = launchHome(null, null);

        TextView emailText = activity.findViewById(R.id.homeUserEmailText);
        TextView roleText = activity.findViewById(R.id.homeRoleText);
        assertTrue(emailText.getText().toString().contains(activity.getString(R.string.auth_unknown_user)));
        assertTrue(roleText.getText().toString().contains(activity.getString(R.string.auth_unknown_user)));
    }

    @Test
    public void customerFlow_hidesAdminSection() {
        authRepository.setSignedIn("customer@example.com", UserRole.CUSTOMER);
        HomeActivity activity = launchHome("customer@example.com", UserRole.CUSTOMER);

        View adminSection = activity.findViewById(R.id.homeAdminSection);
        assertEquals(View.GONE, adminSection.getVisibility());
    }

    @Test
    public void signedOutFlow_redirectsToMainActivity() {
        authRepository.setSignedOut();
        Intent intent = HomeActivity.newIntent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                "nobody@example.com",
                UserRole.ADMIN
        );
        ActivityController<HomeActivity> controller = Robolectric.buildActivity(HomeActivity.class, intent).create().start().resume();
        HomeActivity activity = controller.get();

        ShadowActivity shadow = shadowOf(activity);
        Intent startedIntent = shadow.getNextStartedActivity();
        assertNotNull(startedIntent);
        assertEquals(MainActivity.class.getName(), startedIntent.getComponent().getClassName());
    }

    @Test
    public void signOutClick_signsOutAndNavigatesToMain() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        Button signOut = activity.findViewById(R.id.homeSignOutButton);
        signOut.performClick();

        assertTrue(authRepository.signOutCalls > 0);
        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(startedIntent);
        assertEquals(MainActivity.class.getName(), startedIntent.getComponent().getClassName());
    }

    @Test
    public void loadEventsError_showsFailureTextAndToast() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.loadError = "db down";

        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        TextView emptyText = activity.findViewById(R.id.homeEventsEmptyText);
        assertEquals(activity.getString(R.string.home_events_load_failed), emptyText.getText().toString());
        assertTrue(String.valueOf(ShadowToast.getTextOfLatestToast()).contains("db down"));
    }

    @Test
    public void addEventDialog_success_callsCreateAndReloads() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        int initialLoadCalls = eventRepository.loadCalls;
        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "100", "80");
        clickPositive(dialog);

        assertEquals(1, eventRepository.createCalls);
        assertNotNull(eventRepository.lastCreatedEvent);
        assertEquals("Concert", eventRepository.lastCreatedEvent.getTitle());
        assertEquals(EventStatus.ACTIVE, eventRepository.lastCreatedEvent.getStatus());
        assertTrue(eventRepository.loadCalls > initialLoadCalls);
    }

    @Test
    public void addEventDialog_whenCreateFails_showsToastWithError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.createError = "permission denied";
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "100", "80");
        clickPositive(dialog);

        assertEquals(1, eventRepository.createCalls);
        String toast = String.valueOf(ShadowToast.getTextOfLatestToast());
        assertTrue(toast.contains(activity.getString(R.string.home_event_save_failed)));
        assertTrue(toast.contains("permission denied"));
    }

    @Test
    public void addEventDialog_withInvalidFields_setsErrorsAndDoesNotCreate() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();

        fillEventDialog(dialog, "", "not-a-date", "", "", "0", "-1");
        clickPositive(dialog);

        EditText title = dialog.findViewById(R.id.dialogEventTitleInput);
        assertNotNull(title);
        assertNotNull(title.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void editEventDialog_success_callsUpdate() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Jazz Night", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        View firstItem = eventContainer.getChildAt(0);
        Button editButton = firstItem.findViewById(R.id.eventItemEditButton);
        editButton.performClick();

        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Jazz Night Updated", "2026-05-16 21:00", "Music", "Hall A", "110", "95");
        clickPositive(dialog);

        assertEquals(1, eventRepository.updateCalls);
        assertNotNull(eventRepository.lastUpdatedEvent);
        assertEquals("Jazz Night Updated", eventRepository.lastUpdatedEvent.getTitle());
    }

    @Test
    public void toggleStatus_callsRepositoryForCancelAndActivate() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Active Event", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        eventRepository.events.add(new Event("doc-2", "event-2", "Cancelled Event", "Talk", "Hall B", 1000L, EventStatus.CANCELLED, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        View firstItem = eventContainer.getChildAt(0);
        View secondItem = eventContainer.getChildAt(1);

        Button statusFirst = firstItem.findViewById(R.id.eventItemStatusButton);
        statusFirst.performClick();
        clickPositive(latestDialog());
        assertEquals(1, eventRepository.statusCalls);
        assertEquals(EventStatus.CANCELLED, eventRepository.lastStatus);

        Button statusSecond = secondItem.findViewById(R.id.eventItemStatusButton);
        statusSecond.performClick();
        clickPositive(latestDialog());
        assertEquals(2, eventRepository.statusCalls);
        assertEquals(EventStatus.ACTIVE, eventRepository.lastStatus);
    }

    @Test
    public void toggleStatus_whenRepositoryFails_showsToast() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.statusError = "status fail";
        eventRepository.events.add(new Event("doc-1", "event-1", "Active Event", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        View firstItem = eventContainer.getChildAt(0);
        Button statusButton = firstItem.findViewById(R.id.eventItemStatusButton);
        statusButton.performClick();
        clickPositive(latestDialog());

        String toast = String.valueOf(ShadowToast.getTextOfLatestToast());
        assertTrue(toast.contains(activity.getString(R.string.home_event_cancel_failed)));
        assertTrue(toast.contains("status fail"));
    }

    @Test
    public void utilityMethods_handleParsingFormattingAndErrors() throws Exception {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        Method parse = HomeActivity.class.getDeclaredMethod("parseDateTimeInputMillis", String.class);
        parse.setAccessible(true);
        long parsed = (Long) parse.invoke(activity, "2026-05-15 20:00");
        long parsedAlt = (Long) parse.invoke(activity, "2026/05/15 20:00");
        long parsedInvalid = (Long) parse.invoke(activity, "invalid");
        assertTrue(parsed > 0L);
        assertTrue(parsedAlt > 0L);
        assertEquals(0L, parsedInvalid);

        Method parseInteger = HomeActivity.class.getDeclaredMethod("parseInteger", String.class);
        parseInteger.setAccessible(true);
        assertEquals(42, ((Integer) parseInteger.invoke(activity, "42")).intValue());
        assertNull(parseInteger.invoke(activity, "4x"));

        Method format = HomeActivity.class.getDeclaredMethod("formatDateTimeMillis", long.class);
        format.setAccessible(true);
        assertEquals("", format.invoke(activity, 0L));
        assertFalse(String.valueOf(format.invoke(activity, parsed)).isEmpty());

        Method buildError = HomeActivity.class.getDeclaredMethod("buildErrorMessage", int.class, String.class);
        buildError.setAccessible(true);
        String plain = (String) buildError.invoke(activity, R.string.home_event_save_failed, "");
        String detailed = (String) buildError.invoke(activity, R.string.home_event_save_failed, "boom");
        assertEquals(activity.getString(R.string.home_event_save_failed), plain);
        assertTrue(detailed.contains("(boom)"));
    }

    private AlertDialog latestDialog() {
        shadowOf(Looper.getMainLooper()).idle();
        android.app.Dialog rawDialog = ShadowDialog.getLatestDialog();
        assertNotNull(rawDialog);
        AlertDialog dialog = (AlertDialog) rawDialog;
        assertNotNull(dialog);
        return dialog;
    }

    private void clickPositive(AlertDialog dialog) {
        shadowOf(Looper.getMainLooper()).idle();
        Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertNotNull(positive);
        positive.performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void fillEventDialog(
            AlertDialog dialog,
            String title,
            String time,
            String category,
            String location,
            String total,
            String remaining
    ) {
        EditText titleInput = dialog.findViewById(R.id.dialogEventTitleInput);
        EditText timeInput = dialog.findViewById(R.id.dialogEventTimeInput);
        EditText categoryInput = dialog.findViewById(R.id.dialogEventCategoryInput);
        EditText locationInput = dialog.findViewById(R.id.dialogEventLocationInput);
        EditText totalInput = dialog.findViewById(R.id.dialogEventCapacityTotalInput);
        EditText remainingInput = dialog.findViewById(R.id.dialogEventCapacityRemainingInput);
        assertNotNull(titleInput);
        assertNotNull(timeInput);
        assertNotNull(categoryInput);
        assertNotNull(locationInput);
        assertNotNull(totalInput);
        assertNotNull(remainingInput);

        titleInput.setText(title);
        timeInput.setText(time);
        categoryInput.setText(category);
        locationInput.setText(location);
        totalInput.setText(total);
        remainingInput.setText(remaining);
    }

    private HomeActivity launchHome(String email, UserRole role) {
        Intent intent = HomeActivity.newIntent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                email,
                role
        );
        return Robolectric.buildActivity(HomeActivity.class, intent).setup().get();
    }

    private static final class FakeAuthRepository implements AuthRepository {
        private boolean signedIn;
        private String signedInEmail;
        private UserRole signedInRole;
        private int signOutCalls;

        void setSignedIn(String email, UserRole role) {
            signedIn = true;
            signedInEmail = email;
            signedInRole = role;
        }

        void setSignedOut() {
            signedIn = false;
            signedInEmail = null;
            signedInRole = null;
        }

        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            signedIn = true;
            signedInEmail = identifier;
            signedInRole = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(identifier, signedInRole));
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            signedIn = true;
            signedInEmail = email;
            signedInRole = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(email, signedInRole));
        }

        @Override
        public boolean isSignedIn() {
            return signedIn;
        }

        @Override
        public String getSignedInEmail() {
            return signedInEmail;
        }

        @Override
        public UserRole getSignedInRole() {
            return signedInRole;
        }

        @Override
        public void signOut() {
            signOutCalls++;
            signedIn = false;
            signedInEmail = null;
            signedInRole = null;
        }
    }

    private static final class FakeEventRepository implements EventRepository {
        private final List<Event> events = new ArrayList<>();
        private Event lastCreatedEvent;
        private Event lastUpdatedEvent;
        private String lastStatusDocumentId;
        private EventStatus lastStatus;
        private String loadError;
        private String createError;
        private String updateError;
        private String statusError;
        private int loadCalls;
        private int createCalls;
        private int updateCalls;
        private int statusCalls;

        @Override
        public void loadEvents(EventListCallback callback) {
            loadCalls++;
            if (loadError != null) {
                callback.onError(loadError);
                return;
            }
            callback.onSuccess(new ArrayList<>(events));
        }

        @Override
        public void createEvent(Event event, EventActionCallback callback) {
            createCalls++;
            lastCreatedEvent = event;
            if (createError != null) {
                callback.onError(createError);
                return;
            }
            events.add(event);
            callback.onSuccess();
        }

        @Override
        public void updateEvent(Event event, EventActionCallback callback) {
            updateCalls++;
            lastUpdatedEvent = event;
            if (updateError != null) {
                callback.onError(updateError);
                return;
            }
            callback.onSuccess();
        }

        @Override
        public void updateStatus(String documentId, EventStatus status, EventActionCallback callback) {
            statusCalls++;
            lastStatusDocumentId = documentId;
            lastStatus = status;
            if (statusError != null) {
                callback.onError(statusError);
                return;
            }
            callback.onSuccess();
        }
    }
}
