package me.dmk.app.log;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.serversettings.ServerSettings;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.util.Optional;

/**
 * Created by DMK on 23.12.2022
 */

@AllArgsConstructor
@Slf4j
public class LogMessage {

    private final Server server;
    private final ServerSettings settings;

    public void send(EmbedBuilder embedBuilder) {
        if (this.settings.getLogsChannel() == 0L) {
            return;
        }

        Optional<ServerTextChannel> textChannelOptional = this.server.getTextChannelById(this.settings.getLogsChannel());
        if (textChannelOptional.isEmpty()) {
            log.info("Failed to send log message, channel doesn't exists. Server: " + this.server.getId());
            return;
        }

        ServerTextChannel textChannel = textChannelOptional.get();

        if (textChannel.canYouSee() || textChannel.canYouWrite()) {
            log.info("Failed to send log message, missing permissions. Server: " + this.server.getId());
            return;
        }

        textChannel.sendMessage(embedBuilder)
                .exceptionally(throwable -> {
                    log.error("Failed to send log message. Server: " + this.server.getId(), throwable);
                    return null;
                });
    }
}
