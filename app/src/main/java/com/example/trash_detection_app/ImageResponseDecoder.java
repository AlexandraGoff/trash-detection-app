package com.example.trash_detection_app;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageResponseDecoder {


    // Save the decoded response image as a temporary file in the cache directory.
    public static Uri decodeResponseImage(final Context context, final String imageData) {
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
            Uri uri = Uri.fromFile(file);
            Log.d("Uri Message :", "Decoded response image! The uri is : " + uri);
            return uri;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
