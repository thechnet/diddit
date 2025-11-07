package com.ditto.example.spring.quickstart.service;

import jakarta.annotation.Nonnull;

import java.util.UUID;

public record Post(
        String id,
        String parent,
        String author_id,
        long time,
        String text,
        String attachment,
        int likes,
        int dislikes,
        String tags
) {

    public String getAuthorId() {
        return author_id;
    }

    public String getText() {
        return text;
    }

    public String getAttachment() {
        return attachment;
    }

    public String getTags() {
        return tags;
    }

}
