package me.dmk.app.warn;

import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.database.data.MongoDataService;
import org.javacord.api.DiscordApi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by DMK on 06.12.2022
 */

@Slf4j
@RequiredArgsConstructor
public class WarnController {

    private final MongoDataService mongoDataService;
    private final DiscordApi discordApi;

    @Getter
    private Map<String, Warn> warnMap = new ConcurrentHashMap<>();

    public void load() {
        this.mongoDataService.findAll(Warn.class).forEach(warn -> {
            if (this.discordApi.getServerById(warn.getServer()).isEmpty()) {
                delete(warn);
                log.info("Deleted warn " + warn.getIdentifier() + " due to server doesn't exists.");
                return;
            }

            this.warnMap.put(warn.getIdentifier(), warn);
        });

        log.info("Loaded " + this.warnMap.size() + " users warns.");
    }

    public CompletableFuture<Warn> create(Warn warn) {
        return CompletableFuture.supplyAsync(() -> {
            this.mongoDataService.save(Filters.eq("identifier", warn.getIdentifier()), warn);
            this.warnMap.put(warn.getIdentifier(), warn);

            return warn;
        });
    }

    public boolean delete(Warn warn) {
        this.warnMap.remove(warn.getIdentifier());

        return this.mongoDataService.delete(warn);
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
