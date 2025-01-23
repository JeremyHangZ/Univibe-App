package com.example.comp90018_a2;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DetailedInfoActivity extends AppCompatActivity {
    private EditText nameEditText, ageEditText, birthdayEditText, departmentEditText, majorEditText,
            degreeEditText;
    private Spinner genderSpinner;
    private Button submitDetailsButton, skipButton, uploadAvatarButton;
    private ImageView avatarImageView, photo1ImageView, photo2ImageView, photo3ImageView;
    private ProgressBar uploadProgressBar;  // 进度条
    private DatabaseReference userRef;
    private StorageReference storageRef;
    private String userId;

    // 临时保存图片的URL
    private String avatarUrl = null;

    private String[] photoUrls = new String[3];

    // ActivityResultLaunchers for handling the new image selection
    private ActivityResultLauncher<Intent> avatarLauncher, photo1Launcher, photo2Launcher, photo3Launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_info);

        nameEditText = findViewById(R.id.editTextName);
        genderSpinner = findViewById(R.id.spinnerGender);
        ageEditText = findViewById(R.id.editTextAge);
        birthdayEditText = findViewById(R.id.editTextBirthday);
        departmentEditText = findViewById(R.id.editTextDepartment);
        majorEditText = findViewById(R.id.editTextMajor);
        degreeEditText = findViewById(R.id.editTextDegree);
        submitDetailsButton = findViewById(R.id.buttonSubmitDetails);
        skipButton = findViewById(R.id.buttonSkip);
        uploadAvatarButton = findViewById(R.id.buttonUploadAvatar);
        avatarImageView = findViewById(R.id.imageViewAvatar);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);  // 进度条
        photo1ImageView = findViewById(R.id.imageView_photo_1);
        photo2ImageView = findViewById(R.id.imageView_photo_2);
        photo3ImageView = findViewById(R.id.imageView_photo_3);
        uploadProgressBar = findViewById(R.id.uploadPhotoProgressBar);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        storageRef = FirebaseStorage.getInstance().getReference();

        loadUserData();

        submitDetailsButton.setOnClickListener(v -> submitDetails());
        skipButton.setOnClickListener(v -> skipDetails());

        // 初始化 ActivityResultLaunchers
        initializeActivityResultLaunchers();

        // 设置按钮点击事件来启动图片选择器
        uploadAvatarButton.setOnClickListener(v -> launchImagePicker(avatarLauncher));
        photo1ImageView.setOnClickListener(v -> launchImagePicker(photo1Launcher));
        photo2ImageView.setOnClickListener(v -> launchImagePicker(photo2Launcher));
        photo3ImageView.setOnClickListener(v -> launchImagePicker(photo3Launcher));


        // 设置点击事件，弹出日期选择器
        birthdayEditText.setOnClickListener(v -> {
            // 获取当前日期
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // 创建日期选择器
            DatePickerDialog datePickerDialog = new DatePickerDialog(DetailedInfoActivity.this, (view, selectedYear, selectedMonth, selectedDay) -> {
                // 设置选择的日期到 EditText 中
                birthdayEditText.setText(selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay);
            }, year, month, day);
            datePickerDialog.show();
        });

        // 设置性别 Spinner 的适配器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // 显示返回键
            actionBar.setDisplayShowHomeEnabled(true); // 可选：显示图标
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed(); // 处理返回按键
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 初始化 ActivityResultLaunchers 用于选择图片
    private void initializeActivityResultLaunchers() {
        avatarLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                uploadImageToFirebase(imageUri);
            }
        });

        photo1Launcher = createImageLauncher(imageUri -> uploadImageToFirebase(imageUri, "photo1", photo1ImageView));
        photo2Launcher = createImageLauncher(imageUri -> uploadImageToFirebase(imageUri, "photo2", photo2ImageView));
        photo3Launcher = createImageLauncher(imageUri -> uploadImageToFirebase(imageUri, "photo3", photo3ImageView));
    }

    // 启动图片选择器
    private void launchImagePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        launcher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private ActivityResultLauncher<Intent> createImageLauncher(OnImagePickedCallback callback) {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                callback.onImagePicked(imageUri);
            }
        });
    }

    // 上传图片到 Firebase Storage，并显示上传进度
    private void uploadImageToFirebase(Uri imageUri) {
        StorageReference fileRef = storageRef.child("users/" + userId + "/avatar.jpg");
        UploadTask uploadTask = fileRef.putFile(imageUri);

        // 显示进度条
        uploadProgressBar.setVisibility(ProgressBar.VISIBLE);
        uploadProgressBar.setIndeterminate(false);

        // 监听上传进度
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
            uploadProgressBar.setProgress((int) progress);  // 更新进度条
        }).addOnSuccessListener(taskSnapshot -> {
            // 上传成功，获取下载 URL
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                avatarUrl = uri.toString();
                // 使用 Picasso 加载并显示图片到 ImageView
                Picasso.get().load(avatarUrl).into(avatarImageView);

                // 隐藏进度条
                uploadProgressBar.setVisibility(ProgressBar.GONE);

                Toast.makeText(DetailedInfoActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            // 隐藏进度条并显示错误消息
            uploadProgressBar.setVisibility(ProgressBar.GONE);
            Toast.makeText(DetailedInfoActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
        });
    }


    private void uploadImageToFirebase(Uri imageUri, String type, ImageView imageView) {
        StorageReference fileRef = storageRef.child("users/" + userId + "/" + type + ".jpg");
        UploadTask uploadTask = fileRef.putFile(imageUri);

        // 显示进度条
        uploadProgressBar.setVisibility(ProgressBar.VISIBLE);
        uploadProgressBar.setIndeterminate(false);

        // 监听上传进度
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
            uploadProgressBar.setProgress((int) progress);
        }).addOnSuccessListener(taskSnapshot -> {
            // 上传成功，获取下载 URL
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String photoUrl = uri.toString();
                if (type.equals("photo1")) {
                    photoUrls[0] = photoUrl;
                } else if (type.equals("photo2")) {
                    photoUrls[1] = photoUrl;
                } else if (type.equals("photo3")) {
                    photoUrls[2] = photoUrl;
                }
                // 使用 Picasso 加载并显示图片
                Picasso.get().load(photoUrl).into(imageView);
                uploadProgressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(DetailedInfoActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            uploadProgressBar.setVisibility(ProgressBar.GONE);
            Toast.makeText(DetailedInfoActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
        });
    }

    // 提交用户信息和图片链接
    private void submitDetails() {
        String name = nameEditText.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String age = ageEditText.getText().toString().trim();
        String birthday = birthdayEditText.getText().toString().trim();
        String department = departmentEditText.getText().toString().trim();
        String major = majorEditText.getText().toString().trim();
        String degree = degreeEditText.getText().toString().trim();

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("name", name.isEmpty() ? null : name);
        userDetails.put("gender", gender.isEmpty() ? null : gender);
        userDetails.put("age", age.isEmpty() ? null : Integer.parseInt(age));
        userDetails.put("birthday", birthday.isEmpty() ? null : birthday);
        userDetails.put("department", department.isEmpty() ? null : department);
        userDetails.put("major", major.isEmpty() ? null : major);
        userDetails.put("degree", degree.isEmpty() ? null : degree);
        userDetails.put("avatar", avatarUrl);

        Map<String, Object> photos = new HashMap<>();
        for (int i = 0; i < photoUrls.length; i++) {
            if (photoUrls[i] != null) {
                photos.put("photo" + (i + 1), photoUrls[i]);
            }
        }
        userDetails.put("photos", photos);

        userRef.updateChildren(userDetails).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(DetailedInfoActivity.this, "Details submitted successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DetailedInfoActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(DetailedInfoActivity.this, "Failed to submit details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 跳过详细信息填写，直接进入主界面
    private void skipDetails() {
        startActivity(new Intent(DetailedInfoActivity.this, MainActivity.class));
        finish();
    }

    private interface OnImagePickedCallback {
        void onImagePicked(Uri imageUri);
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // 获取并显示姓名
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null) {
                        nameEditText.setText(name);
                    }

                    // 获取并显示性别
                    String gender = snapshot.child("gender").getValue(String.class);
                    if (gender != null) {
                        int genderIndex = ((ArrayAdapter) genderSpinner.getAdapter()).getPosition(gender);
                        genderSpinner.setSelection(genderIndex);
                    }

                    // 获取并显示年龄（处理为 Long 类型）
                    Object ageValue = snapshot.child("age").getValue();
                    if (ageValue != null) {
                        if (ageValue instanceof Long) {
                            ageEditText.setText(String.valueOf(ageValue));
                        } else if (ageValue instanceof String) {
                            ageEditText.setText((String) ageValue);
                        }
                    }

                    // 获取并显示生日
                    String birthday = snapshot.child("birthday").getValue(String.class);
                    if (birthday != null) {
                        birthdayEditText.setText(birthday);
                    }

                    // 获取并显示院系
                    String department = snapshot.child("department").getValue(String.class);
                    if (department != null) {
                        departmentEditText.setText(department);
                    }

                    // 获取并显示专业
                    String major = snapshot.child("major").getValue(String.class);
                    if (major != null) {
                        majorEditText.setText(major);
                    }

                    // 获取并显示学位
                    String degree = snapshot.child("degree").getValue(String.class);
                    if (degree != null) {
                        degreeEditText.setText(degree);
                    }

                    // 获取并显示头像
                    String avatarUrl = snapshot.child("avatar").getValue(String.class);
                    if (avatarUrl != null) {
                        Picasso.get().load(avatarUrl).into(avatarImageView);
                        DetailedInfoActivity.this.avatarUrl = avatarUrl;
                    }

                    for (int i = 0; i < photoUrls.length; i++) {
                        String photoKey = "photo" + (i + 1);
                        String photoUrl = snapshot.child("photos").child(photoKey).getValue(String.class);
                        if (photoUrl != null) {
                            photoUrls[i] = photoUrl; // 保存URL
                            ImageView imageView = i == 0 ? photo1ImageView : (i == 1 ? photo2ImageView : photo3ImageView);
                            Picasso.get().load(photoUrl).into(imageView);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DetailedInfoActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}