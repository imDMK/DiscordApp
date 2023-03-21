package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.utils.StringUtil;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by DMK on 08.12.2022
 */

public class MessageCommand extends Command {

    public MessageCommand(String commandName, String commandDescription) {
        super(commandName, commandDescription);

        this.setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR);
        this.addOptions(
                SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "action", "Wybierz rodzaj interakcji", true,
                        Arrays.asList(
                                SlashCommandOptionChoice.create("ticket", "ticket"),
                                SlashCommandOptionChoice.create("send", "send")
                        )
                ),
                SlashCommandOption.createChannelOption("channel", "Kanał tekstowy", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL)),
                SlashCommandOption.createStringOption("message", "Treść wiadomości (użyj {NL} do nowej linii w wiadomości embed)", true)
        );
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        String actionType = interaction.getArgumentStringValueByName("action").orElseThrow();
        ServerChannel serverChannel = interaction.getArgumentChannelValueByName("channel").orElseThrow();
        String message = interaction.getArgumentStringValueByName("message").orElseThrow();

        if (serverChannel.asServerTextChannel().isEmpty()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Kanał musi być tekstowy.")).respond();
            return;
        }

        final ServerTextChannel serverTextChannel = serverChannel.asServerTextChannel().get();

        if (!serverTextChannel.canYouSee() || !serverTextChannel.canYouWrite()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam dostępu do oznaczonego kanału.")).respond();
            return;
        }

        EmbedBuilder embedBuilder = new EmbedMessage(server).defaultEmbed().setDescription(message.replace("{NL}", "\n"));

        switch (actionType) {
            case "ticket" -> new MessageBuilder()
                    .setEmbed(
                            embedBuilder.setTitle("Centrum pomocy")
                    )
                    .addComponents(ActionRow.of(
                            Button.primary("ticket-create", "Stwórz zgłoszenie", "✉️"))
                    )
                    .send(serverTextChannel)
                    .thenAcceptAsync(m ->
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Wysłano [wiadomość](" + StringUtil.createJumpMessageUrl(server, m) + ") na kanał " + serverTextChannel.getMentionTag() + ".")).respond())
                    .exceptionallyAsync(throwable -> {
                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas wysyłania wiadomości.").addField("Błąd", throwable.getMessage())).respond();
                        return null;
                    });

            case "send" -> serverTextChannel.sendMessage(embedBuilder.setTitle("Wiadomość"))
                    .thenAcceptAsync(m ->
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Wysłano [wiadomość](" + StringUtil.createJumpMessageUrl(server, m) + ") na kanał " + serverTextChannel.getMentionTag() + ".")).respond())
                    .exceptionallyAsync(throwable -> {
                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas wysyłania wiadomości.").addField("Błąd", throwable.getMessage())).respond();
                        return null;
                    });
        }
    }
}
