package com.ditto.example.spring.quickstart.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SiteController {

	private final Logger logger = LoggerFactory.getLogger(SiteController.class);

	public SiteController() {
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
	public String drilldown(@RequestParam(name = "parent", required = false) String parent,
		Map<String, Object> model) {
		model.put("parent", parent != null ? parent : "");
		return "drilldown";
	}
}
