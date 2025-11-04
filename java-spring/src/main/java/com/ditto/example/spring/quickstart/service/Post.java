package com.ditto.example.spring.quickstart.service;

import jakarta.annotation.Nonnull;

public record Post(
        String id,
        String parent,
        String author_id,
        int time,
        String text,
        String attachment,
        int likes,
        int dislikes,
        String tags
) { }
