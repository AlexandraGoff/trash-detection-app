package com.example.trash_detection_app;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageUploader {

    private static Context context;
    private static String url="http://192.168.1.108:5000";//****Put URL here******

    //Upload image to the REST API.
    public static void uploadImage(Context context, File imageFile) throws IOException{
        try {
            String b64EncodedString = encodeToBase64(context, imageFile);
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
    public static String encodeToBase64(Context context, File filename) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(String.valueOf(filename));
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        String encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);

        return encodedString;
    }

    // Save the decoded image as a temporary file in the cache directory.
    public static File saveDecodedImage(final Context context, final String imageData) {
        try {
            final byte[] imgBytesData = android.util.Base64.decode(imageData,
                    android.util.Base64.DEFAULT);

            final File file = File.createTempFile("image", null, context.getCacheDir());
            final FileOutputStream fileOutputStream;
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }

            final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                    fileOutputStream);
            try {
                bufferedOutputStream.write(imgBytesData);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decodeFromBase64(String imageDataString) {
        return Base64.decode(imageDataString, Base64.DEFAULT);
    }

    public static void sendRequest(String param, ImageUploadRequest imageData) {
        String fullURL=url+"/"+(param==null?"":"/"+param);

        // Create JSON object with encoded image
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("data", imageData.getEncodedImage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Create OkHttp client and request
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(fullURL)
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .build();

        // Send request asynchronously
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
                /*Uri uri = Uri.fromFile(saveDecodedImage(getApplicationContext(), responseData));
                MainActivity.this.runOnUiThread(()-> imageView_response.setImageURI(uri));*/
            }
        });
    }
}
