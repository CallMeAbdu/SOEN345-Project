package com.soen345.project;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthSession;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.UserRole;

public class MainActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button authButton;
    private TextView titleText;
    private TextView modeSwitchText;
    private TextView statusText;
    private ProgressBar authProgressBar;

    private boolean isRegisterMode;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        authService = AuthServiceProvider.getAuthService();

        bindViews();
        setupListeners();
        renderAuthMode();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSignedInState();
    }

    private void bindViews() {
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        authButton = findViewById(R.id.authButton);
        titleText = findViewById(R.id.titleText);
        modeSwitchText = findViewById(R.id.modeSwitchText);
        statusText = findViewById(R.id.authStatusText);
        authProgressBar = findViewById(R.id.authProgressBar);
    }

    private void setupListeners() {
        modeSwitchText.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            statusText.setVisibility(View.GONE);
            renderAuthMode();
        });

        authButton.setOnClickListener(v -> submitAuth());
    }

    private void renderAuthMode() {
        if (isRegisterMode) {
            titleText.setText(R.string.auth_title_register);
            authButton.setText(R.string.auth_action_register);
            modeSwitchText.setText(R.string.auth_switch_to_sign_in);
            emailInput.setHint(R.string.auth_hint_email);
            phoneInput.setVisibility(View.VISIBLE);
            confirmPasswordInput.setVisibility(View.VISIBLE);
        } else {
            titleText.setText(R.string.auth_title_sign_in);
            authButton.setText(R.string.auth_action_sign_in);
            modeSwitchText.setText(R.string.auth_switch_to_register);
            emailInput.setHint(R.string.auth_hint_email_or_phone);
            phoneInput.setVisibility(View.GONE);
            phoneInput.setText("");
            confirmPasswordInput.setVisibility(View.GONE);
            confirmPasswordInput.setText("");
        }
    }

    private void submitAuth() {
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        setLoading(true);

        AuthCallback callback = new AuthCallback() {
            @Override
            public void onSuccess(AuthSession session) {
                setLoading(false);
                statusText.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, R.string.auth_success, Toast.LENGTH_SHORT).show();
                navigateToHome(session.getEmail(), session.getRole());
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                statusText.setText(errorMessage);
                statusText.setVisibility(View.VISIBLE);
            }
        };

        if (isRegisterMode) {
            authService.register(email, phone, password, confirmPassword, callback);
        } else {
            authService.signIn(email, password, callback);
        }
    }

    private void setLoading(boolean isLoading) {
        authProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        authButton.setEnabled(!isLoading);
        modeSwitchText.setEnabled(!isLoading);
        emailInput.setEnabled(!isLoading);
        phoneInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
        confirmPasswordInput.setEnabled(!isLoading);
    }

    private void refreshSignedInState() {
        if (authService.isSignedIn()) {
            UserRole role = authService.getSignedInRole();
            if (role != null) {
                navigateToHome(authService.getSignedInEmail(), role);
            } else {
                authService.signOut();
            }
        }
        statusText.setVisibility(View.GONE);
        setLoading(false);
    }

    private void navigateToHome(String email, UserRole role) {
        startActivity(HomeActivity.newIntent(this, email, role));
        finish();
    }
}
