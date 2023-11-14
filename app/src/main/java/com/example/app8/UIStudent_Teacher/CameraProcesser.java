package com.example.app8.UIStudent_Teacher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.app8.R;
import com.example.app8.SQLServer.SQLConnection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CameraProcesser extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG="MainActivity";
    JavaCameraView javaCameraView;
    File caseFile;
    private static final int FACENET_INPUT_IMAGE_SIZE = 112;
    CascadeClassifier faceDetector;
    private Mat mRgba, mGrey;
    private final int PERMISSIONS_READ_CAMERA=1;
    private Button addFaceButton;
    private EditText faceNameEditText;
    private Mat lastDetectedFace; // Thêm biến này để lưu trữ khuôn mặt đã phát hiện
    private Interpreter faceNetModelInterpreter;
    private static final float DISTANCE_THRESHOLD = 1.0f; // Điều chỉnh ngưỡng tùy theo trường hợp.
    private boolean isSavingFace = false;
    private int cameraid ;
    List<FaceData> faceList = new ArrayList<>();
    private int mFaceSize = 112; // Đặt kích thước khuôn mặt theo ý muốn

    private Connection sqlConnection2;  // Kết nối SQL Server
    private static CameraProcesser cameraProcesser;

    public static int classID;

    public class FaceData {
        private String name;
        private float[] faceEmbeddings;
        private Bitmap faceBitmap;

        public FaceData(String name, float[] faceEmbeddings) {
            this.name = name;
            this.faceEmbeddings = faceEmbeddings;
        }

        public String getName() {
            return name;
        }

        public float[] getFaceEmbeddings() {
            return faceEmbeddings;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_processer);

        javaCameraView = (JavaCameraView)findViewById(R.id.javaCamView);
        addFaceButton = findViewById(R.id.addFaceButton);
        faceNameEditText = new EditText(this);
        Button switchCameraButton = findViewById(R.id.switchCameraButton);

        int classId = getIntent().getIntExtra("classId", -1);classID = classId;
        sqlConnection2 = SQLConnection.getConnection();
        // Load recognised faces from the database
        loadRecognisedFacesFromDatabase();

        cameraProcesser = this; // Thêm dòng này

        // Khởi tạo FaceNet với đường dẫn đến model
        try {
            faceNetModelInterpreter = new Interpreter(loadModelFile("mobile_face_net.tflite"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        } else {
            switchCameraButton.setVisibility(View.GONE);
        }

        addFaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSavingFace = true;
                showAddFaceDialog();
            }
        });
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cameraid == 0){
                    cameraid = 1;
                    javaCameraView.setCameraIndex(cameraid);
                } else {
                    cameraid = 0;
                    javaCameraView.setCameraIndex(cameraid);
                }
                javaCameraView.disableView();
                javaCameraView.enableView();
            }
        });
        javaCameraView.setCameraIndex(cameraid = 1);

        if(!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallBack);
        }
        else
        {
            try {
                baseCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_READ_CAMERA);
            }
        } else {
            Log.d("LOADED", "PERMISSIOns granted");
            javaCameraView.setCameraPermissionGranted();
        }

        javaCameraView.setCvCameraViewListener(this);


    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Show the "Switch Camera" button in landscape mode
            Button switchCameraButton = findViewById(R.id.switchCameraButton);
            switchCameraButton.setVisibility(View.VISIBLE);
            cameraid = 0;
            javaCameraView.setCameraIndex(cameraid);
            javaCameraView.disableView();
            javaCameraView.enableView();
        } else {
            // Hide the "Switch Camera" button in portrait mode
            Button switchCameraButton = findViewById(R.id.switchCameraButton);
            switchCameraButton.setVisibility(View.GONE);
            // Always set the camera to the front camera in portrait mode
            cameraid = 1;
            javaCameraView.setCameraIndex(cameraid);
            javaCameraView.disableView();
            javaCameraView.enableView();
        }
    }

    private void showAddFaceDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Add Face");
        EditText faceNameEditText = new EditText(this);

        if (lastDetectedFace != null) {
            // Tạo một ImageView mới để hiển thị hình ảnh khuôn mặt
            ImageView faceImageView = new ImageView(this);
            faceImageView.setImageBitmap(matToBitmap(lastDetectedFace));

            // Tạo một layout để chứa ImageView và EditText
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            // Kiểm tra và loại bỏ các phần tử cũ trước khi thêm mới
            if (layout.getChildCount() > 0) {
                layout.removeAllViews();
            }
            layout.addView(faceImageView); // Thêm ImageView vào layout
            layout.addView(faceNameEditText); // Thêm EditText vào layout

            dialogBuilder.setView(layout); // Sét layout làm nội dung của AlertDialog
        } else {
            dialogBuilder.setView(faceNameEditText);
        }

        dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String faceName = faceNameEditText.getText().toString();
                float[] faceEmbeddings = getFaceEmbeddings(lastDetectedFace);
                saveFaceInfo(faceName, faceEmbeddings);
                isSavingFace = false; // Khi nhấn Save, đặt lại trạng thái
                savePersonToDatabase(faceName,faceEmbeddings,convertImageToBytes(matToBitmap(lastDetectedFace)));
                dialog.dismiss();
            }
        });

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isSavingFace = false; // Khi nhấn Cancel, đặt lại trạng thái
                dialog.dismiss();
            }
        });

        AlertDialog addFaceDialog = dialogBuilder.create();
        addFaceDialog.show();
        isSavingFace = true; // Khi mở dialog, đặt trạng thái là true

    }

    private float[] getFaceEmbeddings(Mat faceImage) {
        if (faceImage != null) {
            // Chuyển đổi Mat thành Bitmap
            Bitmap faceBitmap = matToBitmap(faceImage);

            // Tạo TensorImage từ Bitmap
            TensorImage tensorImage = TensorImage.fromBitmap(faceBitmap);

            // Xử lý ảnh để chuẩn bị cho FaceNet
            ImageProcessor faceNetImageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(FACENET_INPUT_IMAGE_SIZE, FACENET_INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(0f, 255f))
                    .build();
            tensorImage = faceNetImageProcessor.process(tensorImage);

            // Chuyển đổi TensorImage thành ByteBuffer
            ByteBuffer faceNetByteBuffer = tensorImage.getBuffer();

            // Tạo mảng để lưu trữ kết quả embeddings
            float[][] faceOutputArray = new float[1][192];

            // Sử dụng faceNetModelInterpreter để trích xuất embeddings
            faceNetModelInterpreter.run(faceNetByteBuffer, faceOutputArray);

            return faceOutputArray[0];
        }
        return null;
    }


    private Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    private void saveFaceInfo(String faceName, float[] faceEmbeddings) {
        FaceData faceData = new FaceData(faceName, faceEmbeddings);
        faceList.add(faceData);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mGrey = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGrey.release();
    }
    // Hàm tải mô hình TensorFlow Lite từ tệp tin
    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }



    // Cắt ra khuôn mặt từ khung hình và lưu nó trong biến lastDetectedFace
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (!isSavingFace) {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                mRgba = inputFrame.rgba();
                mGrey = inputFrame.gray();

           /* MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(mRgba, faceDetections);
            for (Rect rect : faceDetections.toArray()) {
                Imgproc.rectangle(mRgba, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(255, 0, 0));*/
                int height = mGrey.rows();
                if (Math.round(height * 0.2) > 0) {
                    mFaceSize = (int) Math.round(height * 0.2);
                }
                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(mRgba, faceDetections);
                for (Rect rect : faceDetections.toArray()) {
                    Imgproc.rectangle(mRgba, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height),
                            new Scalar(255, 0, 0));
                    // Cắt ra hình ảnh khuôn mặt từ mRgba và lưu nó trong lastDetectedFace
                    lastDetectedFace = mRgba.submat(rect);
                    if (lastDetectedFace != null) {
                        float[] faceEmbeddings = getFaceEmbeddings(lastDetectedFace);
                        FaceData nearestFace = findNearestFace(faceEmbeddings);
                        if (nearestFace != null) {
                            String nearestFaceName = nearestFace.getName();
                            savePersonToAttendaceList(nearestFaceName);
                            Imgproc.putText(mRgba, nearestFaceName, new Point(rect.x, rect.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0), 2);
                        }
                    }
                }

            } else {
                mRgba = inputFrame.rgba();
                mGrey = inputFrame.gray();

                Mat rotImage = Imgproc.getRotationMatrix2D(new Point(mRgba.cols() / 2,
                        mRgba.rows() / 2), 90, 1.0);

                Imgproc.warpAffine(mRgba, mRgba, rotImage, mRgba.size());
                Imgproc.warpAffine(mGrey, mGrey, rotImage, mRgba.size());

                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(mRgba, faceDetections);
                for (Rect rect : faceDetections.toArray()) {
                    Imgproc.rectangle(mRgba, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height),
                            new Scalar(255, 0, 0));
                    // Cắt ra hình ảnh khuôn mặt từ mRgba và lưu nó trong lastDetectedFace
                    lastDetectedFace = mRgba.submat(rect);
                    if (lastDetectedFace != null) {
                        float[] faceEmbeddings = getFaceEmbeddings(lastDetectedFace);
                        FaceData nearestFace = findNearestFace(faceEmbeddings);
                        if (nearestFace != null) {
                            String nearestFaceName = nearestFace.getName();
                            savePersonToAttendaceList(nearestFaceName);
                            Imgproc.putText(mRgba, nearestFaceName, new Point(rect.x, rect.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0), 2);
                        }
                    }
                }

            }
        }

        return mRgba;
    }

    private BaseLoaderCallback baseCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    caseFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
                    FileOutputStream fos = new FileOutputStream(caseFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while((bytesRead = is.read(buffer))!=-1)
                    {
                        fos.write(buffer, 0, bytesRead);
                    }

                    is.close();
                    fos.close();

                    faceDetector = new CascadeClassifier(caseFile.getAbsolutePath());

                    if(faceDetector.empty())
                    {
                        faceDetector = null;
                    }
                    else
                        cascadeDir.delete();

                    javaCameraView.enableView();

                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // Ensure that this result is for the camera permission request
        if (requestCode == PERMISSIONS_READ_CAMERA) {
            // Check if the request was granted or denied
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The request was granted -> tell the camera view
                javaCameraView.setCameraPermissionGranted();
            } else {
                // The request was denied -> tell the user and exit the application
                Toast.makeText(this, "Camera permission required.",
                        Toast.LENGTH_LONG).show();
                this.finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private FaceData findNearestFace(float[] vector) {
        FaceData nearestFace = null;
        float minDistance = Float.MAX_VALUE;

        for (FaceData faceData : faceList) {
            float[] knownVector = faceData.getFaceEmbeddings();

            float distance = calculateDistance(vector, knownVector);

            if (distance < minDistance && distance < DISTANCE_THRESHOLD) {
                minDistance = distance;
                nearestFace = faceData;
            }
        }

        return nearestFace;
    }

    private float calculateDistance(float[] vector1, float[] vector2) {
        float distance = 0;
        for (int i = 0; i < vector1.length; i++) {
            float diff = vector1[i] - vector2[i];
            distance += diff * diff;
        }
        return (float) Math.sqrt(distance);
    }

    // Code interface with SQL Server
    private void savePersonToDatabase(final String name, final float[] faceVector, final byte[] imageBytes) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if (sqlConnection2 != null) {
                    try {
                        String insertQuery = "INSERT INTO Person (Name, FaceVector, ImageData) VALUES (?, ?, ?)";
                        PreparedStatement preparedStatement = sqlConnection2.prepareStatement(insertQuery);
                        preparedStatement.setString(1, name);
                        byte[] faceVectorBytes = convertFloatArrayToBytes(faceVector);
                        preparedStatement.setBytes(2, faceVectorBytes);
                        preparedStatement.setBytes(3, imageBytes);
                        preparedStatement.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };

        task.execute();
    }
    private void savePersonToAttendaceList(final String name) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if (sqlConnection2 != null) {
                    try {
                        // Trước hết, kiểm tra xem `name` đã tồn tại trong bảng `STUDENT_LIST` chưa và lấy thông tin cần thiết
                        String selectQuery = "SELECT code_student, date_of_birth FROM STUDENT_LIST WHERE name_student = ?";
                        PreparedStatement selectStatement = sqlConnection2.prepareStatement(selectQuery);
                        selectStatement.setString(1, name);
                        ResultSet resultSet = selectStatement.executeQuery();

                        if (resultSet.next()) {
                            String codeStudent = resultSet.getString("code_student");
                            String dateOfBirth = resultSet.getString("date_of_birth");

                            // Tiếp theo, kiểm tra xem `name` đã tồn tại trong bảng `ATTENDANCE` chưa
                            String checkQuery = "SELECT COUNT(*) FROM ATTENDANCE WHERE name_student = ?";
                            PreparedStatement checkStatement = sqlConnection2.prepareStatement(checkQuery);
                            checkStatement.setString(1, name);
                            ResultSet checkResultSet = checkStatement.executeQuery();

                            checkResultSet.next();
                            int count = checkResultSet.getInt(1);

                            if (count == 0) {
                                // `name` chưa tồn tại, thực hiện thêm vào bảng
                                String insertQuery = "INSERT INTO ATTENDANCE (name_student, code_student, date_of_birth, attendance_date, classId) VALUES (?, ?, ?, ?, ?)";
                                PreparedStatement preparedStatement = sqlConnection2.prepareStatement(insertQuery);
                                preparedStatement.setString(1, name);
                                preparedStatement.setString(2, codeStudent);
                                preparedStatement.setString(3, dateOfBirth);
                                // Chuyển đổi ngày giờ hiện tại thành kiểu dữ liệu DATE
                                Date currentDate = new Date(Calendar.getInstance().getTime().getTime());

                                preparedStatement.setDate(4, currentDate); // Sử dụng setDate để thêm ngày giờ
                                preparedStatement.setInt(5, classID);
                                preparedStatement.executeUpdate();
                            } else {
                                // `name` đã tồn tại, có thể thực hiện xử lý khác hoặc báo lỗi nếu cần.
                            }

                            checkResultSet.close();
                            checkStatement.close();
                        }

                        resultSet.close();
                        selectStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };
        task.execute();
    }

    private byte[] convertImageToBytes(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
    private byte[] convertFloatArrayToBytes(float[] floatArray) {
        ByteBuffer buffer = ByteBuffer.allocate(floatArray.length * 4);
        for (float value : floatArray) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }
    private float[] convertBytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] floatArray = new float[bytes.length / 4];
        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = buffer.getFloat();
        }
        return floatArray;
    }
    private void loadRecognisedFacesFromDatabase() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (sqlConnection2 != null) {
                    try {
                        String selectQuery = "SELECT name_student, FaceVector FROM STUDENT_LIST";
                        PreparedStatement preparedStatement = sqlConnection2.prepareStatement(selectQuery);
                        ResultSet resultSet = preparedStatement.executeQuery();

                        while (resultSet.next()) {
                            String name = resultSet.getString("name_student");
                            byte[] faceVectorBytes = resultSet.getBytes("FaceVector");
                            float[] faceEmbeddings = convertBytesToFloatArray(faceVectorBytes);
                            faceList.add(new FaceData(name, faceEmbeddings));
                        }

                        resultSet.close();
                        preparedStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

}