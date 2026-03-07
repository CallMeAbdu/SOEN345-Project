package com.soen345.project;

import android.app.AlertDialog;
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

import android.view.Menu;
import android.view.MenuItem;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.reservation.ReservationRepository;
import com.soen345.project.reservation.ReservationService;
import com.soen345.project.reservation.ReservationServiceProvider;

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
    private static final int SORT_DATE_ASC  = 0;
    private static final int SORT_DATE_DESC = 1;
    private static final int SORT_NAME_ASC  = 2;
    private static final int SORT_NAME_DESC = 3;
    private int currentSortOrder = SORT_DATE_ASC;

    // Filter state
    private long filterDateFromMillis = 0L;
    private long filterDateToMillis = 0L;
    private String filterCategory = "";
    private String filterLocation = "";
    private boolean hidePastEvents = true;    // default ON
    private boolean hideSoldOutEvents = false; // default OFF
    private boolean hideCancelledEvents = true; // default ON
    private com.soen345.project.event.EventListenerHandle eventsListenerHandle;

    // All loaded events (non-cancelled)
    private List<Event> allEvents = new ArrayList<>();

    // Services
    private AuthService authService;
    private EventService eventService;
    private ReservationService reservationService;


    // Views
    private MaterialToolbar browseToolbar;
    private com.google.android.material.chip.Chip browseFilterToggleChip;
    private LinearLayout browseFilterPanel;
    private LinearLayout browseActiveChipsContainer;
    private Spinner browseSortSpinner;
    private MaterialSwitch browseHidePastSwitch;
    private MaterialSwitch browseHideSoldOutSwitch;
    private MaterialSwitch browseHideCancelledSwitch;
    private EditText browseCategoryFilterInput;
    private EditText browseLocationFilterInput;
    private android.widget.Button browseDateFromButton;
    private android.widget.Button browseDateToButton;
    private android.widget.Button browseClearFiltersButton;
    private TextView browseEventsEmptyText;
    private LinearLayout browseEventsContainer;
    private BottomNavigationView browseBottomNav;

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
        reservationService = ReservationServiceProvider.getReservationService();


        bindViews();
        setupToolbar();
        setupSortSpinner();
        setupHidePastSwitch();
        setupHideSoldOutSwitch();
        setupHideCancelledSwitch();
        setupFilterListeners();
        setupBottomNav();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.browseRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventsListenerHandle != null) {
            eventsListenerHandle.remove();
            eventsListenerHandle = null;
        }
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
        browseToolbar = findViewById(R.id.browseToolbar);
        browseFilterToggleChip = findViewById(R.id.browseFilterToggleChip);
        browseFilterPanel = findViewById(R.id.browseFilterPanel);
        browseActiveChipsContainer = findViewById(R.id.browseActiveChipsContainer);
        browseSortSpinner = findViewById(R.id.browseSortSpinner);
        browseHidePastSwitch = findViewById(R.id.browseHidePastSwitch);
        browseHideSoldOutSwitch = findViewById(R.id.browseHideSoldOutSwitch);
        browseHideCancelledSwitch = findViewById(R.id.browseHideCancelledSwitch);
        browseCategoryFilterInput = findViewById(R.id.browseCategoryFilterInput);
        browseLocationFilterInput = findViewById(R.id.browseLocationFilterInput);
        browseDateFromButton = findViewById(R.id.browseDateFromButton);
        browseDateToButton = findViewById(R.id.browseDateToButton);
        browseClearFiltersButton = findViewById(R.id.browseClearFiltersButton);
        browseEventsEmptyText = findViewById(R.id.browseEventsEmptyText);
        browseEventsContainer = findViewById(R.id.browseEventsContainer);
        browseBottomNav = findViewById(R.id.browseBottomNav);
    }

    private void setupToolbar() {
        setSupportActionBar(browseToolbar);
        // Bold title
        browseToolbar.setTitleTextAppearance(this, R.style.BrowseToolbarTitleStyle);

        String email = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        if (isNullOrBlank(email)) email = authService.getSignedInEmail();
        if (isNullOrBlank(email)) email = getString(R.string.auth_unknown_user);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(getString(R.string.auth_signed_in_as, email));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browse_events, menu);
        MenuItem signOutItem = menu.findItem(R.id.action_sign_out);
        if (signOutItem != null) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;
            android.text.SpannableString s = new android.text.SpannableString(signOutItem.getTitle());
            s.setSpan(new android.text.style.ForegroundColorSpan(primaryColor), 0, s.length(), 0);
            signOutItem.setTitle(s);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void setupHidePastSwitch() {
        browseHidePastSwitch.setChecked(hidePastEvents);
        browseHidePastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hidePastEvents = isChecked;
            applyFiltersAndRender();
        });
    }

    private void setupHideSoldOutSwitch() {
        browseHideSoldOutSwitch.setChecked(hideSoldOutEvents);
        browseHideSoldOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideSoldOutEvents = isChecked;
            applyFiltersAndRender();
        });
    }

    private void setupHideCancelledSwitch() {
        browseHideCancelledSwitch.setChecked(hideCancelledEvents);
        browseHideCancelledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideCancelledEvents = isChecked;
            applyFiltersAndRender();
        });
    }

    private void setupFilterListeners() {
        // Toggle filter panel open/closed
        browseFilterToggleChip.setOnClickListener(v -> {
            boolean isVisible = browseFilterPanel.getVisibility() == View.VISIBLE;
            browseFilterPanel.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            browseFilterToggleChip.setChecked(!isVisible);
        });
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

    private void setupBottomNav() {
        browseBottomNav.setSelectedItemId(R.id.nav_browse_events);
        browseBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_browse_events) {
                return true;
            }
            if (id == R.id.nav_my_tickets) {
                String email = getIntent().getStringExtra(EXTRA_USER_EMAIL);
                String role = getIntent().getStringExtra(EXTRA_USER_ROLE);
                startActivity(MyTicketsActivity.newIntent(this, email, role));
                finish();
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // TASK-2.1.1 - Retrieve events from database
    // -------------------------------------------------------------------------

    private void loadEvents() {
        browseEventsEmptyText.setText(R.string.home_events_loading);
        browseEventsEmptyText.setVisibility(View.VISIBLE);
        browseEventsContainer.removeAllViews();

        if (eventsListenerHandle != null) eventsListenerHandle.remove();
        eventsListenerHandle = eventService.listenToEvents(new EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                allEvents = events != null ? events : new ArrayList<>();
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

    // -------------------------------------------------------------------------
    // Filters + sort
    // -------------------------------------------------------------------------

    private void applyFiltersAndRender() {
        List<Event> filtered = new ArrayList<>(allEvents);
        long nowMillis = System.currentTimeMillis();

        // Hide cancelled events filter (default ON)
        if (hideCancelledEvents) {
            List<Event> notCancelled = new ArrayList<>();
            for (Event e : filtered) if (!e.isCancelled()) notCancelled.add(e);
            filtered = notCancelled;
        }

        // Hide past events filter (default ON)
        if (hidePastEvents) {
            List<Event> upcoming = new ArrayList<>();
            for (Event e : filtered) {
                if (e.getDateTimeMillis() <= 0L || e.getDateTimeMillis() >= nowMillis) {
                    upcoming.add(e);
                }
            }
            filtered = upcoming;
        }

        // Hide sold out events filter
        if (hideSoldOutEvents) {
            List<Event> available = new ArrayList<>();
            for (Event e : filtered) {
                if (e.getCapacityRemaining() > 0) available.add(e);
            }
            filtered = available;
        }

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
        updateActiveChips();
    }

    /** Shows small dismissible chips in the collapsed bar summarising active filters */
    private void updateActiveChips() {
        browseActiveChipsContainer.removeAllViews();
        addSummaryChipIfNeeded(filterDateFromMillis > 0L || filterDateToMillis > 0L,
                buildDateRangeLabel(), () -> {
                    filterDateFromMillis = 0L;
                    filterDateToMillis = 0L;
                    browseDateFromButton.setText(R.string.browse_filter_date_from_hint);
                    browseDateToButton.setText(R.string.browse_filter_date_to_hint);
                    applyFiltersAndRender();
                });
        addSummaryChipIfNeeded(!filterCategory.isEmpty(), filterCategory, () -> {
            filterCategory = "";
            browseCategoryFilterInput.setText("");
            applyFiltersAndRender();
        });
        addSummaryChipIfNeeded(!filterLocation.isEmpty(), filterLocation, () -> {
            filterLocation = "";
            browseLocationFilterInput.setText("");
            applyFiltersAndRender();
        });
        addSummaryChipIfNeeded(!hidePastEvents,
                getString(R.string.browse_chip_showing_past), null);
        addSummaryChipIfNeeded(hideSoldOutEvents,
                getString(R.string.browse_chip_available_only), null);
        addSummaryChipIfNeeded(!hideCancelledEvents,
                getString(R.string.browse_chip_showing_cancelled), null);
    }

    private void addSummaryChipIfNeeded(boolean condition, String label, Runnable onClose) {
        if (!condition || label == null || label.isEmpty()) return;
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(label);
        chip.setTextSize(11f);
        if (onClose != null) {
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> onClose.run());
        }
        browseActiveChipsContainer.addView(chip);
    }

    private String buildDateRangeLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FILTER_DISPLAY_PATTERN, Locale.US);
        String from = filterDateFromMillis > 0L ? sdf.format(new Date(filterDateFromMillis)) : "…";
        String to = filterDateToMillis > 0L ? sdf.format(new Date(filterDateToMillis)) : "…";
        return from + " → " + to;
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
        // Note: hidePastEvents switch is intentionally not reset by clear filters
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
            TextView soldOutBadge = itemView.findViewById(R.id.browseEventSoldOutBadge);
            TextView cancelledBadge = itemView.findViewById(R.id.browseEventCancelledBadge);

            String title = isNullOrBlank(event.getTitle())
                    ? getString(R.string.home_event_untitled) : event.getTitle();
            String when = event.getDateTimeMillis() > 0L
                    ? formatDateTime(event.getDateTimeMillis()) : getString(R.string.home_event_no_time);
            String category = isNullOrBlank(event.getCategory())
                    ? getString(R.string.home_event_no_category) : event.getCategory();
            String location = isNullOrBlank(event.getLocation())
                    ? getString(R.string.home_event_no_location) : event.getLocation();

            boolean isCancelled = event.isCancelled();
            boolean isPast = !isCancelled && event.getDateTimeMillis() > 0L && event.getDateTimeMillis() < nowMillis;
            boolean isSoldOut = !isCancelled && event.getCapacityRemaining() == 0;

            // Capacity: "Tickets remaining: x / x"
            String capacity = getString(R.string.browse_event_capacity_remaining,
                    event.getCapacityRemaining(), event.getCapacityTotal());

            titleText.setText(title);
            detailsText.setText(
                    getString(R.string.home_event_time_label, when) + "\n"
                            + getString(R.string.home_event_category_label, category) + "\n"
                            + getString(R.string.home_event_location_label, location) + "\n"
                            + capacity
            );

            cancelledBadge.setVisibility(isCancelled ? View.VISIBLE : View.GONE);
            pastBadge.setVisibility(isPast ? View.VISIBLE : View.GONE);
            soldOutBadge.setVisibility(isSoldOut ? View.VISIBLE : View.GONE);

            // Alpha: cancelled > past > sold out > normal
            if (isCancelled || isPast) {
                itemView.setAlpha(0.5f);
            } else if (isSoldOut) {
                itemView.setAlpha(0.65f);
            } else {
                itemView.setAlpha(1.0f);
            }

            // Reserve button — hidden for cancelled/past/sold out
            android.widget.Button reserveButton = itemView.findViewById(R.id.browseEventReserveButton);
            if (isCancelled || isPast || isSoldOut) {
                reserveButton.setVisibility(View.GONE);
            } else {
                reserveButton.setVisibility(View.VISIBLE);
                reserveButton.setOnClickListener(v -> {
                    showReserveConfirmation(event);
                });
            }

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
    // Reservations
    // -------------------------------------------------------------------------
    private void showReserveConfirmation(Event e){
        String title = isNullOrBlank(e.getTitle())
                ? getString(R.string.home_event_untitled) : e.getTitle();

        new AlertDialog.Builder(this)
                .setTitle("Confirm Reservation")
                .setMessage("Are you sure you want to reserve " + title + "?")
                .setPositiveButton("Yes", (dialog, which) -> reserveEvent(e))
                .setNegativeButton("No", null)
                .show();
    }
    private void reserveEvent(Event e){
        String userEmail = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        if (isNullOrBlank(userEmail)) {
            userEmail = authService.getSignedInEmail();
        }
        if (isNullOrBlank(userEmail)) {
            Toast.makeText(this, "User email is null or blank", Toast.LENGTH_SHORT).show();
            return;
        }
        reservationService.reserveTicket(e, userEmail, new ReservationRepository.ReservationActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(BrowseEventsActivity.this, "Reservation successful", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(BrowseEventsActivity.this, "Reservation failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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
