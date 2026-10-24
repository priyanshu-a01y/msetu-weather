package com.summitsky.model;

/** Geographic image metadata; weather effects are selected separately from the condition code. */
public record LocationVisual(String name, String imageUrl, String imageAlt, String attribution,
        String sourceUrl, String sceneClass, String imagePosition) {
    public boolean hasImage() { return imageUrl != null && !imageUrl.isBlank(); }
}
