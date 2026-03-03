package com.soen345.project;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.event.EventStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BrowseEventsActivity extends AppCompatActivity {

    private static final String DATE_DISPLAY_PATTERN = "yyyy-MM-dd HH:mm";
    private static final String DATE_FILTER_DISPLAY_PATTERN = "yyyy-MM-dd";
    public static final String EXTRA_USER_EMAIL = "extra_user_email";
    public static final String EXTRA_USER_ROLE = "extra_user_role";

    // Sort options (index matches spinner position)
    private static final int SORT_DATE_ASC  = 0;  // Soonest first
    private static final int SORT_DATE_DESC = 1;  // Latest first
    private static final int SORT_NAME_ASC  = 2;  // A → Z
    private static final int SORT_NAME_DESC = 3;  // Z → A
    private int currentSortOrder = SORT_DATE_ASC;

    // Filter state
    private long filterDateFromMillis = 0L;
    private long filterDateToMillis = 0L;
    private String filterCategory = "";
    private String filterLocation = "";

    // All loaded events (non-cancelled)
    private List<Event> allEvents = new ArrayList<>();

    // Services
    private AuthService authService;
    private EventService eventService;

    // Views
    private TextView browseUserEmailText;
    private Button browseSignOutButton;
    private Spinner browseSortSpinner;
    private EditText browseCategoryFilterInput;
    private EditText browseLocationFilterInput;
    private Button browseDateFromButton;
    private Button browseDateToButton;
    private Button browseClearFiltersButton;
    private TextView browseEventsEmptyText;
    private LinearLayout browseEventsContainer;

    public static Intent newIntent(Context context, String userEmail, String role) {
        Intent intent = new Intent(context, BrowseEventsActivity.class);
        intent.putExtra(EXTRA_USER_EMAIL, userEmail);
        intent.putExtra(EXTRA_USER_ROLE, role);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_browse_events);

        authService = AuthServiceProvider.getAuthService();
        eventService = EventServiceProvider.getEventService();

        bindViews();
        setupUserInfo();
        setupSortSpinner();
        setupFilterListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.browseRoot), (v, insets) -> {
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
        loadEvents();
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private void bindViews() {
        browseUserEmailText = findViewById(R.id.browseUserEmailText);
        browseSignOutButton = findViewById(R.id.browseSignOutButton);
        browseSortSpinner = findViewById(R.id.browseSortSpinner);
        browseCategoryFilterInput = findViewById(R.id.browseCategoryFilterInput);
        browseLocationFilterInput = findViewById(R.id.browseLocationFilterInput);
        browseDateFromButton = findViewById(R.id.browseDateFromButton);
        browseDateToButton = findViewById(R.id.browseDateToButton);
        browseClearFiltersButton = findViewById(R.id.browseClearFiltersButton);
        browseEventsEmptyText = findViewById(R.id.browseEventsEmptyText);
        browseEventsContainer = findViewById(R.id.browseEventsContainer);
    }

    private void setupUserInfo() {
        String email = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        if (isNullOrBlank(email)) email = authService.getSignedInEmail();
        if (isNullOrBlank(email)) email = getString(R.string.auth_unknown_user);
        browseUserEmailText.setText(getString(R.string.auth_signed_in_as, email));
        browseSignOutButton.setOnClickListener(v -> signOut());
    }

    private void setupSortSpinner() {
        String[] sortOptions = {
                getString(R.string.browse_sort_date_asc),
                getString(R.string.browse_sort_date_desc),
                getString(R.string.browse_sort_name_asc),
                getString(R.string.browse_sort_name_desc)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        browseSortSpinner.setAdapter(adapter);
        browseSortSpinner.setSelection(SORT_DATE_ASC);
        browseSortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortOrder = position;
                applyFiltersAndRender();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFilterListeners() {
        browseCategoryFilterInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                filterCategory = s == null ? "" : s.toString().trim();
                applyFiltersAndRender();
            }
        });

        browseLocationFilterInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                filterLocation = s == null ? "" : s.toString().trim();
                applyFiltersAndRender();
            }
        });

        browseDateFromButton.setOnClickListener(v -> showDatePicker(true));
        browseDateToButton.setOnClickListener(v -> showDatePicker(false));
        browseClearFiltersButton.setOnClickListener(v -> clearFilters());
    }

    // -------------------------------------------------------------------------
    // TASK-2.1.1 - Retrieve events from database
    // -------------------------------------------------------------------------

    private void loadEvents() {
        browseEventsEmptyText.setText(R.string.home_events_loading);
        browseEventsEmptyText.setVisibility(View.VISIBLE);
        browseEventsContainer.removeAllViews();

        eventService.loadEvents(new EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                allEvents = filterCancelledEvents(events);
                applyFiltersAndRender();
            }

            @Override
            public void onError(String errorMessage) {
                browseEventsEmptyText.setText(R.string.home_events_load_failed);
                browseEventsEmptyText.setVisibility(View.VISIBLE);
                Toast.makeText(BrowseEventsActivity.this,
                        getString(R.string.home_events_load_failed) + " (" + errorMessage + ")",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // -------------------------------------------------------------------------
    // TASK-2.1.3 - Exclude cancelled events
    // -------------------------------------------------------------------------

    private List<Event> filterCancelledEvents(List<Event> events) {
        List<Event> result = new ArrayList<>();
        if (events == null) return result;
        for (Event e : events) {
            if (e.getStatus() != EventStatus.CANCELLED) {
                result.add(e);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Filters + sort | TASK-2.2.4 - Dynamic UI update
    // -------------------------------------------------------------------------

    private void applyFiltersAndRender() {
        List<Event> filtered = new ArrayList<>(allEvents);

        // Date range filter
        if (filterDateFromMillis > 0L || filterDateToMillis > 0L) {
            long rangeStart = 0L;
            long rangeEnd = Long.MAX_VALUE;

            if (filterDateFromMillis > 0L) {
                Calendar from = Calendar.getInstance();
                from.setTimeInMillis(filterDateFromMillis);
                from.set(Calendar.HOUR_OF_DAY, 0);
                from.set(Calendar.MINUTE, 0);
                from.set(Calendar.SECOND, 0);
                from.set(Calendar.MILLISECOND, 0);
                rangeStart = from.getTimeInMillis();
            }
            if (filterDateToMillis > 0L) {
                Calendar to = Calendar.getInstance();
                to.setTimeInMillis(filterDateToMillis);
                to.set(Calendar.HOUR_OF_DAY, 23);
                to.set(Calendar.MINUTE, 59);
                to.set(Calendar.SECOND, 59);
                to.set(Calendar.MILLISECOND, 999);
                rangeEnd = to.getTimeInMillis();
            }

            final long rs = rangeStart;
            final long re = rangeEnd;
            List<Event> dateFiltered = new ArrayList<>();
            for (Event e : filtered) {
                long t = e.getDateTimeMillis();
                if (t >= rs && t <= re) dateFiltered.add(e);
            }
            filtered = dateFiltered;
        }

        // Category filter
        if (!filterCategory.isEmpty()) {
            String lower = filterCategory.toLowerCase(Locale.US);
            List<Event> catFiltered = new ArrayList<>();
            for (Event e : filtered) {
                if (e.getCategory().toLowerCase(Locale.US).contains(lower)) catFiltered.add(e);
            }
            filtered = catFiltered;
        }

        // Location filter
        if (!filterLocation.isEmpty()) {
            String lower = filterLocation.toLowerCase(Locale.US);
            List<Event> locFiltered = new ArrayList<>();
            for (Event e : filtered) {
                if (e.getLocation().toLowerCase(Locale.US).contains(lower)) locFiltered.add(e);
            }
            filtered = locFiltered;
        }

        // Sort
        switch (currentSortOrder) {
            case SORT_DATE_ASC:
                Collections.sort(filtered, (a, b) -> Long.compare(a.getDateTimeMillis(), b.getDateTimeMillis()));
                break;
            case SORT_DATE_DESC:
                Collections.sort(filtered, (a, b) -> Long.compare(b.getDateTimeMillis(), a.getDateTimeMillis()));
                break;
            case SORT_NAME_ASC:
                Collections.sort(filtered, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
            case SORT_NAME_DESC:
                Collections.sort(filtered, (a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;
        }

        renderEvents(filtered);
    }

    private void clearFilters() {
        filterDateFromMillis = 0L;
        filterDateToMillis = 0L;
        filterCategory = "";
        filterLocation = "";
        browseDateFromButton.setText(R.string.browse_filter_date_from_hint);
        browseDateToButton.setText(R.string.browse_filter_date_to_hint);
        browseCategoryFilterInput.setText("");
        browseLocationFilterInput.setText("");
        applyFiltersAndRender();
    }

    // -------------------------------------------------------------------------
    // TASK-2.1.2 - Display event list UI
    // -------------------------------------------------------------------------

    private void renderEvents(List<Event> events) {
        browseEventsContainer.removeAllViews();

        if (events == null || events.isEmpty()) {
            boolean filtersActive = filterDateFromMillis > 0L || filterDateToMillis > 0L
                    || !filterCategory.isEmpty() || !filterLocation.isEmpty();
            browseEventsEmptyText.setText(
                    filtersActive ? R.string.browse_events_no_match : R.string.home_events_empty);
            browseEventsEmptyText.setVisibility(View.VISIBLE);
            return;
        }

        browseEventsEmptyText.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        long nowMillis = System.currentTimeMillis();

        for (Event event : events) {
            View itemView = inflater.inflate(R.layout.item_browse_event, browseEventsContainer, false);
            TextView titleText = itemView.findViewById(R.id.browseEventItemTitle);
            TextView detailsText = itemView.findViewById(R.id.browseEventItemDetails);
            TextView pastBadge = itemView.findViewById(R.id.browseEventPastBadge);

            String title = isNullOrBlank(event.getTitle())
                    ? getString(R.string.home_event_untitled) : event.getTitle();
            String when = event.getDateTimeMillis() > 0L
                    ? formatDateTime(event.getDateTimeMillis()) : getString(R.string.home_event_no_time);
            String category = isNullOrBlank(event.getCategory())
                    ? getString(R.string.home_event_no_category) : event.getCategory();
            String location = isNullOrBlank(event.getLocation())
                    ? getString(R.string.home_event_no_location) : event.getLocation();

            boolean isPast = event.getDateTimeMillis() > 0L && event.getDateTimeMillis() < nowMillis;

            titleText.setText(title);
            detailsText.setText(
                    getString(R.string.home_event_time_label, when) + "\n"
                            + getString(R.string.home_event_category_label, category) + "\n"
                            + getString(R.string.home_event_location_label, location) + "\n"
                            + getString(R.string.home_event_capacity_label,
                            event.getCapacityRemaining(), event.getCapacityTotal())
            );

            // Past event indicator
            pastBadge.setVisibility(isPast ? View.VISIBLE : View.GONE);

            // Dim the whole card for past events
            itemView.setAlpha(isPast ? 0.5f : 1.0f);

            browseEventsContainer.addView(itemView);
        }
    }

    // -------------------------------------------------------------------------
    // Date picker
    // -------------------------------------------------------------------------

    private void showDatePicker(boolean isFrom) {
        Calendar initial = Calendar.getInstance();
        long current = isFrom ? filterDateFromMillis : filterDateToMillis;
        if (current > 0L) initial.setTimeInMillis(current);

        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    long pickedMillis = selected.getTimeInMillis();

                    if (isFrom && filterDateToMillis > 0L && pickedMillis > filterDateToMillis) {
                        Toast.makeText(this, getString(R.string.browse_filter_date_from_after_to),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!isFrom && filterDateFromMillis > 0L && pickedMillis < filterDateFromMillis) {
                        Toast.makeText(this, getString(R.string.browse_filter_date_to_before_from),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String label = new SimpleDateFormat(DATE_FILTER_DISPLAY_PATTERN, Locale.US)
                            .format(new Date(pickedMillis));
                    if (isFrom) {
                        filterDateFromMillis = pickedMillis;
                        browseDateFromButton.setText(label);
                    } else {
                        filterDateToMillis = pickedMillis;
                        browseDateToButton.setText(label);
                    }
                    applyFiltersAndRender();
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private String formatDateTime(long epochMillis) {
        return new SimpleDateFormat(DATE_DISPLAY_PATTERN, Locale.US).format(new Date(epochMillis));
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}