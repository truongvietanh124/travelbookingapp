package com.uilover.project1992.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.uilover.project1992.R;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvUsername, tvUserEmail;
    private ImageView backBtn; // Thêm biến cho nút back

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Ánh xạ các view từ layout
        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvUsername = findViewById(R.id.tv_username);
        tvUserEmail = findViewById(R.id.tv_user_email); // ID của TextView
        backBtn = findViewById(R.id.backBtn); // Ánh xạ nút back

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();


        ImageButton btnEditProfile = findViewById(R.id.btn_edit_profile);

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });

        if (currentUser != null) {
            loadUserProfile();
        } else {
            // Xử lý trường hợp không có người dùng đăng nhập (ví dụ: chuyển về trang đăng nhập)
            // Ví dụ:
            // startActivity(new Intent(this, LoginActivity.class));
            // finish();
        }

        // Xử lý sự kiện click cho nút back
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng ProfileActivity sau khi chuyển về MainActivity
        });
        Button logoutBtn = findViewById(R.id.btn_logout);
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // hoặc logout logic khác
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserProfile() {
        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String name = document.getString("name");
                    String email = document.getString("email");
                    String profileImageUrl = document.getString("profileImageUrl");

                    tvUsername.setText(name);
                    tvUserEmail.setText(email);

                    // Hiển thị ảnh người dùng (nếu có)
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_user_placeholder) // Ảnh mặc định khi đang tải
                                .error(R.drawable.ic_user_placeholder) // Ảnh mặc định khi có lỗi tải
                                .into(ivProfileImage);
                    } else {
                        // Nếu không có URL ảnh, ImageView sẽ tự động hiển thị ảnh mặc định đã được set trong XML
                        // Trong trường hợp bạn muốn set lại ảnh mặc định từ code, bạn có thể dùng:
                        // ivProfileImage.setImageResource(R.drawable.ic_user_placeholder);
                    }
                } else {
                    // Xử lý trường hợp không tìm thấy thông tin người dùng trong Firestore
                    // Có thể do lỗi hoặc người dùng chưa hoàn thành thông tin
                }
            } else {
                // Xử lý trường hợp lỗi khi truy vấn Firestore
            }
        });
    }
}