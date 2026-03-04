package com.soen345.project;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;

public class MyTicketsActivity extends AppCompatActivity {

    public static final String EXTRA_USER_EMAIL = "extra_user_email";
    public static final String EXTRA_USER_ROLE = "extra_user_role";

    private AuthService authService;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;

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

        toolbar = findViewById(R.id.ticketsToolbar);
        bottomNav = findViewById(R.id.ticketsBottomNav);

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
        }
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
        // Purple sign out — same as BrowseEventsActivity
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