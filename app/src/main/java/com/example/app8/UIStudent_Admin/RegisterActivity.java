package com.example.app8.UIStudent_Admin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.app8.AI_Processor.FaceClassifier;
import com.example.app8.AI_Processor.TFLiteFaceRecognition;
import com.example.app8.R;
import com.example.app8.SQLServer.SQLConnection;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


public class RegisterActivity extends AppCompatActivity {
    CardView galleryCard,cameraCard;
    ImageView imageView;
    Uri image_uri;
    private Connection connection;
    public static final int PERMISSION_CODE = 100;

    private StudentListActivity activityReference;

    //TODO declare face detector
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();
    FaceDetector detector;
    //TODO declare face recognizer
    FaceClassifier faceClassifier;

    //TODO get the image from gallery and display it
    ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        image_uri = result.getData().getData();
                        Bitmap inputImage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputImage);
                        imageView.setImageBitmap(rotated);
                        performFaceDetection(rotated);
                    }
                }
            });

    //TODO capture the image using camera and display it
    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap inputImage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputImage);
                        imageView.setImageBitmap(rotated);
                        performFaceDetection(rotated);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        connection = SQLConnection.getConnection();

        //TODO handling permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, PERMISSION_CODE);
            }
        }

        //TODO initialize views
        galleryCard = findViewById(R.id.gallerycard);
        cameraCard = findViewById(R.id.cameracard);
        imageView = findViewById(R.id.imageView2);

        //TODO code for choosing images from gallery
        galleryCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryActivityResultLauncher.launch(galleryIntent);
            }
        });

        //TODO code for capturing images using camera
        cameraCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);
                    }
                    else {
                        openCamera();
                    }
                }

                else {
                    openCamera();
                }
            }
        });

        //TODO initialize face detector
        detector = FaceDetection.getClient(highAccuracyOpts);

        //TODO initialize face recognition model
        try {
            //faceClassifier = TFLiteFaceRecognition.create(getAssets(),"facenet.tflite",160,false);
            faceClassifier = TFLiteFaceRecognition.create(getAssets(),"mobile_face_net.tflite",112,false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    //TODO opens camera so that user can capture image
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }

    //TODO takes URI of the image and returns bitmap
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

    //TODO rotate image if image captured on samsung devices
    //TODO Most phone cameras are landscape, meaning if you take the photo in portrait, the resulting photos will be rotated 90 degrees.
    @SuppressLint("Range")
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }

    //TODO perform face detection
    public void performFaceDetection(Bitmap input) {
        Bitmap mutableBmp = input.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(mutableBmp);
        InputImage image = InputImage.fromBitmap(input, 0);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        // ...
                                        Log.d("tryFace","Len = "+faces.size());
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            Paint p1 = new Paint();
                                            p1.setColor(Color.RED);
                                            p1.setStyle(Paint.Style.STROKE);
                                            p1.setStrokeWidth(5);
                                            performFaceRecognition(bounds,input);
                                            canvas.drawRect(bounds,p1);
                                        }
                                        imageView.setImageBitmap(mutableBmp);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });

    }

    //TODO perform face recognition
    public void performFaceRecognition(Rect bound, Bitmap input) {
        if(bound.left < 0) {
            bound.left = 0;
        }
        if(bound.top < 0) {
            bound.top = 0;
        }
        if(bound.right > input.getWidth()) {
            bound.right = input.getWidth() - 1;
        }
        if(bound.bottom > input.getHeight()) {
            bound.bottom = input.getHeight() - 1;
        }
        Bitmap croppedFace = Bitmap.createBitmap(input,bound.left,bound.top,bound.width(),bound.height());
        //imageView.setImageBitmap(croppedFace);
        //croppedFace = Bitmap.createScaledBitmap(croppedFace,160,160,false);
        croppedFace = Bitmap.createScaledBitmap(croppedFace,112,112,false);
        FaceClassifier.Recognition recognition = faceClassifier.recognizeImage(croppedFace, true);

        showRegisterDialogue(croppedFace, recognition);
    }
    public void showRegisterDialogue(Bitmap face, FaceClassifier.Recognition recognition) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.register_face_dialogue);
        ImageView imageView1 = dialog.findViewById(R.id.dlg_image);
        EditText editTextName = dialog.findViewById(R.id.dlg_input);
        EditText editTextCode = dialog.findViewById(R.id.editTextCode);
        EditText editTextDOB = dialog.findViewById(R.id.editTextDateOfBirth);
        Button register = dialog.findViewById(R.id.button2);
        imageView1.setImageBitmap(face);

        // Lấy ClassId từ Intent
        int classId = getIntent().getIntExtra("ClassId", -1);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(editTextName.getText().toString().equals("")) {
                    editTextName.setError("Enter name");
                } else {
                    if (connection != null) {
                        try {
                            String checkQuery = "SELECT COUNT(*) AS count FROM THAMGIA WHERE classid = ? AND code_student = ?";
                            try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
                                checkStatement.setInt(1, classId);
                                checkStatement.setString(2, editTextCode.getText().toString());
                                ResultSet checkResult = checkStatement.executeQuery();

                                // Move to the first row of the result set
                                checkResult.next();
                                int rowCount = checkResult.getInt("count");
                                if (rowCount == 0) {
                                    faceClassifier.register(editTextName.getText().toString(), editTextCode.getText().toString(), editTextDOB.getText().toString(), recognition, face);
                                    saveDatatoTHAMGIA(classId, editTextCode.getText().toString());
                                    //store this recognition in database if possible
                                    Toast.makeText(RegisterActivity.this, "Register succesfuly", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();

                                    // Trở về StudentListActivity và cập nhật lại ListView
                                    Intent intent = new Intent(RegisterActivity.this, StudentListActivity.class);
                                    intent.putExtra("ClassId", classId);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // If a matching row exists, display the error message
                                    Toast.makeText(RegisterActivity.this, "Duplicate student code", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
    public void saveDatatoTHAMGIA(int classId, String codeStudent) {
        Connection connection = SQLConnection.getConnection(); // Lấy kết nối đến cơ sở dữ liệu

        if (connection != null) {
            try {
                // Sử dụng PreparedStatement để chèn dữ liệu vào bảng THAMGIA
                String query = "INSERT INTO THAMGIA (classid, code_student) VALUES (?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, classId);
                preparedStatement.setString(2, codeStudent);

                int rowsInserted = preparedStatement.executeUpdate();
                preparedStatement.close();

                if (rowsInserted > 0) {
                    // Dòng đã được chèn thành công
                    System.out.println("Dữ liệu đã được chèn thành công vào bảng THAMGIA.");
                } else {
                    // Dòng chèn không thành công
                    System.out.println("Lỗi khi chèn dữ liệu vào bảng THAMGIA.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}