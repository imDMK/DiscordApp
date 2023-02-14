package me.dmk.app.serversettings;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.database.MongoService;
import org.bson.Document;
import org.javacord.api.DiscordApi;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DMK on 06.12.2022
 */

@Slf4j
@RequiredArgsConstructor
public class ServerSettingsController {

    private final MongoService mongoService;
    private final DiscordApi discordApi;

    @Getter
    private final Map<Long, ServerSettings> serverSettingsMap = new ConcurrentHashMap<>();
    private MongoCollection<Document> serverSettingsCollection;

    public void load() {
        this.serverSettingsCollection = this.mongoService.getMongoDatabase().getCollection("serverSettings");

        this.mongoService.loadAll(this.serverSettingsCollection, ServerSettings.class).forEach(serverSettings ->
                this.discordApi.getServerById(serverSettings.getServer())
                        .ifPresentOrElse(server ->
                                        this.serverSettingsMap.put(server.getId(), serverSettings)
                                , () -> {
                            this.delete(serverSettings);
                            log.info("Deleted server settings " + serverSettings.getServer() + " due to server doesn't exists.");
                        })
        );

        log.info("Loaded " + this.serverSettingsMap.size() + " server settings.");
    }

    public CompletableFuture<ServerSettings> create(long serverId) {
        ServerSettings serverSettings = new ServerSettings(serverId);

        return CompletableFuture.supplyAsync(() -> {
            this.mongoService.save(this.serverSettingsCollection, Filters.eq("server", String.valueOf(serverSettings.getServer())), serverSettings);
            this.serverSettingsMap.put(serverSettings.getServer(), serverSettings);

            return serverSettings;
        });
    }

    public CompletableFuture<ServerSettings> update(ServerSettings serverSettings) {
        return CompletableFuture.supplyAsync(() -> {
            this.mongoService.save(this.serverSettingsCollection, Filters.eq("server", String.valueOf(serverSettings.getServer())), serverSettings);
            return serverSettings;
        });
    }

    public void delete(ServerSettings serverSettings) {
        this.serverSettingsCollection.deleteOne(Filters.eq("server", String.valueOf(serverSettings.getServer())));
        this.serverSettingsMap.remove(serverSettings.getServer());
    }

    public Optional<ServerSettings> get(long serverId) {
        return Optional.ofNullable(this.serverSettingsMap.get(serverId));
    }
}
