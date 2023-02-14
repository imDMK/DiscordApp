package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.warn.Warn;
import me.dmk.app.warn.WarnController;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRowBuilder;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.Comparator;
import java.util.List;

/**
 * Created by DMK on 06.12.2022
 */

public class WarnListCommand extends Command {

    private final WarnController warnController;

    public WarnListCommand(String commandName, String commandDescription, WarnController warnController) {
        super(commandName, commandDescription);

        this.warnController = warnController;
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        User user = interaction.getArgumentUserValueByName("user").orElseThrow();

        if (user.isBot()) {
           interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie możesz sprawdzić ostrzeżeń dla tego użytkownika.")).respond();
           return;
        }

        List<Warn> warnList = warnController.gets(user.getId())
                .stream()
                .sorted(Comparator.comparing(Warn::getCreated).reversed())
                .toList();

        if (warnList.isEmpty()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Użytkownik " + user.getMentionTag() + " nie posiada ostrzeżeń.")).respond();
            return;
        }

        EmbedBuilder embedBuilder = new EmbedMessage(server).success().setDescription("Użytkownik " + user.getMentionTag() + " posiada **" + warnList.size() + "** ostrzeżeń");
        ActionRowBuilder actionRowBuilder = new ActionRowBuilder();

        if (warnList.size() > 4) {
            for (Warn warn : warnList.stream().limit(4).toList()) {
                embedBuilder.addField(warn.getIdentifier(), warn.getReason());
                actionRowBuilder.addComponents(Button.secondary("check-warn-" + warn.getIdentifier(), warn.getIdentifier()));
            }
            actionRowBuilder.addComponents(Button.primary("warns-list-2-" + user.getId(), "Następna strona", "➡"));
        } else {
            for (Warn warn : warnList) {
                embedBuilder.addField(warn.getIdentifier(), warn.getReason());
                actionRowBuilder.addComponents(Button.secondary("check-warn-" + warn.getIdentifier(), warn.getIdentifier()));
            }
        }

        interaction.createImmediateResponder()
                .setFlags(MessageFlag.EPHEMERAL)
                .addEmbed(embedBuilder)
                .addComponents(actionRowBuilder.build())
                .respond();
    }
}
