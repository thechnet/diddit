package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoPostService;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class TaskContentController {
    @Nonnull
    private final DittoPostService taskService;

	private final Logger logger = LoggerFactory.getLogger(TaskContentController.class);

    public TaskContentController(@NotNull final DittoPostService taskService) {
        this.taskService = taskService;
    }

	@GetMapping("/")
	public String login(Map<String, Object> model) {
		return "login";
	}

	@GetMapping("/register")
	public String register(Map<String, Object> model) {
		return "register";
	}

	@GetMapping("/posts")
	public String drilldown(@RequestParam(name = "parent", required = false) String parent, Map<String, Object> model) {
		model.put("parent", parent != null ? parent : "");
		return "drilldown";
	}
}
