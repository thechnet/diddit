package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoPostService;
import com.ditto.example.spring.quickstart.service.Post;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Flux;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
public class TaskRestController {
    @Nonnull
    private final DittoPostService taskService;
    @Nonnull
    private final SpringTemplateEngine templateEngine;

    private final Logger logger = LoggerFactory.getLogger(TaskRestController.class);

    public TaskRestController(final DittoPostService taskService, final SpringTemplateEngine templateEngine) {
        this.taskService = taskService;
        this.templateEngine = templateEngine;
    }

    @GetMapping(value = "/posts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamPosts(@RequestParam(name = "parent", required = false) String parent) {
        return taskService.observeAll().map(tasks -> {
            String htmlFragment = renderPostList(tasks, parent);
            return ServerSentEvent.builder(htmlFragment)
                .event("post_list")
                .build();
        });
    }

    @PostMapping("/tasks")
    public String addTask(
        @RequestParam("title") @Nonnull String title,
        @RequestParam("username") @Nonnull String username,
        @RequestParam("password") @Nonnull String password,
        @RequestParam("parent") String parent,
        @RequestParam(value = "file", required = false) MultipartFile file,
        Model model
    ) {
        String attachmentBase64 = "";
        if (file != null && !file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                // Create the standard Data URI string
                attachmentBase64 = "data:" + file.getContentType() + ";base64," + base64;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        taskService.addReply(parent, title, username, password, attachmentBase64);
        return "";
    }

	@PostMapping("/registerAccount")
    @ResponseBody
	public String registerAccount(@RequestParam("username") @Nonnull String username, @RequestParam("password") @Nonnull String password) {
		taskService.registerAccount(username, password);
		return "";
	}

    @PostMapping("/tasks/reply")
    public String postReply(@RequestParam("username") @Nonnull String username, @RequestParam("password") @Nonnull String password, @RequestParam("text") @Nonnull String text, @RequestParam("_id") @Nonnull String _id, Model model) {
        taskService.addReply(_id, text, username, password, "");
        Post reply = new Post(
                UUID.randomUUID().toString(),
                _id,
                username,
                (int) (System.currentTimeMillis() / 1000),
                text,
                "",
                0,
                0,
                ""
        );
        model.addAttribute("reply", reply);
        return "";
    }

    @PostMapping("/tasks/like")
    @ResponseBody
    public String likePost(@RequestParam("id") String id,
                           @RequestParam("likes") int likes) {
        int newLikes = taskService.likePost(id, likes);
        return String.valueOf(newLikes);
    }

    @PostMapping("/tasks/dislike")
    @ResponseBody
    public String dislikePost(@RequestParam("id") String id,
                              @RequestParam("dislikes") int dislikes) {
        int newDislikes = taskService.dislikePost(id, dislikes);
        return String.valueOf(newDislikes);
    }

     @PostMapping("/tasks/delete")
     @ResponseBody
     public String deleteTask(@RequestParam("username") @Nonnull String username, @RequestParam("password") @Nonnull String password, @RequestParam("_id") @Nonnull String post_id) {
         taskService.deleteTask(username, password, post_id);
         return "";
     }

    @GetMapping("/tasks/filter")
    public String filterTasks(@RequestParam(required = false) String filter,
                              Model model) {

        if (filter == null || filter.isEmpty()) {
            return ""; // Leer wenn SSE aktiv ist
        }
        List<Post> tasks = taskService.getTasksFiltered(filter);

        Context context = new Context();
        context.setVariable("tasks", tasks);
        logger.info("Found {} tasks", tasks.size());
        context.setVariable("parent", "");
        // Manually render and return the HTML
        return templateEngine.process("fragments/postList", Set.of("taskListContent"), context);
    }

    @Nonnull
    private String renderPostList(@Nonnull List<Post> tasks, String parent_id) {
        Post parent = null;
        for (var task : tasks) {
            if (task._id().equals(parent_id)) {
                parent = task;
                break;
            }
        }
        Context context = new Context();
        context.setVariable("tasks", tasks);
        context.setVariable("parent", parent);
        context.setVariable("parent_id", parent_id);
        return templateEngine.process("fragments/postList", Set.of("taskListFrag"), context);
    }
}
