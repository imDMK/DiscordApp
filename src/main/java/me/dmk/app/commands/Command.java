package me.dmk.app.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandInteraction;

/**
 * Created by DMK on 07.12.2022
 */

@Getter
@AllArgsConstructor
public abstract class Command {

    private final String commandName;
    private final String commandDescription;

    public abstract void execute(Server server, SlashCommandInteraction interaction);
}
