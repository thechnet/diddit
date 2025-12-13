package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoPostService;
import com.ditto.example.spring.quickstart.service.Post;
import jakarta.annotation.Nonnull;
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

    public TaskRestController(final DittoPostService taskService, final SpringTemplateEngine templateEngine) {
        this.taskService = taskService;
        this.templateEngine = templateEngine;
    }

    @GetMapping(value = "/tasks/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamTasks() {
        return taskService.observeAll().map(tasks -> {
            String htmlFragment = renderTodoList(tasks);
            return ServerSentEvent.builder(htmlFragment)
                    .event("task_list")
                    .build();
        });
    }

    @PostMapping("/tasks")
    public String addTask(@RequestParam("title") @Nonnull String title, @RequestParam("username") @Nonnull String username, @RequestParam("password") @Nonnull String password) {
        taskService.addPost(title, username, password);
        return "";
    }

	@PostMapping("/registerAccount")
	public String registerAccount(@RequestParam("username") @Nonnull String username, @RequestParam("password") @Nonnull String password) {
		taskService.registerAccount(username, password);
		return "";
	}

    @PostMapping("/tasks/reply")
    public String postReply(@RequestParam("parentId") String parentId,
                            @RequestParam("author_id") String authorId,
                            @RequestParam("text") String text,
                            Model model) {
//        taskService.addReply(parentId, authorId, text);
//        Post reply = new Post(
//                UUID.randomUUID().toString(),
//                parentId,
//                authorId,
//                (int) (System.currentTimeMillis() / 1000),
//                text,
//                "",
//                0,
//                0,
//                ""
//        );
//        model.addAttribute("reply", reply);
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


    // @DeleteMapping("/tasks/{taskId}")
    // public String deleteTask(@PathVariable @Nonnull String taskId) {
    //     taskService.deleteTask(taskId);
    //     return "";
    // }

    @Nonnull
    private String renderTodoList(@Nonnull List<Post> tasks) {
        Context context = new Context();
        context.setVariable("tasks", tasks);
        return templateEngine.process("fragments/taskList", Set.of("taskListFrag"), context);
    }
}
