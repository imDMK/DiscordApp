package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Ban;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by DMK on 06.12.2022
 */

public class UnBanCommand extends Command {
    public UnBanCommand(String commandName, String commandDescription) {
        super(commandName, commandDescription);

        this.setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR);
        this.addOption(
                SlashCommandOption.createStringOption("user", "Wpisz ID lub NICK użytkownika", true)
        );
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        String user = interaction.getArgumentStringValueByName("user").orElseThrow();

        Pattern pattern = Pattern.compile("\\b(#?[0-9]{4})");
        Matcher matcher = pattern.matcher(user);

        if (!matcher.find()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Argument " + user + " został niepoprawnie wprowadzony.").addField("Przykład", "DMK#8917")).respond();
            return;
        }

        if (!server.canYouBanUsers()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam uprawnień do odbanowywania.")).respond();
            return;
        }

        server.getBans()
                .thenAcceptAsync(bans -> bans.stream()
                        .map(Ban::getUser)
                        .filter(bannedUser -> bannedUser.getDiscriminatedName().equalsIgnoreCase(user))
                        .findFirst()
                        .ifPresentOrElse(bannedUser ->
                                server.unbanUser(bannedUser)
                                        .thenAcceptAsync(unused -> interaction.createImmediateResponder().addEmbed(new EmbedMessage(server).success().setDescription("Odbanowano użytkownika " + bannedUser.getDiscriminatedName() + ".")).respond())
                                        .exceptionallyAsync(throwable -> {
                                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas odbanowywania użytkownika.").addField("Błąd", throwable.getMessage())).respond();
                                            return null;
                                        }),
                                () -> interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie znaleziono zbanowanego użytkownika o podanej nazwie.")).respond()))
                .exceptionallyAsync(throwable -> {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd poczas próby wglądu do listy zbanowanych użytkowników.").addField("Błąd", throwable.getMessage())).respond();
                    return null;
                });
    }
}
