package com.uilover.project1992.Activity; // Thay đổi package cho phù hợp

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem; // Import cho nút back trên Action Bar
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Nếu bạn muốn dùng Toolbar cho nút back

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // Dùng cho merge khi set
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.uilover.project1992.R; // Thay đổi package cho phù hợp

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ImageView imgAvatar;
    private EditText edtEmail, edtPhone, edtName, edtBirthdate, edtHometown;
    private Button btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private StorageReference storageRef;

    private Uri imageUri; // Lưu Uri của ảnh mới được chọn
    private String currentProfileImageUrl = null; // Lưu URL ảnh hiện tại
    private ProgressDialog progressDialog;
    private final Calendar myCalendar = Calendar.getInstance();

    // ActivityResultLauncher for picking images
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    // Hiển thị ảnh đã chọn
                    Glide.with(this).load(imageUri).circleCrop().into(imgAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile); // Đảm bảo tên layout đúng

        // --- TÙY CHỌN: Thêm nút Back trên Action Bar ---
        // Bạn cần có một Toolbar trong XML với id là 'toolbar' hoặc tương tự
        // Toolbar toolbar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);
        // if (getSupportActionBar() != null) {
        //    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //    getSupportActionBar().setDisplayShowHomeEnabled(true);
        //    getSupportActionBar().setTitle("Edit Profile"); // Set tiêu đề
        // }
        // -----------------------------------------------

        // Ánh xạ Views
        imgAvatar = findViewById(R.id.img_avatar);
        edtEmail = findViewById(R.id.edt_email);
        edtPhone = findViewById(R.id.edt_phone);
        edtName = findViewById(R.id.edt_name);
        edtBirthdate = findViewById(R.id.edt_birthdate);
        edtHometown = findViewById(R.id.edt_hometown);
        btnSave = findViewById(R.id.btn_save);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải...");
        progressDialog.setCancelable(false);

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = db.collection("users").document(userId);
            storageRef = storage.getReference().child("profile_images").child(userId + ".jpg"); // Đường dẫn lưu ảnh trên Storage
            loadUserProfileData();
        } else {
            // Người dùng chưa đăng nhập, xử lý (ví dụ: quay lại Login)
            Toast.makeText(this, "Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, LoginActivity.class);
            // startActivity(intent);
            finish();
            return; // Dừng thực thi tiếp nếu user null
        }

        // --- Setup Listeners ---

        // Listener chọn ảnh đại diện
        imgAvatar.setOnClickListener(v -> selectImage());

        // Listener chọn ngày sinh
        setupDatePicker();
        edtBirthdate.setOnClickListener(v -> showDatePickerDialog());

        // Listener nút lưu
        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    // --- TÙY CHỌN: Xử lý nút back trên Action Bar ---
    // @Override
    // public boolean onOptionsItemSelected(MenuItem item) {
    //    // handle arrow click here
    //    if (item.getItemId() == android.R.id.home) {
    //        finish(); // close this activity and return to previous one (if any)
    //    }
    //    return super.onOptionsItemSelected(item);
    // }
    // -------------------------------------------------


    private void loadUserProfileData() {
        progressDialog.show();
        userRef.get().addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    edtName.setText(document.getString("name"));
                    edtEmail.setText(document.getString("email")); // Email thường lấy từ Auth hoặc không cho sửa
                    edtPhone.setText(document.getString("phone")); // Tương tự email
                    edtBirthdate.setText(document.getString("birthdate"));
                    edtHometown.setText(document.getString("hometown"));

                    currentProfileImageUrl = document.getString("profileImageUrl");
                    if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(currentProfileImageUrl)
                                .placeholder(R.drawable.ic_person) // Placeholder từ XML
                                .error(R.drawable.ic_person)       // Ảnh lỗi
                                .circleCrop() // Bo tròn ảnh nếu muốn
                                .into(imgAvatar);
                    } else {
                        // Set ảnh mặc định nếu không có URL
                        Glide.with(this).load(R.drawable.ic_person).circleCrop().into(imgAvatar);
                    }
                    // Cập nhật Calendar nếu có ngày sinh
                    updateCalendarFromEditText();

                } else {
                    Log.d(TAG, "No such document");
                    Toast.makeText(EditProfileActivity.this, "Không tìm thấy hồ sơ.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "get failed with ", task.getException());
                Toast.makeText(EditProfileActivity.this, "Lỗi tải dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Hoặc dùng ACTION_GET_CONTENT nếu muốn trình chọn khác
        // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        };

        edtBirthdate.setOnClickListener(v -> new DatePickerDialog(EditProfileActivity.this, dateSetListener,
                myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show());
    }
    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel(); // Cập nhật EditText sau khi chọn ngày
        };

        new DatePickerDialog(EditProfileActivity.this, dateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }


    private void updateLabel() {
        String myFormat = "dd/MM/yyyy"; // Định dạng ngày tháng
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        edtBirthdate.setText(sdf.format(myCalendar.getTime()));
    }

    // Cập nhật Calendar từ EditText khi load dữ liệu
    private void updateCalendarFromEditText() {
        String birthdateStr = edtBirthdate.getText().toString();
        if (!birthdateStr.isEmpty()) {
            String myFormat = "dd/MM/yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            try {
                myCalendar.setTime(Objects.requireNonNull(sdf.parse(birthdateStr)));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date: " + birthdateStr, e);
                // Có thể reset Calendar về ngày hiện tại nếu parse lỗi
                // myCalendar.setTimeInMillis(System.currentTimeMillis());
            }
        }
    }


    private void saveProfileChanges() {
        final String name = edtName.getText().toString().trim();
        final String birthdate = edtBirthdate.getText().toString().trim();
        final String hometown = edtHometown.getText().toString().trim();

        // --- Validate dữ liệu (Tùy chọn) ---
        if (name.isEmpty()) {
            edtName.setError("Tên không được để trống");
            edtName.requestFocus();
            return;
        }
        // Thêm các validate khác nếu cần
        // --- Kết thúc Validate ---

        progressDialog.setMessage("Đang lưu thay đổi...");
        progressDialog.show();

        if (imageUri != null) {
            // Nếu người dùng chọn ảnh mới -> Upload ảnh rồi mới cập nhật Firestore
            uploadImageAndSaveData(name, birthdate, hometown);
        } else {
            // Nếu không chọn ảnh mới -> Chỉ cập nhật Firestore với dữ liệu text
            updateFirestoreData(name, birthdate, hometown, currentProfileImageUrl); // Dùng URL ảnh cũ
        }
    }

    private void uploadImageAndSaveData(final String name, final String birthdate, final String hometown) {
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String newImageUrl = uri.toString();
                            updateFirestoreData(name, birthdate, hometown, newImageUrl);
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(EditProfileActivity.this, "Lỗi lấy URL ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error getting download URL", e);
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error uploading image", e);
                });
    }

    private void updateFirestoreData(String name, String birthdate, String hometown, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("birthdate", birthdate);
        updates.put("hometown", hometown);
        // Chỉ cập nhật URL ảnh nếu nó khác null (có thể là URL mới hoặc cũ)
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }
        // Lưu ý: Không cập nhật email và phone ở đây vì chúng đang bị disable

        userRef.set(updates, SetOptions.merge()) // Dùng merge để chỉ cập nhật các trường có trong map
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User profile updated successfully.");
                    // Có thể đóng Activity sau khi lưu thành công
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating document", e);
                });
    }
}