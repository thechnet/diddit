package com.ditto.example.spring.quickstart.service;

import com.ditto.example.spring.quickstart.Hash;
import com.ditto.java.*;
import com.ditto.java.serialization.DittoCborSerializable;

import jakarta.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class DittoPostService {
    private static final String TASKS_COLLECTION_NAME = "tasks";
	private static final String USERS_COLLECTION_NAME = "users";

    private final DittoService dittoService;
	private final Logger logger = LoggerFactory.getLogger(DittoPostService.class);

    public DittoPostService(DittoService dittoService) {
        this.dittoService = dittoService;
    }

	public void listUsers() {
		try {
			List<? extends DittoQueryResultItem> results = dittoService.getDitto().getStore().execute(
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
			List<? extends DittoQueryResultItem> results = dittoService.getDitto().getStore().execute(
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

    public boolean authenticate(@Nonnull DittoCborSerializable.Dictionary user, @Nonnull String password) {
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

	public void addPost(@Nonnull String text, @Nonnull String username, @Nonnull String password) {
		this.addReply(null, text, username, password);
	}

    public void addReply(String parentId, @Nonnull String text, @Nonnull String username, @Nonnull String password) {
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
                                    .put("parent", Objects.requireNonNullElse( parentId, ""))
                                    .put("author_id", user.get("_id").getString())
                                    .put("time", (int) (System.currentTimeMillis() / 1000))
                                    .put("text", text)
                                    .put("attachment", "")
                                    .put("likes", 0)
                                    .put("dislikes", 0)
                                    .put("tags", "")
                                    .build()
                                )
                                    .build()
            ).toCompletableFuture().join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void likePost(@Nonnull String id, @Nonnull int likes) {
        try {
            dittoService.getDitto().getStore().execute(
                    "UPDATE %s SET likes = :likes WHERE _id = :_id".formatted(TASKS_COLLECTION_NAME),
                    DittoCborSerializable.Dictionary.buildDictionary()
                            .put("likes", likes + 1)
                            .put("_id", id)
                            .build()
            ).toCompletableFuture().join();
        } catch (Error e) {
            throw new RuntimeException(e);
        }
    }
    public void dislikePost(@Nonnull String id, @Nonnull int dislikes) {
        try {
            dittoService.getDitto().getStore().execute(
                    "UPDATE %s SET dislikes = :dislikes WHERE _id = :_id".formatted(TASKS_COLLECTION_NAME),
                    DittoCborSerializable.Dictionary.buildDictionary()
                            .put("dislikes", dislikes + 1)
                            .put("_id", id)
                            .build()
            ).toCompletableFuture().join();
        } catch (Error e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public Flux<List<Post>> observeAll() {
        final String selectQuery = "SELECT * FROM %s ORDER BY time DESC".formatted(TASKS_COLLECTION_NAME);
        //final String selectQuery = "SELECT * FROM %s WHERE NOT deleted ORDER BY likes DESC".formatted(TASKS_COLLECTION_NAME);


        return Flux.create(emitter -> {
            Ditto ditto = dittoService.getDitto();
            try {
                DittoSyncSubscription subscription = ditto.getSync().registerSubscription(selectQuery);

				// Need this to receive updates
	            DittoSyncSubscription subscriptionUsers = ditto.getSync().registerSubscription("SELECT * from %s".formatted(USERS_COLLECTION_NAME));

				DittoStoreObserver observer = ditto.getStore().registerObserver(selectQuery, results -> {
                    emitter.next(results.getItems().stream().map(this::itemToPost).toList());
                });

                emitter.onDispose(() -> {
                    // TODO: Can't just catch, this potentially leaks the `observer` resource.
                    try {
                        subscription.close();
						subscriptionUsers.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        observer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (DittoError e) {
                emitter.error(e);
            }
        }, FluxSink.OverflowStrategy.LATEST);
    }

    private Post itemToPost(@Nonnull DittoQueryResultItem item) {
        DittoCborSerializable.Dictionary value = item.getValue();
        return new Post(
            value.get("_id").getString(),
            value.get("parent").getString(),
            value.get("author_id").getString(),
            value.get("time").getInt(),
            value.get("text").getString(),
            value.get("attachment").getString(),
            value.get("likes").getInt(),
            value.get("dislikes").getInt(),
            value.get("tags").getString()
        );
    }
}
