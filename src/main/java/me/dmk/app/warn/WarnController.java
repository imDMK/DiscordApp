package me.dmk.app.warn;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.dmk.app.database.MongoService;
import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by DMK on 06.12.2022
 */

@RequiredArgsConstructor
public class WarnController {

    private final MongoService mongoService;
    private final DiscordApi discordApi;

    @Getter
    private Map<String, Warn> warnMap = new ConcurrentHashMap<>();
    private MongoCollection<Document> warnCollection;

    public void load() {
        this.warnCollection = mongoService.getMongoDatabase().getCollection("warns");

        Logger logger = LoggerFactory.getLogger(this.getClass());

        this.warnCollection.find().forEach((Consumer<? super Document>) document -> {
            Warn warn = this.mongoService.load(document, Warn.class);

            if (this.discordApi.getServerById(warn.getServer()).isEmpty()) {
                delete(warn);
                logger.info("Deleted warn " + warn.getIdentifier() + " due to server doesn't exists.");
                return;
            }

            this.warnMap.put(warn.getIdentifier(), warn);
        });

        logger.info("Loaded " + this.warnMap.size() + " users warns.");
    }

    public CompletableFuture<Warn> create(Warn warn) {
        return CompletableFuture.supplyAsync(() -> {
            this.mongoService.save(this.warnCollection, Filters.eq("identifier", warn.getIdentifier()), warn);
            this.warnMap.put(warn.getIdentifier(), warn);

            return warn;
        });
    }

    public boolean delete(Warn warn) {
        this.warnMap.remove(warn.getIdentifier());

        DeleteResult deleteResult = this.warnCollection.deleteOne(Filters.eq("identifier", warn.getIdentifier()));
        return deleteResult.wasAcknowledged();
    }

    public Optional<Warn> get(String identifier) {
        return Optional.ofNullable(this.warnMap.get(identifier));
    }

    public List<Warn> gets(long userId) {
        return this.warnMap.values()
                .stream()
                .filter(warn -> warn.getUser() == userId)
                .collect(Collectors.toList());
    }
}
