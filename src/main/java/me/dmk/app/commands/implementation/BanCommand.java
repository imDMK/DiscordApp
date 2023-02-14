package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.concurrent.TimeUnit;

/**
 * Created by DMK on 07.12.2022
 */

public class BanCommand extends Command {
    public BanCommand(String commandName, String commandDescription) {
        super(commandName, commandDescription);
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        User user = interaction.getArgumentUserValueByName("user").orElseThrow();

        String reason = interaction.getArgumentStringValueByName("reason")
                .orElse("Nie podano powodu.");
        boolean deleteMessages = interaction.getArgumentBooleanValueByName("deletemessages")
                .orElse(false);

        if (!server.canYouBanUsers() || !server.canBanUser(interaction.getUser(), user)) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam/posiadasz uprawnień do zbanowania tego użytkownika.")).respond();
            return;
        }

        server.banUser(user, (deleteMessages ? 0 : 7), TimeUnit.DAYS, reason)
                .thenAcceptAsync(action ->
                        interaction.createImmediateResponder().addEmbed(new EmbedMessage(server).success().setDescription("Zbanowano " + user.getDiscriminatedName() + ".").addField("Powód", reason)).respond())
                .exceptionallyAsync(throwable -> {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd.").addField("Błąd", throwable.getMessage())).respond();
                    return null;
                });
    }
}
