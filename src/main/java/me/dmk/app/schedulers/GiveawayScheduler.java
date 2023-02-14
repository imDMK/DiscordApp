package me.dmk.app.schedulers;

import lombok.AllArgsConstructor;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.utils.StringUtil;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Created by DMK on 09.12.2022
 */

@AllArgsConstructor
public class GiveawayScheduler implements Runnable {

    private final DiscordApi discordApi;
    private final GiveawayController giveawayController;

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        List<Giveaway> giveawayList = this.giveawayController.getGiveawayMap().values()
                .stream()
                .filter(giveaway -> !giveaway.isEnded())
                .toList();

        for (Giveaway giveaway : giveawayList) {
            Optional<Server> serverOptional = this.discordApi.getServerById(giveaway.getServer());
            if (serverOptional.isEmpty()) {
                this.giveawayController.delete(giveaway);

                logger.info("Deleted giveaway " + giveaway.getAward() + " due to server is invalid.");
                return;
            }

            final Server server = serverOptional.get();

            Optional<ServerTextChannel> serverTextChannelOptional = server.getTextChannelById(giveaway.getChannel());
            if (serverTextChannelOptional.isEmpty()) {
                this.giveawayController.delete(giveaway);

                logger.info("Deleted giveaway " + giveaway.getAward() + " due to channel doesn't exists.");
                return;
            }

            final ServerTextChannel serverTextChannel = serverTextChannelOptional.get();

            serverTextChannel.getMessageById(giveaway.getMessage())
                    .thenAcceptAsync(message -> {
                        if (Instant.now().isAfter(giveaway.getExpire().toInstant())) {
                            this.giveawayController.finish(server, message, giveaway);
                            return;
                        }

                        message.edit(StringUtil.getGiveawayMessageTemplate(server, giveaway));
                    })
                    .exceptionallyAsync(throwable -> {
                        this.giveawayController.delete(giveaway);

                        logger.info("Deleted giveaway " + giveaway.getWinners() + "x "+ giveaway.getAward() + " due to message doesn't exists.");
                        return null;
                    });
        }
    }
}
