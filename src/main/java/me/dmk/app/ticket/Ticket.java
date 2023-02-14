package me.dmk.app.ticket;

import lombok.Data;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.io.Serializable;

/**
 * Created by DMK on 06.12.2022
 */

@Data
public class Ticket implements Serializable {

    private final long server;
    private final long channel;
    private final long user;

    public Ticket(Server server, TextChannel textChannel, User user) {
        this.server = server.getId();
        this.channel = textChannel.getId();
        this.user = user.getId();
    }
}

