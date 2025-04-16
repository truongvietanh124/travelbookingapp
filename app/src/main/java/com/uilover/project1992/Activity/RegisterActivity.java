package com.uilover.project1992.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
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

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private EditText edtEmail, edtPassword;
    private Button btnRegister;
    private TextView tvAlreadyHaveAccount;
    private CheckBox checkboxTerms;
    private ImageButton btnFacebookRegister, btnGoogleRegister, btnAppleRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtEmail = findViewById(R.id.edit_text_register_email);
        edtPassword = findViewById(R.id.edit_text_register_password);
        btnRegister = findViewById(R.id.btn_register);
        tvAlreadyHaveAccount = findViewById(R.id.tv_already_have_account);
        checkboxTerms = findViewById(R.id.checkbox_terms);
        btnFacebookRegister = findViewById(R.id.btn_facebook_register);
        btnGoogleRegister = findViewById(R.id.btn_google_register);
        btnAppleRegister = findViewById(R.id.btn_apple_register);

        // Google Sign-In setup
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

        btnRegister.setOnClickListener(v -> registerUser());
        tvAlreadyHaveAccount.setOnClickListener(v -> goToLogin());

        // Xử lý các nút đăng ký mạng xã hội (chưa triển khai đầy đủ)
        btnFacebookRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Facebook Register is under development", Toast.LENGTH_SHORT).show();
            // TODO: Implement Facebook registration
        });
        btnGoogleRegister.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
        btnAppleRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Apple Register is under development", Toast.LENGTH_SHORT).show();
            // TODO: Implement Apple registration
        });
    }

    private void registerUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Xác thực đầu vào
        if (!validateInput(email, password)) {
            return; // Dừng nếu xác thực không thành công
        }

        // Kiểm tra checkbox điều khoản
        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this, "Please accept the terms and privacy policy", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo tài khoản Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Lưu thông tin người dùng vào Firestore (tối thiểu là email)
                            saveUserToFirestore(user, email);
                        }
                    } else {
                        // Nếu đăng ký thất bại
                        String errorMessage = "Registration failed: ";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage += "Email address is already in use.";
                            edtEmail.setError("Email address is already in use.");
                        } else {
                            errorMessage += task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e("Firebase Auth", "createUserWithEmail:failure", task.getException());
                    }
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

        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void saveUserToFirestore(FirebaseUser user, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        // Thêm các trường dữ liệu khác nếu cần (ví dụ: name, profileImageUrl, v.v.)

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    goToProfile(); // Hoặc chuyển đến trang xác minh email nếu cần
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error saving user data", e);
                    // Có thể cần xóa tài khoản Firebase nếu lưu Firestore thất bại (tùy thuộc vào yêu cầu)
                    // mAuth.getCurrentUser().delete(); // Cẩn thận với việc xóa tài khoản
                });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void goToProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
        finish();
    }

    // --- Google Sign-In Logic ---

    private void handleGoogleSignIn(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, signInTask -> {
                        if (signInTask.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                createUserWithGoogle(firebaseUser, account); // Luôn tạo mới
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

    private void createUserWithGoogle(FirebaseUser firebaseUser, GoogleSignInAccount account) {
        DocumentReference userRef = db.collection("users").document(firebaseUser.getUid());

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", account.getEmail());
        userData.put("name", account.getDisplayName());
        userData.put("phone", firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : ""); // Có thể null
        userData.put("birthdate", null);
        userData.put("hometown", null);
        userData.put("paymentMethod", null);
        userData.put("profileImageUrl", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null);
        userData.put("hasAlbum", false);
        userData.put("albumId", null);

        userRef.set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Google Registration Successful", Toast.LENGTH_SHORT).show();
                    goToProfile(); // Hoặc trang xác nhận, hướng dẫn, v.v.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create Google account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error creating user with Google data", e);
                    // Có thể cần xóa tài khoản Firebase nếu lưu Firestore thất bại (tùy thuộc vào yêu cầu)
                    // mAuth.getCurrentUser().delete(); // Cẩn thận!
                });
    }
}