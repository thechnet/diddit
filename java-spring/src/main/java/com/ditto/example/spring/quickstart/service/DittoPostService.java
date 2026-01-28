package com.ditto.example.spring.quickstart.service;

import com.ditto.example.spring.quickstart.Hash;
import com.ditto.java.Ditto;
import com.ditto.java.DittoError;
import com.ditto.java.DittoQueryResultItem;
import com.ditto.java.DittoStoreObserver;
import com.ditto.java.DittoSyncSubscription;
import com.ditto.java.serialization.DittoCborSerializable;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Component
public class DittoPostService {

	public static final String TASKS_COLLECTION_NAME = "tasks";
	private static final String USERS_COLLECTION_NAME = "users";

	private final DittoService dittoService;
	private final Logger logger = LoggerFactory.getLogger(DittoPostService.class);

	public DittoPostService(DittoService dittoService) {
		this.dittoService = dittoService;
	}

	public void listUsers() {
		try {
			List<? extends DittoQueryResultItem> results = dittoService.getDitto().getStore()
				.execute(
					"SELECT * FROM %s".formatted(USERS_COLLECTION_NAME)
				).toCompletableFuture().join().getItems();
			logger.info("listUsers");
			for (var r : results) {
				logger.info("{}", r);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Optional<DittoCborSerializable.Dictionary> getUserByUsername(@Nonnull String username) {
		try {
			List<? extends DittoQueryResultItem> results = dittoService.getDitto().getStore()
				.execute(
					"SELECT * FROM %s WHERE username = :username".formatted(USERS_COLLECTION_NAME),
					DittoCborSerializable.Dictionary.buildDictionary()
						.put("username", username)
						.build()
				).toCompletableFuture().join().getItems();

			if (results.isEmpty()) {
				return Optional.empty();
			}

			return Optional.of(results.get(0).getValue());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean authenticate(@Nonnull DittoCborSerializable.Dictionary user,
		@Nonnull String password) {
		try {
			String userId = user.get("_id").getString();
			String hashedPassword = user.get("hashed_password").getString();

			return Hash.hash(password, userId).equals(hashedPassword);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void registerAccount(@Nonnull String username, @Nonnull String password) {
		final String user_id = UUID.randomUUID().toString();

		// TODO: avoid collisions
		try {
			dittoService.getDitto().getStore().execute(
				"INSERT INTO %s DOCUMENTS (:user)".formatted(USERS_COLLECTION_NAME),
				DittoCborSerializable.Dictionary.buildDictionary()
					.put(
						"user",
						DittoCborSerializable.Dictionary.buildDictionary()
							.put("_id", user_id)
							.put("username", username)
							.put("hashed_password", Hash.hash(password, user_id))
							.build()
					)
					.build()
			).toCompletableFuture().join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void addReply(String parentId, @Nonnull String text, @Nonnull String username,
		@Nonnull String password, String attachmentPath) {
		listUsers();

		var userOrEmpty = getUserByUsername(username);
		if (userOrEmpty.isEmpty()) {
			logger.error("User not found: '{}'", username);
			return;
		}

		var user = userOrEmpty.get();
		if (!authenticate(user, password)) {
			logger.error("Invalid password for user: {}", username);
			return;
		}

		try {
			dittoService.getDitto().getStore().execute(
				"INSERT INTO %s DOCUMENTS (:reply)".formatted(TASKS_COLLECTION_NAME),
				DittoCborSerializable.Dictionary.buildDictionary()
					.put(
						"reply",
						DittoCborSerializable.Dictionary.buildDictionary()
							.put("_id", UUID.randomUUID().toString())
							.put("parent", Objects.requireNonNullElse(parentId, ""))
							.put("author_id", user.get("_id").getString())
							.put("time", (int) (System.currentTimeMillis() / 1000))
							.put("text", text)
							.put("attachment", Objects.requireNonNullElse(attachmentPath, ""))
							.put("likes", "")
							.put("dislikes", "")
							.put("tags", "")
							.build()
					)
					.build()
			).toCompletableFuture().join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String ratePost(@Nonnull String username, @Nonnull String password,
		@Nonnull String column_name, @Nonnull String post_id) {
		var userOrEmpty = this.getUserByUsername(username);
		if (userOrEmpty.isEmpty() || !authenticate(userOrEmpty.get(), password)) {
			logger.error("cannot authenticate");
			return "";
		}
		var user = userOrEmpty.get();

		try {
			List<? extends DittoQueryResultItem> results = dittoService.getDitto().getStore()
				.execute(
					"SELECT * FROM %s WHERE _id = :post_id".formatted(TASKS_COLLECTION_NAME),
					DittoCborSerializable.Dictionary.buildDictionary()
						.put("post_id", post_id)
						.build()
				).toCompletableFuture().join().getItems();

			assert !results.isEmpty();
			assert results.size() == 1;
			DittoCborSerializable.Dictionary post = results.get(0).getValue();
			String user_ids = post.get(column_name).getString();

			dittoService.getDitto().getStore().execute(
				"UPDATE %s SET likes = :likes WHERE _id = :_id".formatted(TASKS_COLLECTION_NAME),
				DittoCborSerializable.Dictionary.buildDictionary()
					.put(column_name, user_ids + " " + user.get("_id").getString())
					.put("_id", post_id)
					.build()
			).toCompletableFuture().join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return "";
	}

	public String likePost(@Nonnull String username, @Nonnull String password,
		@Nonnull String post_id) {
		return this.ratePost(username, password, "likes", post_id);
	}

	public String dislikePost(@Nonnull String username, @Nonnull String password,
		@Nonnull String post_id) {
		return this.ratePost(username, password, "dislikes", post_id);
	}

	private void _deleteTask(@Nonnull String post_id) {
		/* Delete all my children. */
		try {
			var result = dittoService.getDitto().getStore().execute(
				"SELECT * FROM %s WHERE parent = :post_id".formatted(TASKS_COLLECTION_NAME),
				DittoCborSerializable.Dictionary.buildDictionary()
					.put("post_id", post_id)
					.build()
			).toCompletableFuture().join();

			List<String> child_ids = result.getItems().stream()
				.map(item -> item.getValue().get("_id").getString())
				.toList();

			for (var child_id : child_ids) {
				this._deleteTask(child_id);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		/* Delete myself. */
		try {
			dittoService.getDitto().getStore().execute(
				"DELETE FROM %s WHERE _id = :post_id".formatted(TASKS_COLLECTION_NAME),
				DittoCborSerializable.Dictionary.buildDictionary()
					.put("post_id", post_id)
					.build()
			).toCompletableFuture().join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteTask(@Nonnull String username, @Nonnull String password,
		@Nonnull String post_id) {
		var userOrEmpty = getUserByUsername(username);
		if (userOrEmpty.isEmpty()) {
			logger.error("User not found: '{}'", username);
			return;
		}

		var user = userOrEmpty.get();
		if (!authenticate(user, password)) {
			logger.error("Invalid password for user: {}", username);
			return;
		}

		/* Authenticate. */
		try {
			var result = dittoService.getDitto().getStore().execute(
				"SELECT * FROM %s WHERE _id = :post_id".formatted(TASKS_COLLECTION_NAME),
				DittoCborSerializable.Dictionary.buildDictionary()
					.put("post_id", post_id)
					.build()
			).toCompletableFuture().join();

			List<String> author_ids = result.getItems().stream()
				.map(item -> item.getValue().get("author_id").getString())
				.toList();
			assert author_ids.size() == 1;

			var author_id = author_ids.get(0);

			if (!user.get("_id").getString().equals(author_id)) {
				logger.error("Cannot delete other user's post");
				return;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this._deleteTask(post_id);
	}

	@Nonnull
	public Flux<List<Post>> observeAll() {
		String tasksQuery = "SELECT * FROM %s ORDER BY time DESC".formatted(TASKS_COLLECTION_NAME);
		String usersQuery = "SELECT * FROM %s".formatted(USERS_COLLECTION_NAME);

		return Flux.combineLatest(
			observeQuery(tasksQuery, "SELECT * FROM %s".formatted(TASKS_COLLECTION_NAME)),
			observeQuery(usersQuery, usersQuery),
			(tasks, users) -> {
				var userMap = users.stream()
					.map(DittoQueryResultItem::getValue)
					.collect(java.util.stream.Collectors.toMap(
						u -> u.get("_id").getString(),
						u -> u.get("username").getString(),
						(u1, u2) -> u1
					));

				return tasks.stream().map(item -> {
					var value = item.getValue();
					String authorId = value.get("author_id").getString();
					String username = userMap.getOrDefault(authorId, "Unknown");
					return itemToPost(value, username);
				}).toList();
			}
		);
	}

	private Flux<List<? extends DittoQueryResultItem>> observeQuery(String query,
		String subscriptionQuery) {
		return Flux.create(emitter -> {
			Ditto ditto = dittoService.getDitto();
			try {
				DittoSyncSubscription subscription = ditto.getSync()
					.registerSubscription(subscriptionQuery);
				DittoStoreObserver observer = ditto.getStore().registerObserver(query, results -> {
					emitter.next(results.getItems());
				});

				emitter.onDispose(() -> {
					try {
						subscription.close();
					} catch (IOException e) {
						logger.error("Error closing subscription", e);
					}
					try {
						observer.close();
					} catch (IOException e) {
						logger.error("Error closing observer", e);
					}
				});
			} catch (DittoError e) {
				emitter.error(e);
			}
		}, FluxSink.OverflowStrategy.LATEST);
	}

	public List<Post> getTasksFiltered(String filter) {

		String orderBy;
		switch (filter) {
			case "time_asc":
				orderBy = "time ASC";
				break;
			case "likes_desc":
				orderBy = "likes - dislikes DESC";
				break;
			case "likes_asc":
				orderBy = "likes - dislikes ASC";
				break;
			case "time_desc":
			default:
				orderBy = "time DESC";
				break;
		}

		String tasksQuery =
			"SELECT * FROM %s ORDER BY %s"
				.formatted(TASKS_COLLECTION_NAME, orderBy);

		String usersQuery =
			"SELECT * FROM %s".formatted(USERS_COLLECTION_NAME);

		try {
			var taskResults = dittoService.getDitto()
				.getStore()
				.execute(tasksQuery)
				.toCompletableFuture()
				.join()
				.getItems();

			var userResults = dittoService.getDitto()
				.getStore()
				.execute(usersQuery)
				.toCompletableFuture()
				.join()
				.getItems();

			var userMap = userResults.stream()
				.map(DittoQueryResultItem::getValue)
				.collect(java.util.stream.Collectors.toMap(
					u -> u.get("_id").getString(),
					u -> u.get("username").getString(),
					(u1, u2) -> u1
				));

			return taskResults.stream().map(item -> {
				var value = item.getValue();
				String authorId = value.get("author_id").getString();
				String username = userMap.getOrDefault(authorId, "Unknown");
				return itemToPost(value, username);
			}).toList();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Post itemToPost(@Nonnull DittoCborSerializable.Dictionary value, String username) {
		return new Post(
			value.get("_id").getString(),
			value.get("parent").getString(),
			username,
			value.get("time").getInt(),
			value.get("text").getString(),
			value.get("attachment").getString(),
			value.get("likes").getString(),
			value.get("dislikes").getString(),
			value.get("tags").getString()
		);
	}
}
