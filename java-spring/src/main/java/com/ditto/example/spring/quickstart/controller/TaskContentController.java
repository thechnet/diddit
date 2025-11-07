package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoPostService;
import com.ditto.example.spring.quickstart.service.Post;
import com.ditto.example.spring.quickstart.service.Task;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class TaskContentController {
    @Nonnull
    private final DittoPostService taskService;

    public TaskContentController(@NotNull final DittoPostService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/")
    public String index(Map<String, Object> model) {
        List<Post> tasks = taskService.observeAll().blockFirst();
        model.put("tasks",  tasks != null ? tasks : Collections.emptyList());
        return "index";
    }

    @GetMapping("/tasks/{id}/edit-form")
    public String editForm(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        return "fragments/editForm :: editFormFrag";
    }
    @GetMapping("/tasks/replyForm")
    public String getReplyForm(@RequestParam String id, Model model) {
        System.out.println("=== REPLY FORM REQUESTED ===");
        System.out.println("ID: " + id);
        System.out.println("=============================");
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID parameter is required");
        }
        model.addAttribute("id", id);
        return "fragments/replyForm :: replyFormFrag";

    }
}
