package com.soen345.project;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;

public class HomeActivity extends AppCompatActivity {
    public static final String EXTRA_USER_EMAIL = "extra_user_email";

    private AuthService authService;
    private TextView homeUserEmailText;

    public static Intent newIntent(Context context, String userEmail) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(EXTRA_USER_EMAIL, userEmail);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        authService = AuthServiceProvider.getAuthService();

        homeUserEmailText = findViewById(R.id.homeUserEmailText);
        Button signOutButton = findViewById(R.id.homeSignOutButton);

        showSignedInEmail();
        signOutButton.setOnClickListener(v -> signOut());

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
        }
    }

    private void showSignedInEmail() {
        String fromIntent = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        String fromSession = authService.getSignedInEmail();
        String chosenEmail = fromIntent;
        if (chosenEmail == null || chosenEmail.isBlank()) {
            chosenEmail = fromSession;
        }
        if (chosenEmail == null || chosenEmail.isBlank()) {
            chosenEmail = getString(R.string.auth_unknown_user);
        }
        homeUserEmailText.setText(getString(R.string.auth_signed_in_as, chosenEmail));
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
