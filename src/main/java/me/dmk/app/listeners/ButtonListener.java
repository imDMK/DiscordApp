package me.dmk.app.listeners;

import lombok.AllArgsConstructor;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.serversettings.ServerSettings;
import me.dmk.app.serversettings.ServerSettingsController;
import me.dmk.app.ticket.Ticket;
import me.dmk.app.ticket.TicketController;
import me.dmk.app.warn.Warn;
import me.dmk.app.warn.WarnController;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.ActionRowBuilder;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.listener.interaction.ButtonClickListener;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Created by DMK on 08.12.2022
 */

@AllArgsConstructor
public class ButtonListener implements ButtonClickListener {

    private final GiveawayController giveawayController;
    private final ServerSettingsController serverSettingsController;
    private final TicketController ticketController;
    private final WarnController warnController;

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        ButtonInteraction interaction = event.getButtonInteraction();
        String customId = interaction.getCustomId();
        Message message = interaction.getMessage();
        User user = interaction.getUser();

        Optional<Server> serverOptional = interaction.getServer();
        Optional<TextChannel> textChannelOptional = interaction.getChannel();

        if (serverOptional.isEmpty() || textChannelOptional.isEmpty() || textChannelOptional.get().asServerTextChannel().isEmpty()) {
            return;
        }

        final Server server = serverOptional.get();
        final ServerTextChannel serverTextChannel = textChannelOptional.get().asServerTextChannel().get();

        boolean checkWarnId = customId.startsWith("check-warn");
        boolean deleteId = customId.startsWith("delete-warn");

        boolean warnsListId = customId.startsWith("warns-list");
        boolean warnsList2Id = customId.startsWith("warns-list-2");
        boolean giveawayLeaveConfirmId = customId.startsWith("giveaway-leave-confirm");

        if (checkWarnId || deleteId) {
            String identifier = customId.substring(customId.length() - 8);

            Optional<Warn> warnOptional = this.warnController.get(identifier);
            if (warnOptional.isEmpty()) {
                interaction.createImmediateResponder().respond();
                return;
            }

            final Warn warn = warnOptional.get();

            if (checkWarnId) {
                interaction.createOriginalMessageUpdater()
                        .setFlags(MessageFlag.EPHEMERAL)
                        .addEmbed(new EmbedMessage(server).success()
                                .setTitle(warn.getIdentifier())
                                .setDescription("Informacje o ostrzeżeniu użytkownika <@" + warn.getUser() + ">")
                                .addField("Administrator", "<@" + warn.getAdmin() + ">")
                                .addField("Powód", warn.getReason())
                                .addField("Data", "<t:" + warn.getCreated().toInstant().getEpochSecond() + ":f>")
                        )
                        .addComponents(ActionRow.of(
                                Button.secondary("warns-list-" + warn.getUser(), "Powrót", "⬅"),
                                Button.danger("delete-warn-" + warn.getIdentifier(), "Usuń", "⛔")
                        ))
                        .update();
            } else {
                if (this.warnController.delete(warn)) {
                    interaction.createOriginalMessageUpdater()
                            .addEmbed(new EmbedMessage(server).success().setDescription("Usunięto ostrzeżenie."))
                            .addComponents(ActionRow.of(Button.secondary("warns-list-" + warn.getUser(), "Powrót", "⬅")))
                            .update();
                } else {
                    interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd.")).update();
                }
            }
        }

        if (warnsListId || warnsList2Id || giveawayLeaveConfirmId) {
            String[] args = customId.split("-");

            if (warnsListId && !warnsList2Id) {
                long userId = Long.parseLong(args[2]);

                List<Warn> warnList = this.warnController.gets(userId)
                        .stream()
                        .sorted(Comparator.comparing(Warn::getCreated).reversed())
                        .toList();

                if (warnList.isEmpty()) {
                    interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).success().setDescription("Użytkownik nie posiada ostrzeżeń.")).update();
                    return;
                }

                EmbedBuilder embedBuilder = new EmbedMessage(server).success().setDescription("Użytkownik <@" + userId + "> posiada **" + warnList.size() + "** ostrzeżeń");
                ActionRowBuilder actionRowBuilder = new ActionRowBuilder();

                if (warnList.size() > 4) {
                    for (Warn warn : warnList.stream().limit(4).toList()) {
                        embedBuilder.addField(warn.getIdentifier(), warn.getReason());
                        actionRowBuilder.addComponents(Button.secondary("check-warn-" + warn.getIdentifier(), warn.getIdentifier()));
                    }
                    actionRowBuilder.addComponents(Button.primary("warns-list-2-" + userId, "Następna strona", "➡"));
                } else {
                    for (Warn warn : warnList) {
                        embedBuilder.addField(warn.getIdentifier(), warn.getReason());
                        actionRowBuilder.addComponents(Button.secondary("check-warn-" + warn.getIdentifier(), warn.getIdentifier()));
                    }
                }

                interaction.createOriginalMessageUpdater()
                        .setFlags(MessageFlag.EPHEMERAL)
                        .addEmbed(embedBuilder)
                        .addComponents(actionRowBuilder.build())
                        .update();

            } else if (warnsList2Id) {
                long userId = Long.parseLong(args[3]);

                List<Warn> warnList = this.warnController.gets(userId)
                        .stream()
                        .sorted(Comparator.comparing(Warn::getCreated).reversed())
                        .toList();

                if (warnList.isEmpty()) {
                    interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).success().setDescription("Użytkownik nie posiada ostrzeżeń.")).update();
                    return;
                }

                List<Warn> warns = warnList.subList(4, warnList.size());
                if (warns.isEmpty()) {
                    interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).success().setDescription("Użytkownik nie posiada drugiej listy ostrzeżeń.")).update();
                    return;
                }

                EmbedBuilder embedBuilder = new EmbedMessage(server).success().setDescription("Użytkownik <@" + userId + "> posiada **" + warnList.size() + "** ostrzeżeń (strona: 2/2)");
                ActionRowBuilder actionRowBuilder = new ActionRowBuilder();

                if (warns.size() < 5) {
                    actionRowBuilder.addComponents(
                            Button.primary("warns-list-" + userId, "Powrót", "⬅")
                    );
                }

                for (Warn warn : warns) {
                    embedBuilder.addField(warn.getIdentifier(), warn.getReason());
                    actionRowBuilder.addComponents(
                            Button.secondary("check-warn-" + warn.getIdentifier(), warn.getIdentifier())
                    );
                }

                interaction.createOriginalMessageUpdater()
                        .addEmbed(embedBuilder)
                        .addComponents(actionRowBuilder.build())
                        .update();

            } else {
                long messageId = Long.parseLong(args[3]);

                this.giveawayController.get(messageId)
                        .ifPresentOrElse(giveaway ->
                                this.giveawayController.removeUser(user, giveaway)
                                        .thenAcceptAsync(g ->
                                                interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).success().setDescription("Opuszczono konkurs.")).update())
                                        .exceptionallyAsync(throwable -> {
                                            interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd.").addField("Błąd", throwable.getMessage())).update();
                                            return null;
                                        }), () -> interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił nieoczekiwany błąd.")).update());
            }
        }

        switch (customId) {
            case "giveaway-join" -> this.giveawayController.get(message.getId()).ifPresentOrElse(giveaway -> {
                if (giveaway.getUsers().contains(user.getId())) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL)
                            .addEmbed(new EmbedMessage(server).warning().setDescription("Aktualnie bierzesz udział w tym konkursie, Czy chcesz go opuścić?"))
                            .addComponents(ActionRow.of(
                                    Button.success("giveaway-leave-confirm-" + message.getId(), "Tak", "✅"))
                            )
                            .respond();
                    return;
                }

                this.giveawayController.addUser(user, giveaway)
                        .thenAcceptAsync(g -> interaction.createImmediateResponder().respond())
                        .exceptionallyAsync(throwable -> {
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd.").addField("Błąd", throwable.getMessage())).respond();
                            return null;
                        });
            }, () -> interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie znaleziono konkursu.")).respond());

            case "ticket-create" -> {
                if (!server.canYouCreateChannels() || !server.canYouManageRoles()) {
                    interaction.createImmediateResponder().addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam uprawnień.")).respond();
                    return;
                }

                Optional<ServerSettings> settingsOptional = this.serverSettingsController.get(server.getId());
                Optional<Ticket> ticketOptional = this.ticketController.get(user);

                if (ticketOptional.isPresent()) {
                    server.getTextChannelById(ticketOptional.get().getChannel())
                            .ifPresentOrElse(textChannel ->
                                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Posiadasz aktywne zgłoszenie (" + textChannel.getMentionTag() + ").")).respond(), () -> {

                                interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Spróbuj ponownie.")).respond();
                                this.ticketController.delete(ticketOptional.get());
                            });
                    return;
                }

                MessageBuilder messageBuilder = new MessageBuilder()
                        .setContent(user.getMentionTag())
                        .setEmbed(new EmbedMessage(server).warning()
                                .setTitle("✉️ Centrum pomocy #" + this.ticketController.getTicketMap().size() + 1)
                                .setDescription("Dziękujemy za kontakt z nami.\nOpisz nam powód kontaktu z nami, a my postaramy się odpowiedzieć jak najszybciej to możliwe.")
                        )
                        .addComponents(ActionRow.of(
                                Button.danger("ticket-close", "Zamknij", "⛔")
                        ));

                ServerTextChannelBuilder textChannelBuilder = new ServerTextChannelBuilder(server)
                        .setName("ticket-" + user.getDiscriminatedName())
                        .addPermissionOverwrite(server.getEveryoneRole(), new PermissionsBuilder().setAllDenied().build())
                        .addPermissionOverwrite(user, new PermissionsBuilder().setAllowed(PermissionType.SEND_MESSAGES, PermissionType.READ_MESSAGE_HISTORY).build())
                        .setSlowmodeDelayInSeconds(2);

                settingsOptional.ifPresent(serverSettings -> {
                    server.getChannelCategoryById(settingsOptional.get().getTicketsCategory())
                            .ifPresent(textChannelBuilder::setCategory);

                    server.getRoleById(settingsOptional.get().getTicketsRoleChecker())
                            .ifPresent(role -> textChannelBuilder.addPermissionOverwrite(role, new PermissionsBuilder().setAllowed(PermissionType.SEND_MESSAGES, PermissionType.READ_MESSAGE_HISTORY).build()));
                });

                textChannelBuilder.create()
                        .thenAcceptAsync(channel -> {
                            Ticket ticket = new Ticket(server, channel, user);

                            this.ticketController.create(ticket)
                                    .thenAcceptAsync(t -> {
                                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Twoje zgłoszenie zostało stworzone (" + channel.getMentionTag() + ").")).respond();

                                        messageBuilder.send(channel);
                                    })
                                    .exceptionallyAsync(throwable -> {
                                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas tworzenia zgłoszenia.").addField("Błąd", throwable.getMessage())).respond();

                                        channel.delete("Error while trying to create ticket user " + user.getName());
                                        return null;
                                    });
                        })
                        .exceptionallyAsync(throwable -> {
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd poczas tworzenia kanału.").addField("Błąd", throwable.getMessage())).respond();
                            return null;
                        });
            }

            case "ticket-close" -> this.ticketController.get(serverTextChannel).ifPresent(ticket ->
                    interaction.createImmediateResponder()
                            .addEmbed(new EmbedMessage(server).warning().setDescription("Czy na pewno chcesz zamknąć zgłoszenie?"))
                            .addComponents(ActionRow.of(Button.success("ticket-close-confirmed", "Tak", "✅"), Button.danger("ticket-close-canceled", "Anuluj", "⛔")))
                            .respond()
            );

            case "ticket-close-confirmed" -> this.ticketController.get(serverTextChannel).ifPresent(ticket -> {
                if (!server.canYouCreateChannels()) {
                    interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam uprawnień do zarządzania kanałami.")).update();
                    return;
                }

                Optional<User> userTicket = server.getMemberById(ticket.getUser());

                if (this.ticketController.delete(ticket)) {
                    ServerTextChannelUpdater serverTextChannelUpdater = serverTextChannel
                            .createUpdater()
                            .unsetSlowmode();

                    userTicket.ifPresent(u -> serverTextChannelUpdater
                            .setName("closed - " + u.getDiscriminatedName())
                            .addPermissionOverwrite(u, new PermissionsBuilder().setAllDenied().build()));

                    serverTextChannelUpdater
                            .update()
                            .thenAcceptAsync(unused ->
                                    interaction.createOriginalMessageUpdater()
                                            .addEmbed(new EmbedMessage(server).success().setDescription("Zgłoszenie zostało zamknięte." + (userTicket.isEmpty() ? "\nUżytkownik opuścił serwer - nazwa kanału nie została zaktualizowana." : "")))
                                            .addComponents(ActionRow.of(
                                                    Button.primary("channel-delete", "Usuń", "⛔")
                                            ))
                                            .update()
                            )
                            .exceptionallyAsync(throwable -> {
                                interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd poczas aktualizowania kanału.").addField("Błąd", throwable.getMessage())).update();
                                return null;
                            });
                } else {
                    interaction.createOriginalMessageUpdater().addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas usuwania zgłoszenia.")).update();
                }
            });

            case "message-delete", "ticket-close-canceled" -> message.delete();
            case "channel-delete" -> serverTextChannel.delete();
        }
    }
}
