package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.serversettings.ServerSettings;
import me.dmk.app.serversettings.ServerSettingsController;
import me.dmk.app.warn.Warn;
import me.dmk.app.warn.WarnController;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by DMK on 06.12.2022
 */

public class WarnCommand extends Command {

    private final WarnController warnController;
    private final ServerSettingsController serverSettingsController;

    public WarnCommand(String commandName, String commandDescription, WarnController warnController, ServerSettingsController serverSettingsController) {
        super(commandName, commandDescription);

        this.warnController = warnController;
        this.serverSettingsController = serverSettingsController;
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        User user = interaction.getArgumentUserValueByName("user").orElseThrow();
        String reason = interaction.getArgumentStringValueByName("reason").orElse("Nie podano powodu.");

        User admin = interaction.getUser();

        if (user.isBot()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie możesz nadać ostrzeżenia dla tego użytkownika.")).respond();
            return;
        }

        Optional<ServerSettings> settings = this.serverSettingsController.get(server.getId());

        Warn warn = new Warn(server, user, admin, reason);

        this.warnController.create(warn)
                .thenAcceptAsync(w -> {
                    if (settings.isPresent() && settings.get().getMaximumWarns() < 0
                            && this.warnController.gets(user.getId()).size() >= settings.get().getMaximumWarns()) {

                        if (!server.canYouBanUser(user)) {
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Użytkownik przekroczył ilość maksymalnych ostrzeżeń, lecz nie posiadam uprawnień do banowania.\nOstrzeżenie zostało nadane.")).respond();
                            return;
                        }

                        server.banUser(user, 7, TimeUnit.DAYS, reason)
                                .thenAccept(unused -> interaction.createImmediateResponder().addEmbed(new EmbedMessage(server).success().setDescription("Ostrzeżono użytkownika " + user.getMentionTag() + " (" + user.getDiscriminatedName() + ")" + "\nPrzekroczył on ilość maksymalnych ostrzeżeń - został zbanowany.").addField("Powód", reason)).respond())
                                .exceptionallyAsync(throwable -> {
                                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Użytkownik przekroczył ilość maksymanych ostrzeżeń, lecz wystąpił błąd podczas próby zbanowania go.\nOstrzeżenie zostało nadane.").addField("Błąd", throwable.getMessage())).respond();
                                    return null;
                                });
                    } else {
                        interaction.createImmediateResponder().addEmbed(new EmbedMessage(server).success().setDescription("Ostrzeżono użytkownika " + user.getMentionTag() + ".").addField("Powód", reason)).respond();
                    }
                })
                .exceptionallyAsync(throwable -> {
                    throwable.printStackTrace();
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystapił błąd.").addField("Błąd", throwable.getMessage())).respond();
                    return null;
                });
    }
}
