package com.example.trash_detection_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity {
    ImageActivity imageActivity;
    private PreviewView previewView;

    private static String url="http://192.168.1.108:5000";//****Put URL here******
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    //private TextView textView;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        //textView = findViewById(R.id.orientation);
        Button captureImg = findViewById(R.id.captureImg);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

        captureImg.setOnClickListener(v -> {
            File file = null;
            try {
                file = File.createTempFile("image", null, this.getCacheDir());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(file).build();

            File finalFile = file;
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            try {
                                uploadImage(getApplicationContext(), finalFile);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException error) {
                            error.getImageCaptureError();
                        }
                    });
        });

    }


    //Upload image to the REST API.
    public void uploadImage(Context context, File file) throws IOException{
        try {
            String b64EncodedString = encodeToBase64(context, file);
            ImageUploadRequest imageData = new ImageUploadRequest(b64EncodedString);
            if(b64EncodedString.isEmpty()){
                Log.d("Image Uploader :", "Encoded String cannot be empty!");
            } else {
                sendRequest("getimage", imageData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Encode the file into Base64 to send it over to the Flask API
    public static String encodeToBase64(Context context, File file) throws IOException {
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public void sendRequest(String param, ImageUploadRequest imageData) {
        String fullURL=url+"/"+(param==null?"":"/"+param);

        // Create JSON object with encoded image
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("data", imageData.getEncodedImage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Create OkHttp client and request
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        ;
        Request request = new Request.Builder()
                .url(fullURL)
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .build();

        // Send the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Read data on the worker thread
                String responseData = response.body().string();
                Log.d("ActivityMain", "Response is : " + responseData);
                try {
                    closeCamera(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void closeCamera(String responseData) {
        Intent closeIntent = new Intent(this, ImageActivity.class);
        closeIntent.putExtra("response", responseData);
        startActivity(closeIntent);
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                image.close();
            }
        });
       /* OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();*/
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview, imageCapture);
    }
}
