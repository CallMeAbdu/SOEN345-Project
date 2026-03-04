package com.soen345.project;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";
    private static final String[] DATE_TIME_PATTERNS = {
            "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy-MM-dd'T'HH:mm",
            "d MMM yyyy HH:mm", "d MMM yyyy 'at' HH:mm:ss 'UTC'X",
            "dd MMM yyyy 'at' HH:mm:ss 'UTC'X"
    };

    public static final String EXTRA_USER_EMAIL = "extra_user_email";
    public static final String EXTRA_USER_ROLE = "extra_user_role";

    // Sort constants
    private static final int SORT_DATE_ASC  = 0;
    private static final int SORT_DATE_DESC = 1;
    private static final int SORT_NAME_ASC  = 2;
    private static final int SORT_NAME_DESC = 3;

    // Status filter constants
    private static final int STATUS_ALL       = 0;
    private static final int STATUS_ACTIVE    = 1;
    private static final int STATUS_CANCELLED = 2;
    private static final int STATUS_PAST      = 3;
    private static final int STATUS_COMPLETE  = 4;

    // Filter state
    private int currentSort = SORT_DATE_ASC;
    private int statusFilter = STATUS_ALL;
    private String filterCategory = "";
    private String filterLocation = "";
    private long filterDateFromMillis = 0L;
    private long filterDateToMillis = 0L;

    private List<Event> allEvents = new ArrayList<>();

    private AuthService authService;
    private EventService eventService;
    private UserRole signedInRole;

    // Views
    private MaterialToolbar homeToolbar;
    private Chip homeFilterToggleChip;
    private LinearLayout homeFilterPanel;
    private LinearLayout homeActiveChipsContainer;
    private android.widget.Button homeDateFromButton;
    private android.widget.Button homeDateToButton;
    private Spinner homeSortSpinner;
    private Spinner homeStatusFilterSpinner;
    private EditText homeCategoryFilterInput;
    private EditText homeLocationFilterInput;
    private LinearLayout homeEventsContainer;
    private TextView homeEventsEmptyText;
    private ExtendedFloatingActionButton homeAddEventButton;

    public static Intent newIntent(Context context, String userEmail, UserRole role) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(EXTRA_USER_EMAIL, userEmail);
        if (role != null) intent.putExtra(EXTRA_USER_ROLE, role.value());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        authService = AuthServiceProvider.getAuthService();
        eventService = EventServiceProvider.getEventService();

        bindViews();
        setupToolbar();
        setupFilterPanel();
        setupDateFilters();
        setupSortSpinner();
        setupStatusFilterSpinner();
        setupTextFilters();

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
        if (!authService.isSignedIn()) { goToAuth(); return; }
        UserRole role = UserRole.fromValue(getIntent().getStringExtra(EXTRA_USER_ROLE));
        if (role == null) role = authService.getSignedInRole();
        signedInRole = role;
        if (signedInRole == UserRole.ADMIN) {
            loadEvents();
        } else {
            startActivity(BrowseEventsActivity.newIntent(this,
                    getIntent().getStringExtra(EXTRA_USER_EMAIL),
                    getIntent().getStringExtra(EXTRA_USER_ROLE)));
            finish();
        }
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private void bindViews() {
        homeToolbar = findViewById(R.id.homeToolbar);
        homeFilterToggleChip = findViewById(R.id.homeFilterToggleChip);
        homeFilterPanel = findViewById(R.id.homeFilterPanel);
        homeActiveChipsContainer = findViewById(R.id.homeActiveChipsContainer);
        homeDateFromButton = findViewById(R.id.homeDateFromButton);
        homeDateToButton = findViewById(R.id.homeDateToButton);
        homeSortSpinner = findViewById(R.id.homeSortSpinner);
        homeStatusFilterSpinner = findViewById(R.id.homeStatusFilterSpinner);
        homeCategoryFilterInput = findViewById(R.id.homeCategoryFilterInput);
        homeLocationFilterInput = findViewById(R.id.homeLocationFilterInput);
        homeEventsContainer = findViewById(R.id.homeEventsContainer);
        homeEventsEmptyText = findViewById(R.id.homeEventsEmptyText);
        homeAddEventButton = findViewById(R.id.homeAddEventButton);
        findViewById(R.id.homeClearFiltersButton).setOnClickListener(v -> clearFilters());
    }

    private void setupToolbar() {
        setSupportActionBar(homeToolbar);
        homeToolbar.setTitleTextAppearance(this, R.style.BrowseToolbarTitleStyle);
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
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
            android.text.SpannableString s = new android.text.SpannableString(signOutItem.getTitle());
            s.setSpan(new android.text.style.ForegroundColorSpan(tv.data), 0, s.length(), 0);
            signOutItem.setTitle(s);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) { signOut(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void setupFilterPanel() {
        homeFilterToggleChip.setOnClickListener(v -> {
            boolean visible = homeFilterPanel.getVisibility() == View.VISIBLE;
            homeFilterPanel.setVisibility(visible ? View.GONE : View.VISIBLE);
            homeFilterToggleChip.setChecked(!visible);
        });
    }

    private void setupDateFilters() {
        homeDateFromButton.setOnClickListener(v ->
                showDatePicker(filterDateFromMillis > 0L ? filterDateFromMillis : System.currentTimeMillis(),
                        pickedMillis -> {
                            if (filterDateToMillis > 0L && pickedMillis > filterDateToMillis) {
                                Toast.makeText(this, R.string.browse_filter_date_from_after_to, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            filterDateFromMillis = setStartOfDay(pickedMillis);
                            homeDateFromButton.setText(formatDateOnly(filterDateFromMillis));
                            applyFiltersAndRender();
                        }));

        homeDateToButton.setOnClickListener(v ->
                showDatePicker(filterDateToMillis > 0L ? filterDateToMillis : System.currentTimeMillis(),
                        pickedMillis -> {
                            if (filterDateFromMillis > 0L && pickedMillis < filterDateFromMillis) {
                                Toast.makeText(this, R.string.browse_filter_date_to_before_from, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            filterDateToMillis = setEndOfDay(pickedMillis);
                            homeDateToButton.setText(formatDateOnly(filterDateToMillis));
                            applyFiltersAndRender();
                        }));
    }

    /** Date-only picker — used for filters. No time step. */
    private void showDatePicker(long initialMillis, OnDateTimeSelectedListener listener) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(initialMillis);
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, day, 0, 0, 0);
                    sel.set(Calendar.MILLISECOND, 0);
                    listener.onSelected(sel.getTimeInMillis());
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private long setStartOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long setEndOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private String formatDateOnly(long millis) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(millis));
    }

    private void setupSortSpinner() {
        String[] opts = {
                getString(R.string.browse_sort_date_asc),
                getString(R.string.browse_sort_date_desc),
                getString(R.string.browse_sort_name_asc),
                getString(R.string.browse_sort_name_desc)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        homeSortSpinner.setAdapter(adapter);
        homeSortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentSort = pos; applyFiltersAndRender();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupStatusFilterSpinner() {
        String[] opts = {
                getString(R.string.home_filter_status_all),
                getString(R.string.home_filter_status_active),
                getString(R.string.home_filter_status_cancelled),
                getString(R.string.home_filter_status_past),
                getString(R.string.home_filter_status_sold_out)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        homeStatusFilterSpinner.setAdapter(adapter);
        homeStatusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                statusFilter = pos; applyFiltersAndRender();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupTextFilters() {
        homeCategoryFilterInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                filterCategory = s == null ? "" : s.toString().trim();
                applyFiltersAndRender();
            }
        });
        homeLocationFilterInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                filterLocation = s == null ? "" : s.toString().trim();
                applyFiltersAndRender();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Load & filter
    // -------------------------------------------------------------------------

    private void loadEvents() {
        homeEventsEmptyText.setText(R.string.home_events_loading);
        homeEventsEmptyText.setVisibility(View.VISIBLE);
        homeEventsContainer.removeAllViews();

        eventService.loadEvents(new EventListCallback() {
            @Override public void onSuccess(List<Event> events) {
                allEvents = events != null ? events : new ArrayList<>();
                applyFiltersAndRender();
            }
            @Override public void onError(String errorMessage) {
                homeEventsEmptyText.setText(R.string.home_events_load_failed);
                homeEventsEmptyText.setVisibility(View.VISIBLE);
                Toast.makeText(HomeActivity.this,
                        buildErrorMessage(R.string.home_events_load_failed, errorMessage),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFiltersAndRender() {
        List<Event> filtered = new ArrayList<>(allEvents);

        // Status filter
        long nowMillis = System.currentTimeMillis();
        if (statusFilter == STATUS_ACTIVE) {
            List<Event> r = new ArrayList<>();
            for (Event e : filtered)
                if (!e.isCancelled() && e.getDateTimeMillis() >= nowMillis) r.add(e);
            filtered = r;
        } else if (statusFilter == STATUS_CANCELLED) {
            List<Event> r = new ArrayList<>();
            for (Event e : filtered) if (e.isCancelled()) r.add(e);
            filtered = r;
        } else if (statusFilter == STATUS_PAST) {
            List<Event> r = new ArrayList<>();
            for (Event e : filtered)
                if (e.getDateTimeMillis() > 0L && e.getDateTimeMillis() < nowMillis) r.add(e);
            filtered = r;
        } else if (statusFilter == STATUS_COMPLETE) {
            List<Event> r = new ArrayList<>();
            for (Event e : filtered) if (e.getCapacityRemaining() == 0) r.add(e);
            filtered = r;
        }

        // Date range filter
        if (filterDateFromMillis > 0L) {
            long from = filterDateFromMillis;
            List<Event> r = new ArrayList<>();
            for (Event e : filtered) if (e.getDateTimeMillis() >= from) r.add(e);
            filtered = r;
        }
        if (filterDateToMillis > 0L) {
            long to = filterDateToMillis;
            List<Event> r = new ArrayList<>();
            for (Event e : filtered) if (e.getDateTimeMillis() <= to) r.add(e);
            filtered = r;
        }

        // Category filter
        if (!filterCategory.isEmpty()) {
            String lower = filterCategory.toLowerCase(Locale.US);
            List<Event> r = new ArrayList<>();
            for (Event e : filtered)
                if (e.getCategory().toLowerCase(Locale.US).contains(lower)) r.add(e);
            filtered = r;
        }

        // Location filter
        if (!filterLocation.isEmpty()) {
            String lower = filterLocation.toLowerCase(Locale.US);
            List<Event> r = new ArrayList<>();
            for (Event e : filtered)
                if (e.getLocation().toLowerCase(Locale.US).contains(lower)) r.add(e);
            filtered = r;
        }

        // Sort
        switch (currentSort) {
            case SORT_DATE_ASC:
                Collections.sort(filtered, (a, b) -> Long.compare(a.getDateTimeMillis(), b.getDateTimeMillis())); break;
            case SORT_DATE_DESC:
                Collections.sort(filtered, (a, b) -> Long.compare(b.getDateTimeMillis(), a.getDateTimeMillis())); break;
            case SORT_NAME_ASC:
                Collections.sort(filtered, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle())); break;
            case SORT_NAME_DESC:
                Collections.sort(filtered, (a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle())); break;
        }

        renderEvents(filtered);
        updateActiveChips();
    }

    private void clearFilters() {
        statusFilter = STATUS_ALL;
        filterCategory = "";
        filterLocation = "";
        filterDateFromMillis = 0L;
        filterDateToMillis = 0L;
        homeStatusFilterSpinner.setSelection(STATUS_ALL);
        homeCategoryFilterInput.setText("");
        homeLocationFilterInput.setText("");
        homeDateFromButton.setText(R.string.browse_filter_date_from_hint);
        homeDateToButton.setText(R.string.browse_filter_date_to_hint);
        applyFiltersAndRender();
    }

    private void updateActiveChips() {
        homeActiveChipsContainer.removeAllViews();

        // Date range chip
        if (filterDateFromMillis > 0L || filterDateToMillis > 0L) {
            String from = filterDateFromMillis > 0L ? formatDateOnly(filterDateFromMillis) : "…";
            String to   = filterDateToMillis   > 0L ? formatDateOnly(filterDateToMillis)   : "…";
            addSummaryChip(from + " → " + to, () -> {
                filterDateFromMillis = 0L;
                filterDateToMillis = 0L;
                homeDateFromButton.setText(R.string.browse_filter_date_from_hint);
                homeDateToButton.setText(R.string.browse_filter_date_to_hint);
                applyFiltersAndRender();
            });
        }
        if (statusFilter != STATUS_ALL) {
            int[] labelRes = {0,
                    R.string.home_filter_status_active,
                    R.string.home_filter_status_cancelled,
                    R.string.home_filter_status_past,
                    R.string.home_filter_status_sold_out
            };
            String label = getString(labelRes[statusFilter]);
            addSummaryChip(label, () -> {
                statusFilter = STATUS_ALL;
                homeStatusFilterSpinner.setSelection(STATUS_ALL);
                applyFiltersAndRender();
            });
        }
        if (!filterCategory.isEmpty()) {
            addSummaryChip(filterCategory, () -> {
                filterCategory = "";
                homeCategoryFilterInput.setText("");
                applyFiltersAndRender();
            });
        }
        if (!filterLocation.isEmpty()) {
            addSummaryChip(filterLocation, () -> {
                filterLocation = "";
                homeLocationFilterInput.setText("");
                applyFiltersAndRender();
            });
        }
    }

    private void addSummaryChip(String label, Runnable onClose) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setTextSize(11f);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> onClose.run());
        homeActiveChipsContainer.addView(chip);
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    private void renderEvents(List<Event> events) {
        homeEventsContainer.removeAllViews();

        if (events == null || events.isEmpty()) {
            boolean filtersActive = statusFilter != STATUS_ALL
                    || !filterCategory.isEmpty() || !filterLocation.isEmpty()
                    || filterDateFromMillis > 0L || filterDateToMillis > 0L;
            homeEventsEmptyText.setText(filtersActive
                    ? R.string.browse_events_no_match : R.string.home_events_empty);
            homeEventsEmptyText.setVisibility(View.VISIBLE);
            return;
        }

        homeEventsEmptyText.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            View itemView = inflater.inflate(R.layout.item_admin_event, homeEventsContainer, false);

            TextView titleText = itemView.findViewById(R.id.eventItemTitle);
            TextView detailsText = itemView.findViewById(R.id.eventItemDetails);
            TextView statusBadge = itemView.findViewById(R.id.eventItemStatusBadge);
            ImageButton editButton = itemView.findViewById(R.id.eventItemEditButton);
            android.widget.Button statusButton = itemView.findViewById(R.id.eventItemStatusButton);

            String title = isNullOrBlank(event.getTitle())
                    ? getString(R.string.home_event_untitled) : event.getTitle();
            String when = event.getDateTimeMillis() > 0L
                    ? formatDateTimeMillis(event.getDateTimeMillis())
                    : getString(R.string.home_event_no_time);
            String category = isNullOrBlank(event.getCategory())
                    ? getString(R.string.home_event_no_category) : event.getCategory();
            String location = isNullOrBlank(event.getLocation())
                    ? getString(R.string.home_event_no_location) : event.getLocation();

            boolean isCancelled = event.isCancelled();
            boolean isPast = event.getDateTimeMillis() > 0L
                    && event.getDateTimeMillis() < System.currentTimeMillis();
            boolean isSoldOut = event.getCapacityRemaining() == 0;

            titleText.setText(title);
            detailsText.setText(
                    getString(R.string.home_event_time_label, when) + "\n"
                            + getString(R.string.home_event_category_label, category) + "\n"
                            + getString(R.string.home_event_location_label, location) + "\n"
                            + getString(R.string.home_event_capacity_label,
                            event.getCapacityRemaining(), event.getCapacityTotal())
            );

            // Status badge — priority: Cancelled > Past > Sold Out > Active
            statusBadge.setVisibility(View.VISIBLE);
            if (isCancelled) {
                statusBadge.setText(R.string.home_status_cancelled);
                statusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
            } else if (isPast) {
                statusBadge.setText(R.string.browse_event_past_badge);
                statusBadge.setBackgroundResource(R.drawable.bg_past_badge);
            } else if (isSoldOut) {
                statusBadge.setText(R.string.browse_event_sold_out_badge);
                statusBadge.setBackgroundResource(R.drawable.bg_sold_out_badge);
            } else {
                statusBadge.setText(R.string.home_status_active);
                statusBadge.setBackgroundResource(R.drawable.bg_status_active);
            }

            // Dim past/cancelled cards
            itemView.setAlpha((isCancelled || isPast) ? 0.55f : 1.0f);

            editButton.setOnClickListener(v -> showEventDialog(event));

            statusButton.setText(isCancelled
                    ? R.string.home_activate_event_action
                    : R.string.home_cancel_event_action);
            statusButton.setOnClickListener(v -> confirmAndToggleEventStatus(
                    event, isCancelled ? EventStatus.ACTIVE : EventStatus.CANCELLED));

            homeEventsContainer.addView(itemView);
        }
    }

    // -------------------------------------------------------------------------
    // Event dialog (unchanged logic, just hoisted)
    // -------------------------------------------------------------------------

    private void showEventDialog(@Nullable Event existingEvent) {
        if (signedInRole != UserRole.ADMIN) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_form, null, false);
        EditText titleInput = dialogView.findViewById(R.id.dialogEventTitleInput);
        EditText timeInput = dialogView.findViewById(R.id.dialogEventTimeInput);
        EditText categoryInput = dialogView.findViewById(R.id.dialogEventCategoryInput);
        EditText locationInput = dialogView.findViewById(R.id.dialogEventLocationInput);
        EditText capacityTotalInput = dialogView.findViewById(R.id.dialogEventCapacityTotalInput);
        EditText capacityRemainingInput = dialogView.findViewById(R.id.dialogEventCapacityRemainingInput);
        final long[] selectedDateTimeMillis = {0L};

        if (existingEvent != null) {
            titleInput.setText(existingEvent.getTitle());
            selectedDateTimeMillis[0] = existingEvent.getDateTimeMillis();
            if (selectedDateTimeMillis[0] > 0L)
                timeInput.setText(formatDateTimeMillis(selectedDateTimeMillis[0]));
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
                .setTitle(existingEvent == null ? R.string.home_add_event_action : R.string.home_edit_event_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(existingEvent == null ? R.string.home_add_event_action : R.string.home_save_changes, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = trimToEmpty(titleInput.getText() == null ? null : titleInput.getText().toString());
            String time = trimToEmpty(timeInput.getText() == null ? null : timeInput.getText().toString());
            String category = trimToEmpty(categoryInput.getText() == null ? null : categoryInput.getText().toString());
            String location = trimToEmpty(locationInput.getText() == null ? null : locationInput.getText().toString());
            String capTotalText = trimToEmpty(capacityTotalInput.getText() == null ? null : capacityTotalInput.getText().toString());
            String capRemainingText = trimToEmpty(capacityRemainingInput.getText() == null ? null : capacityRemainingInput.getText().toString());
            if (capRemainingText.isEmpty()) capRemainingText = capTotalText;

            if (title.isEmpty()) { titleInput.setError(getString(R.string.home_event_title_required)); return; }
            if (time.isEmpty()) { timeInput.setError(getString(R.string.home_event_time_required)); return; }
            if (selectedDateTimeMillis[0] <= 0L) selectedDateTimeMillis[0] = parseDateTimeInputMillis(time);
            if (selectedDateTimeMillis[0] <= 0L) { timeInput.setError(getString(R.string.home_event_time_required)); return; }
            if (category.isEmpty()) { categoryInput.setError(getString(R.string.home_event_category_required)); return; }
            if (location.isEmpty()) { locationInput.setError(getString(R.string.home_event_location_required)); return; }
            if (capTotalText.isEmpty()) { capacityTotalInput.setError(getString(R.string.home_event_capacity_total_required)); return; }

            Integer capacityTotal = parseInteger(capTotalText);
            Integer capacityRemaining = parseInteger(capRemainingText);
            if (capacityTotal == null || capacityTotal <= 0) { capacityTotalInput.setError(getString(R.string.home_event_capacity_total_invalid)); return; }
            if (capacityRemaining == null || capacityRemaining < 0) { capacityRemainingInput.setError(getString(R.string.home_event_capacity_remaining_invalid)); return; }
            if (capacityRemaining > capacityTotal) { capacityRemainingInput.setError(getString(R.string.home_event_capacity_remaining_exceeds_total)); return; }

            if (existingEvent == null) {
                createEvent(title, category, location, selectedDateTimeMillis[0], capacityTotal, capacityRemaining, dialog);
            } else {
                updateEvent(existingEvent, title, category, location, selectedDateTimeMillis[0], capacityTotal, capacityRemaining, dialog);
            }
        }));
        dialog.show();
    }

    private void createEvent(String title, String category, String location, long dateTimeMillis,
                             int capacityTotal, int capacityRemaining, AlertDialog dialog) {
        eventService.createEvent(title, category, location, dateTimeMillis, capacityTotal, capacityRemaining,
                new EventActionCallback() {
                    @Override public void onSuccess() {
                        dialog.dismiss();
                        Toast.makeText(HomeActivity.this, R.string.home_event_added_success, Toast.LENGTH_SHORT).show();
                        loadEvents();
                    }
                    @Override public void onError(String errorMessage) {
                        Toast.makeText(HomeActivity.this, buildErrorMessage(R.string.home_event_save_failed, errorMessage), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateEvent(Event existingEvent, String title, String category, String location,
                             long dateTimeMillis, int capacityTotal, int capacityRemaining, AlertDialog dialog) {
        eventService.updateEvent(existingEvent, title, category, location, dateTimeMillis,
                capacityTotal, capacityRemaining, new EventActionCallback() {
                    @Override public void onSuccess() {
                        dialog.dismiss();
                        Toast.makeText(HomeActivity.this, R.string.home_event_updated_success, Toast.LENGTH_SHORT).show();
                        loadEvents();
                    }
                    @Override public void onError(String errorMessage) {
                        Toast.makeText(HomeActivity.this, buildErrorMessage(R.string.home_event_save_failed, errorMessage), Toast.LENGTH_LONG).show();
                    }
                });
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
                .setPositiveButton(activating ? R.string.home_activate_event_action : R.string.home_cancel_event_action,
                        (dialog, which) -> eventService.updateEventStatus(event, targetStatus,
                                new EventActionCallback() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(HomeActivity.this, activating
                                                ? R.string.home_event_activated_success
                                                : R.string.home_event_cancelled_success, Toast.LENGTH_SHORT).show();
                                        loadEvents();
                                    }
                                    @Override public void onError(String errorMessage) {
                                        Toast.makeText(HomeActivity.this, buildErrorMessage(R.string.home_event_cancel_failed, errorMessage), Toast.LENGTH_LONG).show();
                                    }
                                }))
                .show();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private boolean isNullOrBlank(String v) { return v == null || v.trim().isEmpty(); }
    private static String trimToEmpty(String v) { return v == null ? "" : v.trim(); }

    private long parseDateTimeInputMillis(String value) {
        String n = trimToEmpty(value);
        if (n.isEmpty()) return 0L;
        for (String pattern : DATE_TIME_PATTERNS) {
            SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
            f.setLenient(false);
            try { Date d = f.parse(n); if (d != null) return d.getTime(); } catch (Exception ignored) {}
        }
        return 0L;
    }

    private Integer parseInteger(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return null; }
    }

    private interface OnDateTimeSelectedListener { void onSelected(long epochMillis); }

    private void showDateTimePicker(long initialMillis, OnDateTimeSelectedListener listener) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(initialMillis);
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, day, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), 0);
            new TimePickerDialog(this, (tv, hour, minute) -> {
                sel.set(Calendar.HOUR_OF_DAY, hour);
                sel.set(Calendar.MINUTE, minute);
                sel.set(Calendar.SECOND, 0);
                sel.set(Calendar.MILLISECOND, 0);
                listener.onSelected(sel.getTimeInMillis());
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), true).show();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String formatDateTimeMillis(long epochMillis) {
        if (epochMillis <= 0L) return "";
        return new SimpleDateFormat(DATE_TIME_PATTERN, Locale.US).format(new Date(epochMillis));
    }

    private String buildErrorMessage(int fallbackResId, String raw) {
        String fallback = getString(fallbackResId);
        return isNullOrBlank(raw) ? fallback : fallback + " (" + raw.trim() + ")";
    }

    private void signOut() { authService.signOut(); goToAuth(); }

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