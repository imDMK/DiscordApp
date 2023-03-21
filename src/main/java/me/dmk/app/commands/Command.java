package me.dmk.app.commands;

import lombok.Getter;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

/**
 * Created by DMK on 07.12.2022
 */

public abstract class Command extends SlashCommandBuilder {

    @Getter
    private final String name;

    public Command(String name, String description) {
        this.name = name;

        this.setName(name);
        this.setDescription(description);
    }

    public abstract void execute(Server server, SlashCommandInteraction interaction);

    public void addOptions(SlashCommandOption... slashCommandOptions) {
        for (SlashCommandOption option : slashCommandOptions) {
            this.addOption(option);
        }
    }
}
