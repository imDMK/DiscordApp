package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.utils.StringUtil;
import me.dmk.app.utils.TimeUtil;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

/**
 * Created by DMK on 07.12.2022
 */

public class GiveawayCommand extends Command {

    private final GiveawayController giveawayController;

    public GiveawayCommand(String commandName, String commandDescription, GiveawayController giveawayController) {
        super(commandName, commandDescription);

        this.giveawayController = giveawayController;

        this.setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR);
        this.addOptions(
                SlashCommandOption.createChannelOption("channel", "Wskaż kanał tekstowy", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL)),
                SlashCommandOption.createStringOption("award", "Wpisz nagrodę", true),
                SlashCommandOption.createLongOption("winners", "Podaj ilość zwyciężców", true),
                SlashCommandOption.createStringOption("expire", "Podaj czas trwania konkursu (np. 7d)", true)
        );
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        ServerChannel serverChannel = interaction.getArgumentChannelValueByName("channel").orElseThrow();
        String award = interaction.getArgumentStringValueByName("award").orElseThrow();
        int winners = interaction.getArgumentLongValueByName("winners").orElseThrow().intValue();
        String expire = interaction.getArgumentStringValueByName("expire").orElseThrow();

        if (serverChannel.asServerTextChannel().isEmpty()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Oznaczony kanał musi być tekstowy.")).respond();
            return;
        }

        ServerTextChannel serverTextChannel = serverChannel.asServerTextChannel().orElseThrow();

        if (!serverTextChannel.canYouSee() || !serverTextChannel.canYouWrite()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam uprawnień do oznaczonego kanału.")).respond();
            return;
        }

        if (winners < 1) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Podano nieprawidłową liczbę wygrywających.")).respond();
            return;
        }

        Optional<Instant> expireInstant = TimeUtil.stringToInstant(expire);
        if (expireInstant.isEmpty()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Podano nieprawidłowy czas (np. 1m, 2h, 30d).")).respond();
            return;
        }

        new MessageBuilder()
                .append("Trwa próba stworzenia konkursu...")
                .send(serverTextChannel)
                .thenAcceptAsync(message -> {
                    Giveaway giveaway = new Giveaway(server, message, award, winners, expireInstant.get());

                    this.giveawayController.create(giveaway)
                            .thenAcceptAsync(g -> message.createUpdater()
                                    .setContent(
                                            server.getEveryoneRole().getMentionTag()
                                    )
                                    .setEmbed(
                                            StringUtil.getGiveawayMessageTemplate(server, g)
                                    )
                                    .addComponents(ActionRow.of(
                                            Button.success("giveaway-join", "Dołącz", "\uD83C\uDF89"))
                                    )
                                    .applyChanges()
                                    .thenAcceptAsync(updatedMessage ->
                                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Stworzono nowy [konkurs](" + StringUtil.createJumpMessageUrl(server, updatedMessage) + ").")).respond())
                                    .exceptionallyAsync(throwable -> {
                                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas aktualizowania wiadomości.").addField("Błąd", throwable.getMessage())).respond();
                                        return null;
                                    }))
                            .exceptionallyAsync(throwable -> {
                                throwable.printStackTrace();
                                interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas tworzenia konkursu.").addField("Błąd", throwable.getMessage())).respond();
                                message.delete();
                                return null;
                            });
                })
                .exceptionallyAsync(throwable -> {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas wysyłania wiadomości na kanał.").addField("Błąd", throwable.getMessage())).respond();
                    return null;
                });
    }
}
