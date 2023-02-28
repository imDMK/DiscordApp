package me.dmk.app.listeners;

import lombok.AllArgsConstructor;
import me.dmk.app.commands.CommandController;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

/**
 * Created by DMK on 07.12.2022
 */

@AllArgsConstructor
public class CommandListener implements SlashCommandCreateListener {

    private final CommandController commandController;

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();

        String commandName = interaction.getCommandName();

        interaction.getServer().ifPresent(server -> this.commandController.getCommand(commandName)
                .ifPresent(command -> command.execute(server, interaction))
        );
    }
}
