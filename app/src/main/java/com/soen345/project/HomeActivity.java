package com.soen345.project;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.UserRole;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.event.EventStatus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";
    private static final String[] DATE_TIME_PATTERNS = new String[] {
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd'T'HH:mm",
            "d MMM yyyy HH:mm",
            "d MMM yyyy 'at' HH:mm:ss 'UTC'X",
            "dd MMM yyyy 'at' HH:mm:ss 'UTC'X"
    };

    public static final String EXTRA_USER_EMAIL = "extra_user_email";
    public static final String EXTRA_USER_ROLE = "extra_user_role";

    private AuthService authService;
    private EventService eventService;
    private UserRole signedInRole;

    private TextView homeUserEmailText;
    private TextView homeRoleText;
    private LinearLayout homeAdminSection;
    private Button homeAddEventButton;
    private TextView homeEventsEmptyText;
    private LinearLayout homeEventsContainer;

    public static Intent newIntent(Context context, String userEmail, UserRole role) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(EXTRA_USER_EMAIL, userEmail);
        if (role != null) {
            intent.putExtra(EXTRA_USER_ROLE, role.value());
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        authService = AuthServiceProvider.getAuthService();
        eventService = EventServiceProvider.getEventService();

        homeUserEmailText = findViewById(R.id.homeUserEmailText);
        homeRoleText = findViewById(R.id.homeRoleText);
        homeAdminSection = findViewById(R.id.homeAdminSection);
        homeAddEventButton = findViewById(R.id.homeAddEventButton);
        homeEventsEmptyText = findViewById(R.id.homeEventsEmptyText);
        homeEventsContainer = findViewById(R.id.homeEventsContainer);
        Button signOutButton = findViewById(R.id.homeSignOutButton);

        showSignedInEmail();
        signOutButton.setOnClickListener(v -> signOut());
        homeAddEventButton.setOnClickListener(v -> showEventDialog(null));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!authService.isSignedIn()) {
            goToAuth();
            return;
        }
        showSignedInEmail();
        if (signedInRole == UserRole.ADMIN) {
            loadEvents();
        }
    }

    private void showSignedInEmail() {
        String fromIntent = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        String fromSession = authService.getSignedInEmail();
        String chosenEmail = fromIntent;
        if (isNullOrBlank(chosenEmail)) {
            chosenEmail = fromSession;
        }
        if (isNullOrBlank(chosenEmail)) {
            chosenEmail = getString(R.string.auth_unknown_user);
        }
        homeUserEmailText.setText(getString(R.string.auth_signed_in_as, chosenEmail));

        UserRole role = UserRole.fromValue(getIntent().getStringExtra(EXTRA_USER_ROLE));
        if (role == null) {
            role = authService.getSignedInRole();
        }
        signedInRole = role;
        String roleLabel;
        if (role == UserRole.ADMIN) {
            roleLabel = getString(R.string.role_administrator);
        } else if (role == UserRole.CUSTOMER) {
            roleLabel = getString(R.string.role_customer);
        } else {
            roleLabel = getString(R.string.auth_unknown_user);
        }
        homeRoleText.setText(getString(R.string.home_role_label, roleLabel));
        setAdminSectionVisibility(role == UserRole.ADMIN);
    }

    private void setAdminSectionVisibility(boolean isAdmin) {
        homeAdminSection.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (!isAdmin) {
            homeEventsContainer.removeAllViews();
            homeEventsEmptyText.setVisibility(View.GONE);
        }
    }

    private void loadEvents() {
        homeEventsEmptyText.setText(R.string.home_events_loading);
        homeEventsEmptyText.setVisibility(View.VISIBLE);
        homeEventsContainer.removeAllViews();

        eventService.loadEvents(new EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                renderEvents(events);
            }

            @Override
            public void onError(String errorMessage) {
                homeEventsEmptyText.setText(R.string.home_events_load_failed);
                homeEventsEmptyText.setVisibility(View.VISIBLE);
                Toast.makeText(HomeActivity.this, buildErrorMessage(R.string.home_events_load_failed, errorMessage), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderEvents(List<Event> events) {
        homeEventsContainer.removeAllViews();
        if (events == null || events.isEmpty()) {
            homeEventsEmptyText.setText(R.string.home_events_empty);
            homeEventsEmptyText.setVisibility(View.VISIBLE);
            return;
        }

        homeEventsEmptyText.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Event event : events) {
            View itemView = inflater.inflate(R.layout.item_admin_event, homeEventsContainer, false);
            TextView titleText = itemView.findViewById(R.id.eventItemTitle);
            TextView detailsText = itemView.findViewById(R.id.eventItemDetails);
            TextView statusText = itemView.findViewById(R.id.eventItemStatus);
            Button editButton = itemView.findViewById(R.id.eventItemEditButton);
            Button statusButton = itemView.findViewById(R.id.eventItemStatusButton);

            String title = isNullOrBlank(event.getTitle()) ? getString(R.string.home_event_untitled) : event.getTitle();
            String when = event.getDateTimeMillis() > 0L
                    ? formatDateTimeMillis(event.getDateTimeMillis())
                    : getString(R.string.home_event_no_time);
            String category = isNullOrBlank(event.getCategory()) ? getString(R.string.home_event_no_category) : event.getCategory();
            String location = isNullOrBlank(event.getLocation()) ? getString(R.string.home_event_no_location) : event.getLocation();
            String statusValue = event.getStatus().value();

            titleText.setText(title);
            detailsText.setText(getString(R.string.home_event_time_label, when) + "\n"
                    + getString(R.string.home_event_category_label, category) + "\n"
                    + getString(R.string.home_event_location_label, location) + "\n"
                    + getString(R.string.home_event_capacity_label, event.getCapacityRemaining(), event.getCapacityTotal()));
            statusText.setText(getString(R.string.home_event_status_label, statusValue));

            editButton.setOnClickListener(v -> showEventDialog(event));
            boolean isCancelled = event.isCancelled();
            statusButton.setText(isCancelled ? R.string.home_activate_event_action : R.string.home_cancel_event_action);
            statusButton.setOnClickListener(v -> confirmAndToggleEventStatus(
                    event,
                    isCancelled ? EventStatus.ACTIVE : EventStatus.CANCELLED
            ));
            homeEventsContainer.addView(itemView);
        }
    }

    private void showEventDialog(@Nullable Event existingEvent) {
        if (signedInRole != UserRole.ADMIN) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_form, null, false);
        EditText titleInput = dialogView.findViewById(R.id.dialogEventTitleInput);
        EditText timeInput = dialogView.findViewById(R.id.dialogEventTimeInput);
        EditText categoryInput = dialogView.findViewById(R.id.dialogEventCategoryInput);
        EditText locationInput = dialogView.findViewById(R.id.dialogEventLocationInput);
        EditText capacityTotalInput = dialogView.findViewById(R.id.dialogEventCapacityTotalInput);
        EditText capacityRemainingInput = dialogView.findViewById(R.id.dialogEventCapacityRemainingInput);
        final long[] selectedDateTimeMillis = new long[] {0L};

        if (existingEvent != null) {
            titleInput.setText(existingEvent.getTitle());
            selectedDateTimeMillis[0] = existingEvent.getDateTimeMillis();
            if (selectedDateTimeMillis[0] > 0L) {
                timeInput.setText(formatDateTimeMillis(selectedDateTimeMillis[0]));
            }
            categoryInput.setText(existingEvent.getCategory());
            locationInput.setText(existingEvent.getLocation());
            capacityTotalInput.setText(String.valueOf(existingEvent.getCapacityTotal()));
            capacityRemainingInput.setText(String.valueOf(existingEvent.getCapacityRemaining()));
        }

        timeInput.setOnClickListener(v -> showDateTimePicker(
                selectedDateTimeMillis[0] > 0L ? selectedDateTimeMillis[0] : System.currentTimeMillis(),
                pickedMillis -> {
                    selectedDateTimeMillis[0] = pickedMillis;
                    timeInput.setText(formatDateTimeMillis(pickedMillis));
                    timeInput.setError(null);
                }
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existingEvent == null
                        ? R.string.home_add_event_action
                        : R.string.home_edit_event_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(existingEvent == null
                        ? R.string.home_add_event_action
                        : R.string.home_save_changes, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = trimToEmpty(titleInput.getText() == null ? null : titleInput.getText().toString());
            String time = trimToEmpty(timeInput.getText() == null ? null : timeInput.getText().toString());
            String category = trimToEmpty(categoryInput.getText() == null ? null : categoryInput.getText().toString());
            String location = trimToEmpty(locationInput.getText() == null ? null : locationInput.getText().toString());
            String capacityTotalText = trimToEmpty(capacityTotalInput.getText() == null
                    ? null
                    : capacityTotalInput.getText().toString());
            String capacityRemainingText = trimToEmpty(capacityRemainingInput.getText() == null
                    ? null
                    : capacityRemainingInput.getText().toString());

            if (capacityRemainingText.isEmpty()) {
                capacityRemainingText = capacityTotalText;
            }
            if (title.isEmpty()) {
                titleInput.setError(getString(R.string.home_event_title_required));
                return;
            }
            if (time.isEmpty()) {
                timeInput.setError(getString(R.string.home_event_time_required));
                return;
            }
            if (selectedDateTimeMillis[0] <= 0L) {
                selectedDateTimeMillis[0] = parseDateTimeInputMillis(time);
            }
            if (selectedDateTimeMillis[0] <= 0L) {
                timeInput.setError(getString(R.string.home_event_time_required));
                return;
            }
            if (category.isEmpty()) {
                categoryInput.setError(getString(R.string.home_event_category_required));
                return;
            }
            if (location.isEmpty()) {
                locationInput.setError(getString(R.string.home_event_location_required));
                return;
            }
            if (capacityTotalText.isEmpty()) {
                capacityTotalInput.setError(getString(R.string.home_event_capacity_total_required));
                return;
            }
            if (capacityRemainingText.isEmpty()) {
                capacityRemainingInput.setError(getString(R.string.home_event_capacity_remaining_required));
                return;
            }

            Integer capacityTotal = parseInteger(capacityTotalText);
            Integer capacityRemaining = parseInteger(capacityRemainingText);
            if (capacityTotal == null || capacityTotal <= 0) {
                capacityTotalInput.setError(getString(R.string.home_event_capacity_total_invalid));
                return;
            }
            if (capacityRemaining == null || capacityRemaining < 0) {
                capacityRemainingInput.setError(getString(R.string.home_event_capacity_remaining_invalid));
                return;
            }
            if (capacityRemaining > capacityTotal) {
                capacityRemainingInput.setError(getString(R.string.home_event_capacity_remaining_exceeds_total));
                return;
            }

            if (existingEvent == null) {
                createEvent(title, category, location, selectedDateTimeMillis[0], capacityTotal, capacityRemaining, dialog);
            } else {
                updateEvent(existingEvent, title, category, location, selectedDateTimeMillis[0], capacityTotal, capacityRemaining, dialog);
            }
        }));

        dialog.show();
    }

    private void createEvent(
            String title,
            String category,
            String location,
            long dateTimeMillis,
            int capacityTotal,
            int capacityRemaining,
            AlertDialog dialog
    ) {
        eventService.createEvent(title, category, location, dateTimeMillis, capacityTotal, capacityRemaining, new EventActionCallback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
                Toast.makeText(HomeActivity.this, R.string.home_event_added_success, Toast.LENGTH_SHORT).show();
                loadEvents();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(HomeActivity.this, buildErrorMessage(R.string.home_event_save_failed, errorMessage), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateEvent(
            Event existingEvent,
            String title,
            String category,
            String location,
            long dateTimeMillis,
            int capacityTotal,
            int capacityRemaining,
            AlertDialog dialog
    ) {
        eventService.updateEvent(
                existingEvent,
                title,
                category,
                location,
                dateTimeMillis,
                capacityTotal,
                capacityRemaining,
                new EventActionCallback() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                        Toast.makeText(HomeActivity.this, R.string.home_event_updated_success, Toast.LENGTH_SHORT).show();
                        loadEvents();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(HomeActivity.this, buildErrorMessage(R.string.home_event_save_failed, errorMessage), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void confirmAndToggleEventStatus(Event event, EventStatus targetStatus) {
        String title = isNullOrBlank(event.getTitle()) ? getString(R.string.home_event_untitled) : event.getTitle();
        boolean activating = targetStatus == EventStatus.ACTIVE;
        new AlertDialog.Builder(this)
                .setTitle(activating ? R.string.home_activate_event_action : R.string.home_cancel_event_title)
                .setMessage(activating
                        ? getString(R.string.home_activate_event_message, title)
                        : getString(R.string.home_cancel_event_message, title))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(activating
                        ? R.string.home_activate_event_action
                        : R.string.home_cancel_event_action, (dialog, which) -> eventService.updateEventStatus(
                        event,
                        targetStatus,
                        new EventActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(HomeActivity.this, activating
                                        ? R.string.home_event_activated_success
                                        : R.string.home_event_cancelled_success, Toast.LENGTH_SHORT).show();
                                loadEvents();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(HomeActivity.this, buildErrorMessage(R.string.home_event_cancel_failed, errorMessage), Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private long parseDateTimeInputMillis(String value) {
        String normalized = trimToEmpty(value);
        if (normalized.isEmpty()) {
            return 0L;
        }
        for (String pattern : DATE_TIME_PATTERNS) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(false);
            try {
                Date parsedDate = format.parse(normalized);
                if (parsedDate != null) {
                    return parsedDate.getTime();
                }
            } catch (Exception ignored) {
                // Try next pattern.
            }
        }
        return 0L;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private interface OnDateTimeSelectedListener {
        void onSelected(long epochMillis);
    }

    private void showDateTimePicker(long initialMillis, OnDateTimeSelectedListener listener) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(initialMillis);

        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth,
                            initial.get(Calendar.HOUR_OF_DAY),
                            initial.get(Calendar.MINUTE),
                            0);
                    TimePickerDialog timeDialog = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);
                                selected.set(Calendar.MILLISECOND, 0);
                                listener.onSelected(selected.getTimeInMillis());
                            },
                            initial.get(Calendar.HOUR_OF_DAY),
                            initial.get(Calendar.MINUTE),
                            true
                    );
                    timeDialog.show();
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );
        dateDialog.show();
    }

    private String formatDateTimeMillis(long epochMillis) {
        if (epochMillis <= 0L) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.US);
        return format.format(new Date(epochMillis));
    }

    private String buildErrorMessage(int fallbackResId, String rawErrorMessage) {
        String fallback = getString(fallbackResId);
        if (isNullOrBlank(rawErrorMessage)) {
            return fallback;
        }
        return fallback + " (" + rawErrorMessage.trim() + ")";
    }

    private void signOut() {
        authService.signOut();
        goToAuth();
    }

    private void goToAuth() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
