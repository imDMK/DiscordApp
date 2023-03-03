package me.dmk.app.serversettings;

import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.database.data.MongoDataService;
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

    private final MongoDataService mongoDataService;
    private final DiscordApi discordApi;

    @Getter
    private final Map<Long, ServerSettings> serverSettingsMap = new ConcurrentHashMap<>();

    public void load() {

        this.mongoDataService.findAll(ServerSettings.class).forEach(serverSettings ->
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
            this.mongoDataService.save(Filters.eq("server", String.valueOf(serverSettings.getServer())), serverSettings);
            this.serverSettingsMap.put(serverSettings.getServer(), serverSettings);

            return serverSettings;
        });
    }

    public CompletableFuture<ServerSettings> update(ServerSettings serverSettings) {
        return CompletableFuture.supplyAsync(() -> {
            this.mongoDataService.save(Filters.eq("server", String.valueOf(serverSettings.getServer())), serverSettings);
            return serverSettings;
        });
    }

    public void delete(ServerSettings serverSettings) {
        this.mongoDataService.delete(serverSettings);
        this.serverSettingsMap.remove(serverSettings.getServer());
    }

    public Optional<ServerSettings> get(long serverId) {
        return Optional.ofNullable(this.serverSettingsMap.get(serverId));
    }
}
