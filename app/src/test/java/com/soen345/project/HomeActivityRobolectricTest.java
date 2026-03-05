package com.soen345.project;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.soen345.project.event.EventListenerHandle;
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

import java.util.ArrayList;
import java.util.Calendar;
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
    public void toolbar_showsTitleAndAdminEmailSubtitle() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                activity.findViewById(R.id.homeToolbar);
        assertNotNull(toolbar);
        assertEquals(activity.getString(R.string.home_admin_title), toolbar.getTitle().toString());
        assertNotNull(toolbar.getSubtitle());
        assertTrue(toolbar.getSubtitle().toString().contains("admin@example.com"));
    }

    @Test
    public void adminFlow_showsAdminSectionAndRendersEventRows() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Jazz Night", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        eventRepository.events.add(new Event("doc-2", "event-2", "Old Event", "Talk", "Room B", 1000L, EventStatus.CANCELLED, 50, 0));

        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        // homeAdminSection removed — admin UI is the full HomeActivity
        // Verify FAB is visible and both events rendered
        View fab = activity.findViewById(R.id.homeAddEventButton);
        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(View.VISIBLE, fab.getVisibility());
        assertEquals(2, eventContainer.getChildCount());
    }

    @Test
    public void toolbar_withNullEmail_showsUnknownUserInSubtitle() {
        authRepository.setSignedIn(null, null);
        HomeActivity activity = launchHome(null, null);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                activity.findViewById(R.id.homeToolbar);
        assertNotNull(toolbar);
        assertNotNull(toolbar.getSubtitle());
        assertTrue(toolbar.getSubtitle().toString()
                .contains(activity.getString(R.string.auth_unknown_user)));
    }

    @Test
    public void customerFlow_redirectsToBrowseEventsActivity() {
        authRepository.setSignedIn("customer@example.com", UserRole.CUSTOMER);
        Intent intent = HomeActivity.newIntent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                "customer@example.com",
                UserRole.CUSTOMER
        );
        ActivityController<HomeActivity> controller =
                Robolectric.buildActivity(HomeActivity.class, intent).create().start().resume();
        HomeActivity activity = controller.get();

        ShadowActivity shadow = shadowOf(activity);
        Intent startedIntent = shadow.getNextStartedActivity();
        assertNotNull(startedIntent);
        assertNotNull(startedIntent.getComponent());
        assertEquals(BrowseEventsActivity.class.getName(), startedIntent.getComponent().getClassName());
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
        assertNotNull(startedIntent.getComponent());
        assertEquals(MainActivity.class.getName(), startedIntent.getComponent().getClassName());
    }

    @Test
    public void signOut_viaMenu_signsOutAndNavigatesToMain() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        shadowOf(Looper.getMainLooper()).idle();
        // Trigger the sign out menu item via Robolectric shadow
        shadowOf(activity).clickMenuItem(R.id.action_sign_out);
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(authRepository.signOutCalls > 0);
        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(startedIntent);
        assertNotNull(startedIntent.getComponent());
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

        int initialListenCalls = eventRepository.listenCalls;
        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "100", "80");
        clickPositive(dialog);

        assertEquals(1, eventRepository.createCalls);
        assertNotNull(eventRepository.lastCreatedEvent);
        assertEquals("Concert", eventRepository.lastCreatedEvent.getTitle());
        assertEquals(EventStatus.ACTIVE, eventRepository.lastCreatedEvent.getStatus());
        assertTrue(eventRepository.listenCalls > initialListenCalls);
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
        assertNotNull(title); assertNotNull(title.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_timeRequired_setsTimeError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "", "Music", "Bell", "100", "80");
        clickPositive(dialog);

        EditText timeInput = dialog.findViewById(R.id.dialogEventTimeInput);
        assertNotNull(timeInput);
        assertNotNull(timeInput); assertNotNull(timeInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_invalidTimeFormat_setsTimeErrorAfterParseAttempt() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "bad-time", "Music", "Bell", "100", "80");
        clickPositive(dialog);

        EditText timeInput = dialog.findViewById(R.id.dialogEventTimeInput);
        assertNotNull(timeInput);
        assertNotNull(timeInput); assertNotNull(timeInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_categoryRequired_setsCategoryError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "", "Bell", "100", "80");
        clickPositive(dialog);

        EditText categoryInput = dialog.findViewById(R.id.dialogEventCategoryInput);
        assertNotNull(categoryInput);
        assertNotNull(categoryInput); assertNotNull(categoryInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_locationRequired_setsLocationError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "", "100", "80");
        clickPositive(dialog);

        EditText locationInput = dialog.findViewById(R.id.dialogEventLocationInput);
        assertNotNull(locationInput);
        assertNotNull(locationInput); assertNotNull(locationInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_capacityTotalRequired_setsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "", "10");
        clickPositive(dialog);

        EditText totalInput = dialog.findViewById(R.id.dialogEventCapacityTotalInput);
        assertNotNull(totalInput);
        assertNotNull(totalInput); assertNotNull(totalInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_capacityTotalInvalid_setsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "0", "0");
        clickPositive(dialog);

        EditText totalInput = dialog.findViewById(R.id.dialogEventCapacityTotalInput);
        assertNotNull(totalInput);
        assertNotNull(totalInput); assertNotNull(totalInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_capacityRemainingInvalid_setsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "10", "-1");
        clickPositive(dialog);

        EditText remainingInput = dialog.findViewById(R.id.dialogEventCapacityRemainingInput);
        assertNotNull(remainingInput);
        assertNotNull(remainingInput); assertNotNull(remainingInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_capacityRemainingExceedsTotal_setsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "10", "11");
        clickPositive(dialog);

        EditText remainingInput = dialog.findViewById(R.id.dialogEventCapacityRemainingInput);
        assertNotNull(remainingInput);
        assertNotNull(remainingInput); assertNotNull(remainingInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_whenRemainingEmpty_defaultsToTotal() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Bell", "10", "");
        clickPositive(dialog);

        assertEquals(1, eventRepository.createCalls);
        assertNotNull(eventRepository.lastCreatedEvent);
        assertEquals(10, eventRepository.lastCreatedEvent.getCapacityRemaining());
    }

    @Test
    public void editEventDialog_success_callsUpdate() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Jazz Night", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        View firstItem = eventContainer.getChildAt(0);
        android.widget.ImageButton editButton = firstItem.findViewById(R.id.eventItemEditButton);
        editButton.performClick();

        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Jazz Night Updated", "2026-05-16 21:00", "Music", "Hall A", "110", "95");
        clickPositive(dialog);

        assertEquals(1, eventRepository.updateCalls);
        assertNotNull(eventRepository.lastUpdatedEvent);
        assertEquals("Jazz Night Updated", eventRepository.lastUpdatedEvent.getTitle());
    }

    @Test
    public void editEventDialog_whenUpdateFails_showsToastWithError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.updateError = "update denied";
        eventRepository.events.add(new Event("doc-1", "event-1", "Jazz Night", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        View firstItem = eventContainer.getChildAt(0);
        android.widget.ImageButton editButton = firstItem.findViewById(R.id.eventItemEditButton);
        editButton.performClick();

        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Jazz Night Updated", "2026-05-16 21:00", "Music", "Hall A", "110", "95");
        clickPositive(dialog);

        String toast = String.valueOf(ShadowToast.getTextOfLatestToast());
        assertTrue(toast.contains(activity.getString(R.string.home_event_save_failed)));
        assertTrue(toast.contains("update denied"));
        assertEquals(1, eventRepository.updateCalls);
    }

    @Test
    public void toggleStatus_activeEvent_sendsCancel() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Active Event", "Music", "Hall A", 2000L, EventStatus.ACTIVE, 100, 80));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        Button statusButton = eventContainer.getChildAt(0).findViewById(R.id.eventItemStatusButton);
        statusButton.performClick();
        clickPositive(latestDialog());

        assertEquals(1, eventRepository.statusCalls);
        assertEquals(EventStatus.CANCELLED, eventRepository.lastStatus);
    }

    @Test
    public void toggleStatus_cancelledEvent_sendsActivate() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Cancelled Event", "Music", "Hall A", 2000L, EventStatus.CANCELLED, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        Button statusButton = eventContainer.getChildAt(0).findViewById(R.id.eventItemStatusButton);
        statusButton.performClick();
        clickPositive(latestDialog());

        assertEquals(1, eventRepository.statusCalls);
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
    public void renderEvent_withBlankFields_usesFallbackLabelsAndUntitledInConfirmDialog() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "   ", "   ", "   ", 0L, EventStatus.ACTIVE, 10, 5));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        View firstItem = eventContainer.getChildAt(0);
        TextView title = firstItem.findViewById(R.id.eventItemTitle);
        TextView details = firstItem.findViewById(R.id.eventItemDetails);
        Button statusButton = firstItem.findViewById(R.id.eventItemStatusButton);

        assertEquals(activity.getString(R.string.home_event_untitled), title.getText().toString());
        String detailText = details.getText().toString();
        assertTrue(detailText.contains(activity.getString(R.string.home_event_no_time)));
        assertTrue(detailText.contains(activity.getString(R.string.home_event_no_category)));
        assertTrue(detailText.contains(activity.getString(R.string.home_event_no_location)));

        statusButton.performClick();
        AlertDialog confirmDialog = latestDialog();
        TextView messageView = confirmDialog.findViewById(android.R.id.message);
        assertNotNull(messageView);
        assertTrue(messageView.getText().toString().contains(activity.getString(R.string.home_event_untitled)));
    }

    @Test
    public void timeInputClick_opensDateTimePickers_andSetsFieldValue() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog eventDialog = latestDialog();
        EditText timeInput = eventDialog.findViewById(R.id.dialogEventTimeInput);
        assertNotNull(timeInput);
        timeInput.setError("old");
        timeInput.performClick();

        shadowOf(Looper.getMainLooper()).idle();
        android.app.Dialog rawDateDialog = ShadowDialog.getLatestDialog();
        assertTrue(rawDateDialog instanceof DatePickerDialog);
        DatePickerDialog dateDialog = (DatePickerDialog) rawDateDialog;
        dateDialog.getDatePicker().updateDate(2026, 4, 15);
        Button datePositive = dateDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertNotNull(datePositive);
        datePositive.performClick();

        shadowOf(Looper.getMainLooper()).idle();
        android.app.Dialog rawTimeDialog = ShadowDialog.getLatestDialog();
        assertTrue(rawTimeDialog instanceof TimePickerDialog);
        TimePickerDialog timeDialog = (TimePickerDialog) rawTimeDialog;
        timeDialog.updateTime(21, 30);
        Button timePositive = timeDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertNotNull(timePositive);
        timePositive.performClick();

        assertFalse(timeInput.getText().toString().isEmpty());
        assertNull(timeInput.getError());
    }

    @Test
    public void alternateDateFormat_isAcceptedThroughDialogFlow() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026/05/15 20:00", "Music", "Bell", "100", "80");
        clickPositive(dialog);

        assertEquals(1, eventRepository.createCalls);
        assertNotNull(eventRepository.lastCreatedEvent);
        assertTrue(eventRepository.lastCreatedEvent.getDateTimeMillis() > 0L);
    }

    @Test
    public void eventDetails_useFallbackLabelsForNullValues() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("doc-1", "event-1", "Title", null, null, 1000L, EventStatus.ACTIVE, 10, 5));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout eventContainer = activity.findViewById(R.id.homeEventsContainer);
        View firstItem = eventContainer.getChildAt(0);
        TextView details = firstItem.findViewById(R.id.eventItemDetails);
        String detailText = details.getText().toString();

        assertTrue(detailText.contains(activity.getString(R.string.home_event_no_category)));
        assertTrue(detailText.contains(activity.getString(R.string.home_event_no_location)));
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
        private int listenCalls;
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


        private boolean listenerRemoved;

        @Override
        public EventListenerHandle listenToEvents(EventListCallback callback) {
            listenCalls++;
            if (callback != null) {
                if (loadError != null) callback.onError(loadError);
                else callback.onSuccess(new ArrayList<>(events));
            }
            return new EventListenerHandle() {
                @Override
                public void remove() {
                    listenerRemoved = true;
                }
            };
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
            // Mutate in-memory so re-delivery via listenToEvents reflects the new status
            for (int i = 0; i < events.size(); i++) {
                Event e = events.get(i);
                if (e.getDocumentId().equals(documentId)) {
                    events.set(i, new Event(
                            e.getDocumentId(), e.getEventId(), e.getTitle(),
                            e.getCategory(), e.getLocation(), e.getDateTimeMillis(),
                            status != null ? status : EventStatus.ACTIVE,
                            e.getCapacityTotal(), e.getCapacityRemaining()
                    ));
                    break;
                }
            }
            callback.onSuccess();
        }
    }
    // =========================================================================
    // STATUS FILTER SPINNER
    // =========================================================================

    @Test
    public void statusFilter_active_showsOnlyFutureActiveEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        long past   = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Future Active",    "C", "L", future, EventStatus.ACTIVE,    100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Past Event",       "C", "L", past,   EventStatus.ACTIVE,    100, 50));
        eventRepository.events.add(new Event("d3", "e3", "Cancelled Event",  "C", "L", future, EventStatus.CANCELLED, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        Spinner statusSpinner = activity.findViewById(R.id.homeStatusFilterSpinner);
        statusSpinner.setSelection(1); // STATUS_ACTIVE
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(1, container.getChildCount());
        TextView title = container.getChildAt(0).findViewById(R.id.eventItemTitle);
        assertEquals("Future Active", title.getText().toString());
    }

    @Test
    public void statusFilter_cancelled_showsOnlyCancelledEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Active Event",    "C", "L", future, EventStatus.ACTIVE,    100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Cancelled Event", "C", "L", future, EventStatus.CANCELLED, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeStatusFilterSpinner);
        Spinner statusSpinner = activity.findViewById(R.id.homeStatusFilterSpinner);
        statusSpinner.setSelection(2); // STATUS_CANCELLED
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(1, container.getChildCount());
        TextView title = container.getChildAt(0).findViewById(R.id.eventItemTitle);
        assertEquals("Cancelled Event", title.getText().toString());
    }

    @Test
    public void statusFilter_past_showsOnlyPastEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        long past   = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Future Event", "C", "L", future, EventStatus.ACTIVE, 100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Past Event",   "C", "L", past,   EventStatus.ACTIVE, 100, 50));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        Spinner statusSpinner = activity.findViewById(R.id.homeStatusFilterSpinner);
        statusSpinner.setSelection(3); // STATUS_PAST
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(1, container.getChildCount());
        TextView title = container.getChildAt(0).findViewById(R.id.eventItemTitle);
        assertEquals("Past Event", title.getText().toString());
    }

    @Test
    public void statusFilter_soldOut_showsOnlySoldOutEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Normal Event",   "C", "L", future, EventStatus.ACTIVE, 100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Sold Out Event", "C", "L", future, EventStatus.ACTIVE, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        Spinner statusSpinner = activity.findViewById(R.id.homeStatusFilterSpinner);
        statusSpinner.setSelection(4); // STATUS_COMPLETE
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(1, container.getChildCount());
        TextView title = container.getChildAt(0).findViewById(R.id.eventItemTitle);
        assertEquals("Sold Out Event", title.getText().toString());
    }

    // =========================================================================
    // ADMIN FILTER PANEL — category / location / date range / clear
    // =========================================================================

    @Test
    public void filterPanel_toggle_expandsAndCollapses() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        com.google.android.material.chip.Chip chip = activity.findViewById(R.id.homeFilterToggleChip);
        LinearLayout panel = activity.findViewById(R.id.homeFilterPanel);

        assertEquals(View.GONE, panel.getVisibility());
        chip.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.VISIBLE, panel.getVisibility());
        chip.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.GONE, panel.getVisibility());
    }

    @Test
    public void categoryFilter_showsOnlyMatchingEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Jazz Night", "Music", "Hall A", future, EventStatus.ACTIVE, 100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Tech Talk",  "Tech",  "Hall B", future, EventStatus.ACTIVE, 100, 50));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        android.widget.EditText catInput = activity.findViewById(R.id.homeCategoryFilterInput);
        catInput.setText("Music");
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(1, container.getChildCount());
        TextView title = container.getChildAt(0).findViewById(R.id.eventItemTitle);
        assertEquals("Jazz Night", title.getText().toString());
    }

    @Test
    public void locationFilter_showsOnlyMatchingEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Event A", "C", "Montreal", future, EventStatus.ACTIVE, 100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Event B", "C", "Toronto",  future, EventStatus.ACTIVE, 100, 50));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        android.widget.EditText locInput = activity.findViewById(R.id.homeLocationFilterInput);
        locInput.setText("Toronto");
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(1, container.getChildCount());
        TextView title = container.getChildAt(0).findViewById(R.id.eventItemTitle);
        assertEquals("Event B", title.getText().toString());
    }

    @Test
    public void clearFilters_resetsAllAndShowsAllEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Jazz Night", "Music", "Hall A", future, EventStatus.ACTIVE, 100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Tech Talk",  "Tech",  "Hall B", future, EventStatus.ACTIVE, 100, 50));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        android.widget.EditText catInput = activity.findViewById(R.id.homeCategoryFilterInput);
        catInput.setText("Music");
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, ((LinearLayout) activity.findViewById(R.id.homeEventsContainer)).getChildCount());

        activity.findViewById(R.id.homeClearFiltersButton).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(2, ((LinearLayout) activity.findViewById(R.id.homeEventsContainer)).getChildCount());
        assertEquals("", catInput.getText().toString());
    }

    @Test
    public void dateFrom_afterDateTo_showsToastAndDoesNotChangeFilter() throws Exception {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        // Set TO date = Jan 1 2025
        Calendar toCal = Calendar.getInstance();
        toCal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        toCal.set(Calendar.MILLISECOND, 0);

        java.lang.reflect.Field toField =
                HomeActivity.class.getDeclaredField("filterDateToMillis");
        toField.setAccessible(true);
        toField.set(activity, toCal.getTimeInMillis());

        // Open FROM picker
        activity.findViewById(R.id.homeDateFromButton).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        DatePickerDialog dialog =
                (DatePickerDialog) ShadowDialog.getLatestDialog();

        // Pick Jan 1 2030 (clearly after 2025)
        dialog.getDatePicker().updateDate(2030, Calendar.JANUARY, 1);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(
                activity.getString(R.string.browse_filter_date_from_after_to),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void dateTo_beforeDateFrom_showsToastAndDoesNotChangeFilter() throws Exception {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        // Set FROM date = Jan 1 2030
        Calendar fromCal = Calendar.getInstance();
        fromCal.set(2030, Calendar.JANUARY, 1, 0, 0, 0);
        fromCal.set(Calendar.MILLISECOND, 0);

        java.lang.reflect.Field fromField =
                HomeActivity.class.getDeclaredField("filterDateFromMillis");
        fromField.setAccessible(true);
        fromField.set(activity, fromCal.getTimeInMillis());

        // Open TO picker
        activity.findViewById(R.id.homeDateToButton).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        DatePickerDialog dialog =
                (DatePickerDialog) ShadowDialog.getLatestDialog();

        // Pick Jan 1 2025 (clearly before 2030)
        dialog.getDatePicker().updateDate(2025, Calendar.JANUARY, 1);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(
                activity.getString(R.string.browse_filter_date_to_before_from),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void dateRangeFilter_showsOnlyEventsWithinRange() throws Exception {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);

        long now = System.currentTimeMillis();
        long inRange = now + 2 * 24 * 60 * 60 * 1000L;
        long outRange = now + 10 * 24 * 60 * 60 * 1000L;

        eventRepository.events.add(new Event("d1","e1","In","C","L", inRange, EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Out","C","L", outRange, EventStatus.ACTIVE,100,50));

        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        java.lang.reflect.Field from =
                HomeActivity.class.getDeclaredField("filterDateFromMillis");
        java.lang.reflect.Field to =
                HomeActivity.class.getDeclaredField("filterDateToMillis");
        from.setAccessible(true);
        to.setAccessible(true);

        from.set(activity, now);
        to.set(activity, now + 5 * 24 * 60 * 60 * 1000L);

        java.lang.reflect.Method apply =
                HomeActivity.class.getDeclaredMethod("applyFiltersAndRender");
        apply.setAccessible(true);
        apply.invoke(activity);
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        assertEquals(1, container.getChildCount());
    }



    // =========================================================================
    // RENDER — status badge priority on admin cards
    // =========================================================================

    @Test
    public void renderEvents_pastEvent_showsPastBadge() {
        long past = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Past Event", "C", "L", past, EventStatus.ACTIVE, 100, 50));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        View card = container.getChildAt(0);
        TextView badge = card.findViewById(R.id.eventItemStatusBadge);
        assertEquals(activity.getString(R.string.browse_event_past_badge), badge.getText().toString());
    }

    @Test
    public void renderEvents_soldOutEvent_showsSoldOutBadge() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Sold Out", "C", "L", future, EventStatus.ACTIVE, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        View card = container.getChildAt(0);
        TextView badge = card.findViewById(R.id.eventItemStatusBadge);
        assertEquals(activity.getString(R.string.browse_event_sold_out_badge), badge.getText().toString());
    }

    @Test
    public void renderEvents_cancelledEvent_showsCancelledBadge() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Cancelled", "C", "L", future, EventStatus.CANCELLED, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        LinearLayout container = activity.findViewById(R.id.homeEventsContainer);
        View card = container.getChildAt(0);
        TextView badge = card.findViewById(R.id.eventItemStatusBadge);
        assertEquals(activity.getString(R.string.home_status_cancelled), badge.getText().toString());
    }

    @Test
    public void renderEvents_emptyList_withFilters_showsNoMatchText() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        eventRepository.events.add(new Event("d1", "e1", "Jazz Night", "Music", "L", future, EventStatus.ACTIVE, 100, 50));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        android.widget.EditText catInput = activity.findViewById(R.id.homeCategoryFilterInput);
        catInput.setText("NonExistentCategory");
        shadowOf(Looper.getMainLooper()).idle();

        TextView emptyText = activity.findViewById(R.id.homeEventsEmptyText);
        assertEquals(View.VISIBLE, emptyText.getVisibility());
        assertEquals(activity.getString(R.string.browse_events_no_match), emptyText.getText().toString());
    }

    @Test
    public void renderEvents_emptyList_withoutFilters_showsEmptyText() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        TextView emptyText = activity.findViewById(R.id.homeEventsEmptyText);
        assertEquals(View.VISIBLE, emptyText.getVisibility());
        assertEquals(activity.getString(R.string.home_events_empty), emptyText.getText().toString());
    }

    // =========================================================================
    // ACTIVE CHIPS
    // =========================================================================

    @Test
    public void activeChip_category_dismissClearsFilter() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Jazz Night", "Music", "L", future, EventStatus.ACTIVE, 100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Tech Talk",  "Tech",  "L", future, EventStatus.ACTIVE, 100, 50));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        android.widget.EditText catInput = activity.findViewById(R.id.homeCategoryFilterInput);
        catInput.setText("Music");
        shadowOf(Looper.getMainLooper()).idle();

        // One chip should appear — dismiss it
        LinearLayout chipsContainer = activity.findViewById(R.id.homeActiveChipsContainer);
        assertTrue(chipsContainer.getChildCount() > 0);
        com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipsContainer.getChildAt(0);
        chip.getCloseIconContentDescription(); // just ensure it's a chip
        chip.performCloseIconClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("", catInput.getText().toString());
        assertEquals(2, ((LinearLayout) activity.findViewById(R.id.homeEventsContainer)).getChildCount());
    }

    @Test
    public void activeChip_statusFilter_dismissClearsFilter() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1", "e1", "Active",    "C", "L", future, EventStatus.ACTIVE,    100, 50));
        eventRepository.events.add(new Event("d2", "e2", "Cancelled", "C", "L", future, EventStatus.CANCELLED, 100, 0));
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        Spinner statusSpinner = activity.findViewById(R.id.homeStatusFilterSpinner);
        statusSpinner.setSelection(2); // STATUS_CANCELLED
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, ((LinearLayout) activity.findViewById(R.id.homeEventsContainer)).getChildCount());

        LinearLayout chipsContainer = activity.findViewById(R.id.homeActiveChipsContainer);
        com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipsContainer.getChildAt(0);
        chip.performCloseIconClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(2, ((LinearLayout) activity.findViewById(R.id.homeEventsContainer)).getChildCount());
    }

    @Test
    public void activeChip_dateRange_dismissClearsDatesAndButtons() throws Exception {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        long now = System.currentTimeMillis();

        java.lang.reflect.Field from =
                HomeActivity.class.getDeclaredField("filterDateFromMillis");
        java.lang.reflect.Field to =
                HomeActivity.class.getDeclaredField("filterDateToMillis");
        from.setAccessible(true);
        to.setAccessible(true);
        from.set(activity, now);
        to.set(activity, now);

        java.lang.reflect.Method apply =
                HomeActivity.class.getDeclaredMethod("applyFiltersAndRender");
        apply.setAccessible(true);
        apply.invoke(activity);
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout chips = activity.findViewById(R.id.homeActiveChipsContainer);
        assertTrue(chips.getChildCount() > 0);

        com.google.android.material.chip.Chip chip =
                (com.google.android.material.chip.Chip) chips.getChildAt(0);

        chip.performCloseIconClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(0L, from.get(activity));
        assertEquals(0L, to.get(activity));

        Button fromBtn = activity.findViewById(R.id.homeDateFromButton);
        Button toBtn = activity.findViewById(R.id.homeDateToButton);

        assertEquals(
                activity.getString(R.string.browse_filter_date_from_hint),
                fromBtn.getText().toString()
        );
        assertEquals(
                activity.getString(R.string.browse_filter_date_to_hint),
                toBtn.getText().toString()
        );
    }

    // =========================================================================
    // DIALOG VALIDATION
    // =========================================================================

    @Test
    public void addEventDialog_emptyTitle_showsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "", "2026-05-15 20:00", "Music", "Hall", "100", "80");
        clickPositive(dialog);

        android.widget.EditText titleInput = dialog.findViewById(R.id.dialogEventTitleInput);
        assertNotNull(titleInput); assertNotNull(titleInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_emptyCategory_showsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "", "Hall", "100", "80");
        clickPositive(dialog);

        android.widget.EditText catInput = dialog.findViewById(R.id.dialogEventCategoryInput);
        assertNotNull(catInput); assertNotNull(catInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_emptyLocation_showsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "", "100", "80");
        clickPositive(dialog);

        android.widget.EditText locInput = dialog.findViewById(R.id.dialogEventLocationInput);
        assertNotNull(locInput); assertNotNull(locInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_emptyCapacityTotal_showsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Hall", "", "");
        clickPositive(dialog);

        android.widget.EditText capInput = dialog.findViewById(R.id.dialogEventCapacityTotalInput);
        assertNotNull(capInput); assertNotNull(capInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_remainingExceedsTotal_showsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Hall", "50", "100");
        clickPositive(dialog);

        android.widget.EditText remInput = dialog.findViewById(R.id.dialogEventCapacityRemainingInput);
        assertNotNull(remInput); assertNotNull(remInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }

    @Test
    public void addEventDialog_negativeCapacity_showsError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        activity.findViewById(R.id.homeAddEventButton).performClick();
        AlertDialog dialog = latestDialog();
        fillEventDialog(dialog, "Concert", "2026-05-15 20:00", "Music", "Hall", "-5", "");
        clickPositive(dialog);

        android.widget.EditText capInput = dialog.findViewById(R.id.dialogEventCapacityTotalInput);
        assertNotNull(capInput); assertNotNull(capInput.getError());
        assertEquals(0, eventRepository.createCalls);
    }
    // =========================================================================
    // ON DESTROY
    // =========================================================================

    @Test
    public void onDestroy_removesEventListener() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        // Force loadEvents() to attach listener
        activity.onStart();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(eventRepository.listenCalls > 0);

        activity.onDestroy();

        assertTrue(eventRepository.listenerRemoved);
    }



    // =========================================================================
    // ONSTART — unauthenticated redirect
    // =========================================================================

    @Test
    public void onStart_whenNotSignedIn_redirectsToMain() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        HomeActivity activity = launchHome("admin@example.com", UserRole.ADMIN);

        // Sign out and call onStart manually
        authRepository.signedIn = false;
        activity.onStart();
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertNotNull(started.getComponent());
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

}