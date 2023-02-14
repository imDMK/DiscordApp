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
@Header("# File configuraion for discord client")
@Header("#")
public class ClientConfiguration extends OkaeriConfig {

    @Comment("# Application token from discord developers")
    public String token = "MTA0MDY1MDUyODA5MjMzMjExMg.Gv3nEY.JS1e2V_bKXP2KEWmx-SmFSXS6NUqOXosfg4fEg";

    public DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration();
}
