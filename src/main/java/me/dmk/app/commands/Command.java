package me.dmk.app.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandInteraction;

/**
 * Created by DMK on 07.12.2022
 */

@AllArgsConstructor
public abstract class Command {

    @Getter
    private final String commandName;
    @Getter
    private final String commandDescription;

    public abstract void execute(Server server, SlashCommandInteraction interaction);
}
