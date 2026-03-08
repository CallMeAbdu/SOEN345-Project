package com.soen345.project;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.reservation.Reservation;
import com.soen345.project.reservation.ReservationRepository;
import com.soen345.project.reservation.ReservationService;
import com.soen345.project.reservation.ReservationServiceProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyTicketsActivity extends AppCompatActivity {

    public static final String EXTRA_USER_EMAIL = "extra_user_email";
    public static final String EXTRA_USER_ROLE = "extra_user_role";

    private AuthService authService;
    private ReservationService reservationService;
    private EventService eventService;

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private LinearLayout ticketsContainer;
    private TextView ticketsEmptyText;

    public static Intent newIntent(Context context, String userEmail, String role) {
        Intent intent = new Intent(context, MyTicketsActivity.class);
        intent.putExtra(EXTRA_USER_EMAIL, userEmail);
        intent.putExtra(EXTRA_USER_ROLE, role);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_tickets);

        authService = AuthServiceProvider.getAuthService();
        reservationService = ReservationServiceProvider.getReservationService();
        eventService = EventServiceProvider.getEventService();

        toolbar = findViewById(R.id.ticketsToolbar);
        bottomNav = findViewById(R.id.ticketsBottomNav);
        ticketsContainer = findViewById(R.id.ticketsContainer);
        ticketsEmptyText = findViewById(R.id.ticketsEmptyText);

        setupToolbar();
        setupBottomNav();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ticketsRoot), (v, insets) -> {
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
        } else {
            loadMyTickets();
        }
    }

    private void loadMyTickets() {
        String userEmail = authService.getSignedInEmail();
        if (userEmail == null) return;

        ticketsEmptyText.setVisibility(View.GONE);
        ticketsContainer.removeAllViews();

        reservationService.getMyReservations(userEmail, new ReservationRepository.ReservationListCallback() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                if (reservations.isEmpty()) {
                    ticketsEmptyText.setVisibility(View.VISIBLE);
                    return;
                }
                fetchEventsAndRender(reservations);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MyTicketsActivity.this, "Error loading tickets: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEventsAndRender(List<Reservation> reservations) {
        eventService.loadEvents(new EventListCallback() {
            @Override
            public void onSuccess(List<Event> allEvents) {
                renderTickets(reservations, allEvents);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MyTicketsActivity.this, "Error loading event details: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderTickets(List<Reservation> reservations, List<Event> allEvents) {
        ticketsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Reservation res : reservations) {
            Event event = findEventById(res.getEventId(), allEvents);
            if (event == null) continue;

            View itemView = inflater.inflate(R.layout.item_browse_event, ticketsContainer, false);
            TextView titleText = itemView.findViewById(R.id.browseEventItemTitle);
            TextView detailsText = itemView.findViewById(R.id.browseEventItemDetails);
            android.widget.Button cancelButton = itemView.findViewById(R.id.browseEventReserveButton);

            titleText.setText(event.getTitle());
            String when = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(event.getDateTimeMillis()));
            detailsText.setText("When: " + when + "\nLocation: " + event.getLocation() + "\nReserved on: " +
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(res.getReservedAt())));

            cancelButton.setText("Cancel Reservation");
            cancelButton.setOnClickListener(v -> showCancelConfirmation(res, event));

            ticketsContainer.addView(itemView);
        }
    }

    private void showCancelConfirmation(Reservation res, Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Reservation")
                .setMessage("Are you sure you want to cancel your reservation for " + event.getTitle() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelReservation(res, event))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelReservation(Reservation res, Event event) {
        reservationService.cancelReservation(res, event, new ReservationRepository.ReservationActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MyTicketsActivity.this, "Reservation cancelled", Toast.LENGTH_SHORT).show();
                loadMyTickets(); // Reload list
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MyTicketsActivity.this, "Failed to cancel: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Event findEventById(String eventId, List<Event> allEvents) {
        for (Event e : allEvents) {
            if (e.getDocumentId().equals(eventId)) return e;
        }
        return null;
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setTitleTextAppearance(this, R.style.BrowseToolbarTitleStyle);

        String email = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        if (email == null || email.trim().isEmpty()) email = authService.getSignedInEmail();
        if (email == null || email.trim().isEmpty()) email = getString(R.string.auth_unknown_user);
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
            authService.signOut();
            goToAuth();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_my_tickets);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_tickets) {
                return true;
            }
            if (id == R.id.nav_browse_events) {
                String email = getIntent().getStringExtra(EXTRA_USER_EMAIL);
                String role = getIntent().getStringExtra(EXTRA_USER_ROLE);
                startActivity(BrowseEventsActivity.newIntent(this, email, role));
                finish();
                return true;
            }
            return false;
        });
    }

    private void goToAuth() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
