package com.ditto.example.spring.quickstart.service;

import com.ditto.java.*;
import com.ditto.java.serialization.DittoCborSerializable;

import jakarta.annotation.Nonnull;
import java.util.Objects;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class DittoPostService {
    private static final String TASKS_COLLECTION_NAME = "tasks";

    private final DittoService dittoService;

    public DittoPostService(DittoService dittoService) {
        this.dittoService = dittoService;
    }

    public void addPost(@Nonnull String author_id, @Nonnull String text) {
        this.addReply(null, author_id, text);
    }

    public void addReply(String parentId, @Nonnull String authorId, @Nonnull String text) {
        try {
            dittoService.getDitto().getStore().execute(
                    "INSERT INTO %s DOCUMENTS (:reply)".formatted(TASKS_COLLECTION_NAME),
                    DittoCborSerializable.Dictionary.buildDictionary()
                            .put(
                                    "reply",
                                    DittoCborSerializable.Dictionary.buildDictionary()
                                    .put("_id", UUID.randomUUID().toString())
                                    .put("parent", Objects.requireNonNullElse( parentId, ""))
                                    .put("author_id", authorId)
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

    @Nonnull
    public Flux<List<Post>> observeAll() {
        final String selectQuery = "SELECT * FROM %s ORDER BY time DESC".formatted(TASKS_COLLECTION_NAME);

        return Flux.create(emitter -> {
            Ditto ditto = dittoService.getDitto();
            try {
                DittoSyncSubscription subscription = ditto.getSync().registerSubscription(selectQuery);
                DittoStoreObserver observer = ditto.getStore().registerObserver(selectQuery, results -> {
                    emitter.next(results.getItems().stream().map(this::itemToPost).toList());
                });

                emitter.onDispose(() -> {
                    // TODO: Can't just catch, this potentially leaks the `observer` resource.
                    try {
                        subscription.close();
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
