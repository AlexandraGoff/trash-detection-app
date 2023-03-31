package com.example.trash_detection_app;

public class ImageUploadRequest {
    private String encodedImage;

    public ImageUploadRequest(String encodedImage) {
        this.encodedImage = encodedImage;
    }

    public String getEncodedImage() {
        return encodedImage;
    }
}
