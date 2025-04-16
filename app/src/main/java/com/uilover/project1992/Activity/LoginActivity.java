package com.uilover.project1992.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.uilover.project1992.R;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private EditText edtEmail, edtPassword;
    private Button btnSignIn, btnSignUp;
    private TextView tvForgotPassword;
    private ImageButton btnGoogle, btnFacebook, btnTwitter, btnInstagram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtEmail = findViewById(R.id.editTextUsername); // Đổi ID nếu cần
        edtPassword = findViewById(R.id.editTextPassword); // Đổi ID nếu cần
        btnSignIn = findViewById(R.id.btnSignIn); // Đổi ID nếu cần
        btnSignUp = findViewById(R.id.btnSignUp); // Đổi ID nếu cần
        tvForgotPassword = findViewById(R.id.tvForgotPassword); // Đổi ID nếu cần
        btnGoogle = findViewById(R.id.btnGoogleLogin); // Đổi ID nếu cần
        btnFacebook = findViewById(R.id.btnFacebookLogin); // Đổi ID nếu cần
        btnTwitter = findViewById(R.id.btnTwitterLogin); // Đổi ID nếu cần
        btnInstagram = findViewById(R.id.btnInstagramLogin); // Đổi ID nếu cần

        // Google Sign-In setup (giữ nguyên hoặc điều chỉnh nếu cần)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignIn(task);
                    } else {
                        // Xử lý trường hợp người dùng không chọn tài khoản Google
                        Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show();
                    }
                });

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // Đăng nhập bằng Email/Mật khẩu (giữ nguyên)
        btnSignIn.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (!validateInput(email, password)) {
                return; // Dừng nếu xác thực không thành công
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Đăng nhập thành công
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                goToProfile();
                            }
                        } else {
                            // Đăng nhập thất bại
                            String errorMessage = "Login failed: ";
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                errorMessage += "Email address not found.";
                                edtEmail.setError("Email address not found.");
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage += "Invalid password.";
                                edtPassword.setError("Invalid password.");
                            } else {
                                errorMessage += task.getException().getMessage();
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                            Log.e("Firebase Auth", "signInWithEmailAndPassword failed", task.getException());
                        }
                    });
        });

        btnSignUp.setOnClickListener(v -> goToRegister());

        // Xử lý Quên Mật Khẩu
        tvForgotPassword.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Email is required");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Invalid email format");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent. Check your inbox.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to send password reset email: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            Log.e("Firebase Auth", "sendPasswordResetEmail failed", task.getException());
                        }
                    });
        });

        // Các nút đăng nhập mạng xã hội khác (có thể triển khai sau)
        btnFacebook.setOnClickListener(v -> {
            Toast.makeText(this, "Facebook Login is under development", Toast.LENGTH_SHORT).show();
            // TODO: Triển khai logic đăng nhập Facebook
        });

        btnTwitter.setOnClickListener(v -> {
            Toast.makeText(this, "Twitter Login is under development", Toast.LENGTH_SHORT).show();
            // TODO: Triển khai logic đăng nhập Twitter
        });

        btnInstagram.setOnClickListener(v -> {
            Toast.makeText(this, "Instagram Login is under development", Toast.LENGTH_SHORT).show();
            // TODO: Triển khai logic đăng nhập Instagram
        });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Invalid email format");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Password is required");
            return false;
        }

        return true;
    }

    private void handleGoogleSignIn(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, signInTask -> {
                        if (signInTask.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                checkAndCreateUser(firebaseUser, account);
                            }
                        } else {
                            Toast.makeText(this, "Google Sign-In failed: " + signInTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("Google SignIn", "signInWithCredential failed", signInTask.getException());
                        }
                    });
        } catch (ApiException e) {
            Log.w("Google SignIn", "signInResult:failed code=" + e.getStatusCode() + ", message: " + e.getMessage());
            Toast.makeText(this, "Google Sign-In error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndCreateUser(FirebaseUser firebaseUser, GoogleSignInAccount account) {
        String userEmail = account.getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Cannot get email from Google account", Toast.LENGTH_SHORT).show();
            mAuth.signOut(); // Sign out if no email
            return;
        }

        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        // User exists, go to profile
                        goToProfile();
                    } else {
                        // User does not exist, create new
                        createUserWithGoogle(firebaseUser, account);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking user existence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error checking user existence by email", e);
                });
    }

    private void createUserWithGoogle(FirebaseUser firebaseUser, GoogleSignInAccount account) {
        DocumentReference userRef = db.collection("users").document(firebaseUser.getUid());

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", account.getEmail());
        userData.put("name", account.getDisplayName());
        userData.put("phone", firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : ""); // May be null
        userData.put("birthdate", null);
        userData.put("hometown", null);
        userData.put("paymentMethod", null);
        userData.put("profileImageUrl", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null);
        userData.put("hasAlbum", false);
        userData.put("albumId", null);

        userRef.set(userData)
                .addOnSuccessListener(aVoid -> {
                    goToProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error creating new user with Google data", e);
                    // Consider deleting the Firebase user if Firestore save fails
                    // mAuth.getCurrentUser().delete(); // Be cautious with this!
                });
    }

    private void goToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    private void goToProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
        finish();
    }
}