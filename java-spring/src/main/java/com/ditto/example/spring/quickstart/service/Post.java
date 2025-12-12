package com.ditto.example.spring.quickstart.service;

public record Post(
        String _id,
        String parent,
        String username,
        long time,
        String text,
        String attachment,
        int likes,
        int dislikes,
        String tags
) {

    public String getUsername() {
        return username;
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
