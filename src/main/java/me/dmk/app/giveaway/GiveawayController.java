package me.dmk.app.giveaway;

import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.database.data.MongoDataService;
import me.dmk.app.embed.EmbedMessage;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DMK on 06.12.2022
 */

@Slf4j
@RequiredArgsConstructor
public class GiveawayController {

    private final MongoDataService mongoDataService;
    private final DiscordApi discordApi;

    @Getter
    private Map<Long, Giveaway> giveawayMap = new ConcurrentHashMap<>();

    public void load() {
        this.mongoDataService.findAll("giveaways", Giveaway.class).forEach(giveaway -> {
            Optional<Server> server = discordApi.getServerById(giveaway.getServer());
            if (server.isEmpty()) {
                this.delete(giveaway);
                log.info("Deleted giveaway with award " + giveaway.getWinners() + "x " + giveaway.getAward() + " due to server doesn't exists.");
                return;
            }

            Optional<ServerTextChannel> textChannel = server.get().getTextChannelById(giveaway.getChannel());
            if (textChannel.isEmpty()) {
                this.delete(giveaway);
                log.info("Deleted giveaway with award " + giveaway.getWinners() + "x " + giveaway.getAward() + " due to channel doesn't exists.");
                return;
            }

            textChannel.get().getMessageById(giveaway.getMessage())
                    .exceptionally(throwable -> {
                        this.delete(giveaway);
                        log.info("Deleted giveaway with award " + giveaway.getWinners() + "x " + giveaway.getAward() + " due to message doesn't exists.");
                        return null;
                    });

            //Check if giveaway is active
            if (!giveaway.isEnded() && Instant.now().isAfter(giveaway.getExpire().toInstant())) {
                this.delete(giveaway);
                log.info("Deleted giveaway with award " + giveaway.getWinners() + "x " + giveaway.getAward() + " due to expire.");
                return;
            }

            //Check if giveaway was created above 30 days ago.
            if (Instant.now().plus(30, ChronoUnit.DAYS).isBefore(giveaway.getCreated().toInstant())) {
                this.delete(giveaway);
                log.info("Deleted giveaway from database with award " + giveaway.getWinners() + "x " + giveaway.getAward() + " due to was over a month ago.");
                return;
            }

            this.giveawayMap.put(giveaway.getMessage(), giveaway);
        });

        log.info("Loaded " + this.giveawayMap.size() + " giveaways.");
    }

    public CompletableFuture<Giveaway> create(Giveaway giveaway) {
        return CompletableFuture.supplyAsync(() -> {
            this.mongoDataService.save(Filters.eq("message", String.valueOf(giveaway.getMessage())), giveaway);
            this.giveawayMap.put(giveaway.getMessage(), giveaway);
            return giveaway;
        });
    }

    public CompletableFuture<Giveaway> update(Giveaway giveaway) {
        return CompletableFuture.supplyAsync(() -> {
            this.mongoDataService.save(Filters.eq("message", String.valueOf(giveaway.getMessage())), giveaway);
            return giveaway;
        });
    }

    public void delete(Giveaway giveaway) {
        this.giveawayMap.remove(giveaway.getMessage());
        this.mongoDataService.delete(giveaway);
    }

    public CompletableFuture<Giveaway> addUser(User user, Giveaway giveaway) {
        giveaway.getUsers().add(user.getId());
        return this.update(giveaway);
    }

    public CompletableFuture<Giveaway> removeUser(User user, Giveaway giveaway) {
        giveaway.getUsers().remove(user.getId());
        return this.update(giveaway);
    }

    public List<String> selectWinners(Giveaway giveaway, int winners) {
        ArrayList<String> winnerList = new ArrayList<>();

        for (int i = 0; i < winners; i++) {
            long winner = giveaway.getUsers().stream().toList().get((int) (Math.random() * giveaway.getUsers().size()));
            winnerList.add("<@" + winner + ">");
        }

        return winnerList;
    }

    public void finish(Server server, Message message, Giveaway giveaway) {
        if (giveaway.getUsers().size() < giveaway.getWinners()) {
            message.delete("Not enough people took part in the giveaway (" + giveaway.getUsers().size() + "/" + giveaway.getWinners() + ")");

            this.delete(giveaway);
            return;
        }

        List<String> winners = this.selectWinners(giveaway, giveaway.getWinners());

        message.createUpdater()
                .setEmbed(new EmbedMessage(server).giveaway()
                        .setDescription("Informacje o zakończonym konkursie:")
                        .addField("Nagroda", "**" + giveaway.getWinners() + "x** " + giveaway.getAward())
                        .addField("Zwyciężcy", String.join("\n", winners)).addField("Wzięło udział", String.valueOf(giveaway.getUsers().size())))
                .removeAllComponents()
                .applyChanges()
                .thenAcceptAsync(msg -> {
                    giveaway.setEnded(true);
                    this.update(giveaway);

                    new MessageBuilder()
                            .setContent("Gratulacje " + String.join(", ", winners) + " "+ (winners.size() > 1 ? "wygraliście" : "wygrałeś(-aś)") + " konkurs.")
                            .replyTo(message)
                            .send(message.getChannel());
                });
    }

    public Optional<Giveaway> get(long message) {
        return Optional.ofNullable(this.giveawayMap.get(message));
    }
}
