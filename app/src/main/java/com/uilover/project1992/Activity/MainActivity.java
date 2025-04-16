package com.uilover.project1992.Activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast; // Thêm import cho Toast (nếu cần hiển thị lỗi)

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide; // <<< THÊM IMPORT CHO GLIDE
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference; // <<< THÊM IMPORT FIRESTORE
import com.google.firebase.firestore.DocumentSnapshot; // <<< THÊM IMPORT FIRESTORE
import com.google.firebase.firestore.FirebaseFirestore; // <<< THÊM IMPORT FIRESTORE
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.uilover.project1992.Model.Location;
import com.uilover.project1992.R;
import com.uilover.project1992.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity"; // Thêm TAG để Log dễ hơn
    private ActivityMainBinding binding;
    private int adultPassenger = 1, childPassenger = 1;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.ENGLISH); // Sửa định dạng ngày nếu cần
    private Calendar calendar = Calendar.getInstance();

    // --- Biến Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // <<< THÊM BIẾN FIRESTORE
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Firebase một lần (có thể đã làm ở BaseActivity hoặc Application)
        // FirebaseApp.initializeApp(this);
        Log.d(TAG, "Initialized Firebase components");

        // --- Khởi tạo Firebase Auth và Firestore ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // <<< KHỞI TẠO FIRESTORE
        currentUser = mAuth.getCurrentUser(); // <<< LẤY USER HIỆN TẠI

        // --- Tải thông tin Header nếu đã đăng nhập ---
        if (currentUser != null) {
            loadUserProfileHeader(); // <<< GỌI HÀM TẢI THÔNG TIN
        } else {
            // Xử lý khi chưa đăng nhập: hiển thị giá trị mặc định
            binding.textView4.setText("Khách"); // Tên mặc định
            // Đặt ảnh mặc định (có thể dùng Glide để load drawable nếu muốn bo tròn)
            Glide.with(this)
                    .load(R.drawable.profile) // Load ảnh mặc định
                    .circleCrop() // Bo tròn
                    .into(binding.imageView3);
            // Hoặc đơn giản: binding.imageView3.setImageResource(R.drawable.profile);
            Log.d(TAG, "User not logged in, showing default header.");
        }

        // --- Gọi các hàm khởi tạo khác ---
        initLocations();
        initPassengers();
        initClassSeat();
        initDatePickup();
        setVariable();
        setupBottomNav(); // <<< Gọi hàm cài đặt Bottom Nav
    }

    // Kiểm tra trạng thái đăng nhập
    private boolean isLoggedIn() {
        // Có thể dùng biến currentUser đã lấy ở onCreate cho hiệu quả hơn
        // return currentUser != null;
        return FirebaseAuth.getInstance().getCurrentUser() != null; // Giữ nguyên nếu logic cũ cần
    }

    // --- HÀM MỚI: Tải thông tin người dùng cho Header ---
    private void loadUserProfileHeader() {
        Log.d(TAG, "Loading user profile header for user: " + currentUser.getUid());
        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("users").document(userId); // Đảm bảo collection là "users"

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Log.d(TAG, "User document found. Data: " + document.getData());
                    String name = document.getString("name"); // Lấy tên từ Firestore
                    String profileImageUrl = document.getString("profileImageUrl"); // Lấy URL ảnh từ Firestore

                    // Cập nhật Tên (sử dụng View Binding)
                    if (name != null && !name.isEmpty()) {
                        binding.textView4.setText(name);
                    } else {
                        binding.textView4.setText("Người dùng"); // Tên mặc định nếu trống
                    }

                    // Cập nhật Ảnh Đại Diện (sử dụng Glide và View Binding)
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(MainActivity.this) // Context là MainActivity
                                .load(profileImageUrl) // Tải từ URL
                                .placeholder(R.drawable.profile) // Ảnh chờ
                                .error(R.drawable.profile) // Ảnh lỗi
                                .circleCrop() // Bo tròn ảnh
                                .into(binding.imageView3); // Đặt vào ImageView qua binding
                    } else {
                        // Load ảnh mặc định nếu không có URL
                        Log.d(TAG, "Profile image URL is missing, loading default.");
                        Glide.with(MainActivity.this)
                                .load(R.drawable.profile)
                                .circleCrop()
                                .into(binding.imageView3);
                        // Hoặc: binding.imageView3.setImageResource(R.drawable.profile);
                    }
                } else {
                    // Không tìm thấy document người dùng trong Firestore
                    Log.w(TAG, "No Firestore document found for user: " + userId);
                    binding.textView4.setText("Người dùng"); // Đặt tên mặc định
                    binding.imageView3.setImageResource(R.drawable.profile); // Đặt ảnh mặc định
                }
            } else {
                // Lỗi khi truy cập Firestore
                Log.e(TAG, "Error getting user document from Firestore: ", task.getException());
                // Toast.makeText(MainActivity.this, "Lỗi tải thông tin.", Toast.LENGTH_SHORT).show();
                binding.textView4.setText("Người dùng"); // Đặt tên mặc định khi lỗi
                binding.imageView3.setImageResource(R.drawable.profile); // Đặt ảnh mặc định khi lỗi
            }
        });
    }
    // --- Kết thúc hàm mới ---


    // --- HÀM MỚI: Cài đặt Bottom Navigation Bar ---
    private void setupBottomNav() {
        // ChipNavigationBar bottomNav = findViewById(R.id.bottom_nav); // Nếu không có trong binding
        // Nên dùng binding nếu bottom_nav nằm trực tiếp trong activity_main.xml
        ChipNavigationBar bottomNav = binding.bottomNav; // Giả sử ID là bottomNav trong binding

        bottomNav.setOnItemSelectedListener(id -> { // Dùng lambda cho ngắn gọn
            if (id == R.id.home) {
                // Đang ở Home rồi, không cần xử lý gì thêm hoặc refresh nếu cần
            } else if (id == R.id.explorer) {
                // Xử lý Explorer (ví dụ: mở Activity/Fragment mới)
                // Toast.makeText(this, "Explorer Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.bookmark) {
                // Xử lý Bookmark
                // Toast.makeText(this, "Bookmark Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.profile) {
                Intent intent; // Khai báo Intent trong khối lệnh
                if (isLoggedIn()) { // Kiểm tra trạng thái đăng nhập
                    intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } else {
                    intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        });
        // Có thể đặt mục được chọn mặc định nếu muốn
        // bottomNav.setItemSelected(R.id.home, true);
    }
    // --- Kết thúc hàm mới ---


    private void setVariable() {
        binding.searchBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            // Lấy dữ liệu từ Spinner một cách an toàn hơn
            Object fromItem = binding.fromSp.getSelectedItem();
            Object toItem = binding.toSp.getSelectedItem();

            if (fromItem instanceof Location) {
                intent.putExtra("from", ((Location) fromItem).getName());
            }
            if (toItem instanceof Location) {
                intent.putExtra("to", ((Location) toItem).getName());
            }

            intent.putExtra("date", binding.departureDateTxt.getText().toString());
            intent.putExtra("numPassenger", adultPassenger + childPassenger);
            startActivity(intent);
        });
    }

    private void initDatePickup() {
        Calendar calendarToday = Calendar.getInstance();
        String currentDate = dateFormat.format(calendarToday.getTime());
        binding.departureDateTxt.setText(currentDate);

        Calendar calendarTommorow = Calendar.getInstance();
        calendarTommorow.add(Calendar.DAY_OF_YEAR, 1);
        String tommorowDate = dateFormat.format(calendarTommorow.getTime());
        binding.returnDateTxt.setText(tommorowDate);

        binding.departureDateTxt.setOnClickListener(v -> showDatePickerDialog(binding.departureDateTxt));
        binding.returnDateTxt.setOnClickListener(v -> showDatePickerDialog(binding.returnDateTxt));
    }

    private void initClassSeat() {
        binding.progressBarClass.setVisibility(View.VISIBLE);
        ArrayList<String> list = new ArrayList<>();
        list.add("Business Class");
        list.add("First Class");
        list.add("Economy Class");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.classSp.setAdapter(adapter);
        binding.progressBarClass.setVisibility(View.GONE);
    }

    private void initPassengers() {
        // Cập nhật text ban đầu
        binding.AdultTxt.setText(adultPassenger + " Adult");
        binding.childTxt.setText(childPassenger + " Child");

        binding.plusAdultBtn.setOnClickListener(v -> {
            adultPassenger++;
            binding.AdultTxt.setText(adultPassenger + " Adult");
        });

        binding.minusAdultBtn.setOnClickListener(v -> {
            if (adultPassenger > 1) { // Cho phép tối thiểu 1 người lớn
                adultPassenger--;
                binding.AdultTxt.setText(adultPassenger + " Adult");
            }
        });

        binding.plusChildBtn.setOnClickListener(v -> {
            childPassenger++;
            binding.childTxt.setText(childPassenger + " Child");
        });

        binding.minusChildBtn.setOnClickListener(v -> {
            if (childPassenger > 0) { // Cho phép tối thiểu 0 trẻ em
                childPassenger--;
                binding.childTxt.setText(childPassenger + " Child");
            }
        });
    }

    private void initLocations() {
        binding.progressBarFrom.setVisibility(View.VISIBLE);
        binding.progressBarTo.setVisibility(View.VISIBLE);
        // Giả sử 'database' được khởi tạo ở BaseActivity hoặc đâu đó
        if (database == null) {
            Log.e(TAG, "Firebase Realtime Database instance is null in initLocations!");
            binding.progressBarFrom.setVisibility(View.GONE);
            binding.progressBarTo.setVisibility(View.GONE);
            Toast.makeText(this, "Lỗi tải địa điểm.", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference myRef = database.getReference("Locations");
        ArrayList<Location> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    list.clear(); // Xóa list cũ trước khi thêm mới
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        try {
                            Location loc = issue.getValue(Location.class);
                            if (loc != null) { // Kiểm tra null trước khi thêm
                                list.add(loc);
                            } else {
                                Log.w(TAG, "Null Location object found for key: " + issue.getKey());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting location snapshot: " + issue.toString(), e);
                        }
                    }
                    if (!list.isEmpty()) {
                        ArrayAdapter<Location> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.fromSp.setAdapter(adapter);
                        binding.toSp.setAdapter(adapter);
                        // Chọn giá trị mặc định một cách an toàn hơn
                        if (list.size() > 1) {
                            binding.fromSp.setSelection(1); // Chọn mục thứ 2 nếu có
                        } else if (!list.isEmpty()){
                            binding.fromSp.setSelection(0); // Chọn mục đầu tiên nếu chỉ có 1
                        }
                        // Tương tự cho toSp nếu muốn chọn khác
                    } else {
                        Log.w(TAG, "Location list is empty after processing snapshot.");
                        Toast.makeText(MainActivity.this, "Không có dữ liệu địa điểm.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Log.w(TAG, "Locations node does not exist in Realtime Database.");
                    Toast.makeText(MainActivity.this, "Không tìm thấy dữ liệu địa điểm.", Toast.LENGTH_SHORT).show();
                }
                binding.progressBarFrom.setVisibility(View.GONE);
                binding.progressBarTo.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Realtime Database error: " + error.getMessage());
                binding.progressBarFrom.setVisibility(View.GONE);
                binding.progressBarTo.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Lỗi tải địa điểm: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePickerDialog(TextView textView) {
        // Lấy ngày hiện tại đang hiển thị trên TextView để làm ngày mặc định cho DatePicker
        Calendar initialCalendar = Calendar.getInstance();
        try {
            initialCalendar.setTime(dateFormat.parse(textView.getText().toString()));
        } catch (Exception e) {
            // Nếu parse lỗi, dùng ngày hiện tại
            initialCalendar = Calendar.getInstance();
        }

        int year = initialCalendar.get(Calendar.YEAR);
        int month = initialCalendar.get(Calendar.MONTH);
        int day = initialCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            calendar.set(selectedYear, selectedMonth, selectedDay);
            String formattedDate = dateFormat.format(calendar.getTime());
            textView.setText(formattedDate);
            // Thêm logic kiểm tra ngày đi/ngày về nếu cần
            // validateDates();
        }, year, month, day);

        // Set giới hạn ngày tối thiểu là ngày hôm nay (tùy chọn)
        datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());

        datePickerDialog.show();
    }

    // (Tùy chọn) Thêm hàm kiểm tra ngày về phải sau ngày đi
    // private void validateDates() { ... }
}