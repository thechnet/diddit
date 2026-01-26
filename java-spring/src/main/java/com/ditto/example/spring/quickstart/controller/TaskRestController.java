package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoPostService;
import com.ditto.example.spring.quickstart.service.Post;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
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

	@PostMapping("/registerAccount")
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
