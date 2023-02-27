package me.dmk.app.warn;

import lombok.Data;
import me.dmk.app.database.data.entity.DataEntity;
import org.apache.commons.lang3.RandomStringUtils;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by DMK on 06.12.2022
 */

@Data
@DataEntity(collection = "warns")
public class Warn implements Serializable {

    private final long server;
    private final long user;
    private final long admin;

    private final String reason;
    private final String identifier = RandomStringUtils.randomNumeric(8);

    private final Date created = new Date();

    public Warn(Server server, User user, User admin, String reason) {
        this.server = server.getId();
        this.user = user.getId();
        this.admin = admin.getId();

        this.reason = reason;
    }
}
