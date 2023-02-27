package me.dmk.app.giveaway;

import lombok.Data;
import me.dmk.app.database.data.entity.DataEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by DMK on 06.12.2022
 */

@Data
@DataEntity(collection = "giveaways")
public class Giveaway implements Serializable {

    private final long server;
    private final long channel;
    private final long message;
    private final String award;

    private final int winners;

    private final Date created;
    private final Date expire;

    private List<Long> users;

    private boolean ended;

    public Giveaway(Server server, Message message, String award, int winners, Instant expire) {
        this.server = server.getId();
        this.channel = message.getChannel().getId();
        this.message = message.getId();
        this.award = award;
        this.winners = winners;

        this.created = new Date();
        this.expire = Date.from(expire);

        this.users = new ArrayList<>();

        this.ended = false;
    }
}
