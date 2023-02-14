package me.dmk.app.serversettings;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DMK on 06.12.2022
 */

@Data
public class ServerSettings implements Serializable {

    private long server;
    private long logsChannel;
    private long ticketsCategory;
    private long ticketsRoleChecker;
    private long welcomeChannel;

    private List<Long> welcomeRoles;

    private int maximumWarns;

    public ServerSettings(long serverId) {
        this.server = serverId;
        this.logsChannel = 0L;
        this.ticketsCategory = 0L;
        this.ticketsRoleChecker = 0L;
        this.welcomeChannel = 0L;

        this.welcomeRoles = new ArrayList<>();

        this.maximumWarns = 0;
    }
}
