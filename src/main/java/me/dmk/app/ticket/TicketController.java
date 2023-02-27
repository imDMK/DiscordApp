package me.dmk.app.ticket;

import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.database.data.MongoDataService;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DMK on 06.12.2022
 */

@Slf4j
@RequiredArgsConstructor
public class TicketController {

    private final MongoDataService mongoDataService;
    private final DiscordApi discordApi;

    @Getter
    private final Map<Long, Ticket> ticketMap = new ConcurrentHashMap<>();

    public void load() {
        this.mongoDataService.findAll("tickets", Ticket.class).forEach(ticket -> {
            Optional<Server> serverOptional = discordApi.getServerById(ticket.getServer());
            if (serverOptional.isEmpty()) {
                this.delete(ticket);

                log.info("Deleted ticket user " + ticket.getUser() + " due to server doesn't exists.");
                return;
            }

            final Server server = serverOptional.get();

            if (server.getMemberById(ticket.getUser()).isEmpty()) {
                this.delete(ticket);

                log.info("Deleted ticket user " + ticket.getUser() + " due to user doesn't exists.");
                return;
            }

            Optional<ServerChannel> serverChannel = server.getChannelById(ticket.getChannel());
            if (serverChannel.isEmpty()) {
                this.delete(ticket);

                log.info("Deleted ticket user " + ticket.getUser() + " due to channel doesn't exists.");
                return;
            }

            this.ticketMap.put(ticket.getUser(), ticket);
        });

        log.info("Loaded " + this.ticketMap.size() + " active tickets.");
    }

    public CompletableFuture<Ticket> create(Ticket ticket) {
        return CompletableFuture.supplyAsync(() -> {
            this.mongoDataService.save(Filters.eq("user", String.valueOf(ticket.getUser())), ticket);
            this.ticketMap.put(ticket.getUser(), ticket);

            return ticket;
        });
    }

    public boolean delete(Ticket ticket) {
        this.ticketMap.remove(ticket.getUser());

        return this.mongoDataService.delete(Filters.eq("user", String.valueOf(ticket.getUser())));
    }

    public Optional<Ticket> get(User user) {
        return Optional.ofNullable(this.ticketMap.get(user.getId()));
    }

    public Optional<Ticket> get(Channel channel) {
        return this.ticketMap.values()
                .stream()
                .filter(ticket -> ticket.getChannel() == channel.getId())
                .findFirst();
    }
}
