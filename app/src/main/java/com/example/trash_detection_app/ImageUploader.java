package com.example.trash_detection_app;


import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.media.Image;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class ImageUploader extends ImageActivity{

    private ImageActivity imageActivity;

    public ImageUploader(ImageActivity imageActivity) {
        this.imageActivity = imageActivity;
    }

    private static Context context;
    private static String url="http://192.168.1.108:5000";//****Put URL here******
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
                final String responseData = response.body().string();
                Log.d("ActivityMain", "Response is : " + responseData);
                try {
                    //MainActivity.this.runOnUiThread(()-> imageView_response.setImageURI(uri));*/
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
