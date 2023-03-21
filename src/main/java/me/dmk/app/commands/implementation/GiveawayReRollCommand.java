package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.utils.StringUtil;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

import java.util.List;
import java.util.Optional;

/**
 * Created by DMK on 06.12.2022
 */

public class GiveawayReRollCommand extends Command {

    private final GiveawayController giveawayController;

    public GiveawayReRollCommand(String commandName, String commandDescription, GiveawayController giveawayController) {
        super(commandName, commandDescription);

        this.giveawayController = giveawayController;

        this.setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR);
        this.addOptions(
                SlashCommandOption.createStringOption("messageId", "ID wiadomości z konkursem", true),
                SlashCommandOption.createLongOption("winners", "Ilość zwyciężców", true)
        );
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        String messageId = interaction.getArgumentStringValueByName("messageId").orElseThrow();
        int winners = interaction.getArgumentLongValueByName("winners").orElseThrow().intValue();

        if (interaction.getChannel().isEmpty()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Ta komenda może zostać użyta tylko na kanałach tekstowych.")).respond();
            return;
        }

        if (!interaction.getChannel().get().canYouWrite()) {
            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam uprawnień do wysyłania wiadomości.")).respond();
            return;
        }

        final TextChannel textChannel = interaction.getChannel().get();

        textChannel.getMessageById(messageId)
                .thenAcceptAsync(message -> {
                    Optional<Giveaway> giveawayOptional = this.giveawayController.get(message.getId());
                    if (giveawayOptional.isEmpty()) {
                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Konkurs o podanej wiadomości nie istnieje.")).respond();
                        return;
                    }

                    final Giveaway giveaway = giveawayOptional.get();

                    if (!giveaway.isEnded()) {
                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Konkurs jeszcze trwa.")).respond();
                        return;
                    }

                    if (giveaway.getUsers().size() < winners) {
                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Podano większa liczbę od użytkowników, którzy wzieli udział w konkursie.")).respond();
                        return;
                    }

                    List<String> winnersList = this.giveawayController.selectWinners(giveaway, winners);

                    new MessageBuilder()
                            .setContent("Gratulacje " + String.join(", ", winnersList) + " " + (winnersList.size() > 1 ? "wygraliście" : "wygrałeś(-aś)") + " w ponownym losowaniu konkursu!")
                            .replyTo(message)
                            .send(textChannel)
                            .thenAcceptAsync(msg ->
                                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Rozlosowano nowych zwyciężców [konkursu](" + StringUtil.createJumpMessageUrl(server, msg) + ").")).respond())
                            .exceptionallyAsync(throwable -> {
                                interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd poczas wysyłania wiadomości.").addField("Błąd", throwable.getMessage())).respond();
                                return null;
                            });
                })
                .exceptionallyAsync(throwable -> {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie znaleziono wiadomości.")).respond();
                    return null;
                });
    }
}
