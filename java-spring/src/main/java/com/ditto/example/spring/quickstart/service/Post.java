package com.ditto.example.spring.quickstart.service;

public record Post(
        String _id,
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
