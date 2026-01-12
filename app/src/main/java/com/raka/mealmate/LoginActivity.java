package com.raka.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.raka.mealmate.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        setupClickListeners();
        animateViews();
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> attemptLogin());
        binding.registerLink.setOnClickListener(v -> startRegisterActivity());
        binding.forgotPasswordLink.setOnClickListener(v -> handleForgotPassword());
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        // Show loading state
        binding.googleSignInButton.setEnabled(false);
        binding.loginProgress.setVisibility(View.VISIBLE);
        
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Reset UI state
                binding.googleSignInButton.setEnabled(true);
                binding.loginProgress.setVisibility(View.GONE);
                
                // Show specific error message based on error code
                String errorMessage;
                switch (e.getStatusCode()) {
                    case CommonStatusCodes.CANCELED:
                        errorMessage = "Google Sign in cancelled";
                        break;
                    case CommonStatusCodes.NETWORK_ERROR:
                        errorMessage = "Network error occurred";
                        break;
                    default:
                        errorMessage = "Google sign in failed: " + e.getStatusCode();
                }
                showToast(errorMessage);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    binding.loginProgress.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        startMainActivity();
                    } else {
                        showToast("Authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    private void attemptLogin() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        // Validate inputs
        if (email.isEmpty()) {
            binding.emailInput.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            binding.passwordInput.setError("Password is required");
            return;
        }

        // Show loading state
        binding.loginButton.setEnabled(false);
        binding.loginProgress.setVisibility(View.VISIBLE);

        // Attempt Firebase login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.loginButton.setEnabled(true);
                    binding.loginProgress.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser().isEmailVerified()) {
                            startMainActivity();
                        } else {
                            showToast("Please verify your email before logging in.");
                            FirebaseAuth.getInstance().signOut(); // Prevent unverified login
                        }
                    } else {
                        showToast("Authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    private void handleForgotPassword() {
        String email = binding.emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            binding.emailInput.setError("Enter your email first");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Password reset email sent.");
                    } else {
                        showToast("Failed to send reset email: " + task.getException().getMessage());
                    }
                });
    }

    private void startRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void animateViews() {
        // Load and start logo animation
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_fade_in);
        binding.logoImage.startAnimation(logoAnim);

        // Animate content with slight delay
        LinearLayout contentLayout = binding.getRoot().findViewById(R.id.contentLayout);
        contentLayout.setAlpha(0f);
        contentLayout.setTranslationY(50f);
        contentLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setStartDelay(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
