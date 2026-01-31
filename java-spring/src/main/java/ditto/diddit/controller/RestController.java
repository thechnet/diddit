package ditto.diddit.controller;

import ditto.diddit.Post;
import ditto.diddit.service.DidditService;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Flux;

@org.springframework.web.bind.annotation.RestController
public class RestController {

	@Nonnull
	private final DidditService didditService;
	@Nonnull
	private final SpringTemplateEngine templateEngine;

	private final Logger logger = LoggerFactory.getLogger(RestController.class);

	public RestController(@NotNull final DidditService didditService,
		@NotNull final SpringTemplateEngine templateEngine) {
		this.didditService = didditService;
		this.templateEngine = templateEngine;
	}

	@GetMapping(value = "/posts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamPosts(
		@RequestParam(name = "parent", required = false) String parent) {
		return didditService.observeAll().map(posts -> {
			String htmlFragment = renderPostList(posts, parent);
			return ServerSentEvent.builder(htmlFragment)
				.event("post_list")
				.build();
		});
	}

	@PostMapping("/posts/new")
	public String addPost(
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
				String originalName = file.getOriginalFilename();
				if (originalName == null || originalName.isEmpty()) {
					originalName = "file";
				}
				attachmentBase64 =
					originalName + ":::" + "data:" + file.getContentType() + ";base64," + base64;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		didditService.addReply(parent, title, username, password, attachmentBase64);
		return "";
	}

	@PostMapping("/registerAccount")
	@ResponseBody
	public String registerAccount(@RequestParam("username") @Nonnull String username,
		@RequestParam("password") @Nonnull String password) {
		return String.valueOf(didditService.registerAccount(username, password));
	}

	@PostMapping("/posts/reply")
	public String postReply(@RequestParam("username") @Nonnull String username,
		@RequestParam("password") @Nonnull String password,
		@RequestParam("text") @Nonnull String text, @RequestParam("_id") @Nonnull String _id,
		Model model) {
		didditService.addReply(_id, text, username, password, "");
		Post reply = new Post(
			UUID.randomUUID().toString(),
			_id,
			username,
			(int) (System.currentTimeMillis() / 1000),
			text,
			"",
			"",
			"",
			""
		);
		model.addAttribute("reply", reply);
		return "";
	}

	@PostMapping("/posts/like")
	@ResponseBody
	public String likePost(@RequestParam("username") @Nonnull String username,
		@RequestParam("password") @Nonnull String password,
		@RequestParam("post_id") @Nonnull String post_id) {
		return didditService.likePost(username, password, post_id);
	}

	@PostMapping("/posts/dislike")
	@ResponseBody
	public String dislikePost(@RequestParam("username") @Nonnull String username,
		@RequestParam("password") @Nonnull String password,
		@RequestParam("post_id") @Nonnull String post_id) {
		didditService.dislikePost(username, password, post_id);
		return "";
	}

	@PostMapping("/posts/delete")
	@ResponseBody
	public String deletePost(@RequestParam("username") @Nonnull String username,
		@RequestParam("password") @Nonnull String password,
		@RequestParam("post_id") @Nonnull String post_id) {
		return didditService.deletePost(username, password, post_id);
	}

	@GetMapping("/posts/filter")
	public String filterPosts(@RequestParam(required = false) String filter, @RequestParam(name = "parent_id", required = false) String parent_id) {

		if (filter == null || filter.isEmpty()) {
			return ""; // Leer wenn SSE aktiv ist
		}
		List<Post> posts = didditService.getPostsFiltered(filter);

		return renderPostList(posts, parent_id);
	}

	@PostMapping("/authenticate")
	@ResponseBody
	public String authenticate(@RequestParam("username") @Nonnull String username,
		@RequestParam("password") @Nonnull String password) {
		var userOrEmpty = didditService.getUserByUsername(username);

		if (userOrEmpty.isEmpty()) {
			return "false";
		}

		if (!didditService.authenticate(userOrEmpty.get(), password)) {
			return "false";
		}

		return "true";
	}


	@Nonnull
	private String renderPostList(@Nonnull List<Post> posts, String parent_id) {
		Post parent = null;
		for (var post : posts) {
			if (post._id().equals(parent_id)) {
				parent = post;
				break;
			}
		}
		Context context = new Context();
		context.setVariable("posts", posts);
		context.setVariable("parent", parent);
		context.setVariable("parent_id", parent_id);
		return templateEngine.process("fragments/postList", Set.of("postListFrag"), context);
	}
}
