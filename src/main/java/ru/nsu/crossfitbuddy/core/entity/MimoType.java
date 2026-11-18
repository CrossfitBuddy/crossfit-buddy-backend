package ru.nsu.crossfitbuddy.core.entity;

public enum MimoType {

    JPEG("image/jpeg"),
    PNG("image/png"),
    MP4("video/mp4");

    private final String mimeType;

    MimoType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
