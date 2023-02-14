package me.dmk.app.configuration;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.Getter;

/**
 * Created by DMK on 06.12.2022
 */

@Getter

@Header("#")
@Header("# Database configuration for MongoDB server")
@Header("#")
public class DatabaseConfiguration extends OkaeriConfig {

    @Comment("# Boolean value whether to connect to the database user.")
    public boolean authentication = false;

    @Comment("# Username")
    public String userName = "";

    @Comment("# Password")
    public String password = "";

    @Comment("# Hostname (example: localhost)")
    public String hostName = "localhost";

    @Comment("# Port (default: 27017")
    public int port = 27017;

    @Comment("# Database name")
    public String databaseName = "discordclient";
}