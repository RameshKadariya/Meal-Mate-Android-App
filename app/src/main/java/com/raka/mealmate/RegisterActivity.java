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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.raka.mealmate.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.common.api.CommonStatusCodes;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        binding.registerButton.setOnClickListener(v -> attemptRegister());
        binding.loginLink.setOnClickListener(v -> finish());
        binding.backButton.setOnClickListener(v -> onBackPressed());
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void attemptRegister() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty()) {
            binding.nameInput.setError("Name is required");
            return;
        }
        if (email.isEmpty()) {
            binding.emailInput.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            binding.passwordInput.setError("Password is required");
            return;
        }
        if (password.length() < 4) {
            binding.passwordInput.setError("Password must be at least 4 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordInput.setError("Passwords don't match");
            return;
        }

        // Show loading state
        binding.registerButton.setEnabled(false);
        binding.registerProgress.setVisibility(View.VISIBLE);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Update user profile
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        mAuth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        // Send verification email
                                        sendEmailVerification();
                                    } else {
                                        showToast("Failed to update profile");
                                        resetUI();
                                    }
                                });
                    } else {
                        showToast("Registration failed: " + task.getException().getMessage());
                        resetUI();
                    }
                });
    }

    private void sendEmailVerification() {
        mAuth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(verificationTask -> {
                    if (verificationTask.isSuccessful()) {
                        showToast("Verification email sent. Please check your email.");
                        mAuth.signOut(); // Sign out the user to prevent unverified access
                        finish(); // Close RegisterActivity and return to LoginActivity
                    } else {
                        showToast("Failed to send verification email: " + verificationTask.getException().getMessage());
                    }
                    resetUI();
                });
    }

    private void resetUI() {
        binding.registerButton.setEnabled(true);
        binding.registerProgress.setVisibility(View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void signInWithGoogle() {
        binding.googleSignInButton.setEnabled(false);
        binding.registerProgress.setVisibility(View.VISIBLE);
        
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
                binding.googleSignInButton.setEnabled(true);
                binding.registerProgress.setVisibility(View.GONE);
                
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
                    binding.registerProgress.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        startMainActivity();
                    } else {
                        showToast("Authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void animateViews() {
        // Animate back button
        binding.backButton.setAlpha(0f);
        binding.backButton.animate()
                .alpha(1f)
                .setDuration(500)
                .start();

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
