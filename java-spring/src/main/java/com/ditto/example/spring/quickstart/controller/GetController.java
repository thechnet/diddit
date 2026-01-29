package com.ditto.example.spring.quickstart.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GetController {

	private final Logger logger = LoggerFactory.getLogger(GetController.class);

	public GetController() {
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
	public String posts(@RequestParam(name = "parent", required = false) String parent,
		Map<String, Object> model) {
		model.put("parent", parent != null ? parent : "");
		return "posts";
	}
}
