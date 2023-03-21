package me.dmk.app.commands.implementation;

import me.dmk.app.commands.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.serversettings.ServerSettings;
import me.dmk.app.serversettings.ServerSettingsController;
import me.dmk.app.utils.StringUtil;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by DMK on 06.12.2022
 */

public class SettingsCommand extends Command {

    private final ServerSettingsController serverSettingsController;
    private final DiscordApi discordApi;

    public SettingsCommand(String commandName, String commandDescription, ServerSettingsController serverSettingsController, DiscordApi discordApi) {
        super(commandName, commandDescription);

        this.serverSettingsController = serverSettingsController;
        this.discordApi = discordApi;

        this.setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR);
        this.addOptions(
                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "edit", "Edytuj ustawienia",
                        Arrays.asList(
                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "welcomechannel", "Zmień kanał powitalny",
                                        Collections.singletonList(
                                                SlashCommandOption.createChannelOption("channel", "Oznacz kanał", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL))
                                        )
                                ),

                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "logschannel", "Zmień kanał logów",
                                        Collections.singletonList(
                                                SlashCommandOption.createChannelOption("channel", "Oznacz kanał", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL))
                                        )
                                ),

                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "maxwarns", "Zmień wartość maksymalnych ostrzeżeń",
                                        Collections.singletonList(SlashCommandOption.createLongOption("value", "Wpisz nową ilość maksymalnych ostrzeżeń", true))
                                ),

                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "welcomeroles", "Zmień role powitalne",
                                        Arrays.asList(
                                                SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "action", "Wybierz akcję", true,
                                                        Arrays.asList(
                                                                SlashCommandOptionChoice.create("add", "add"),
                                                                SlashCommandOptionChoice.create("remove", "remove")
                                                        )
                                                ),
                                                SlashCommandOption.createRoleOption("role", "Oznacz rolę", true)
                                        )
                                ),

                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "ticketscategory", "Kategoria zgłoszeń",
                                        Collections.singletonList(
                                                SlashCommandOption.createStringOption("category", "Podaj ID kategorii", true)
                                        )
                                ),

                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "ticketsrolechecker", "Rola administracji zgłoszeń",
                                        Collections.singletonList(
                                                SlashCommandOption.createRoleOption("role", "Oznacz rolę", true)
                                        )
                                )
                        )
                )
        );
    }

    @Override
    public void execute(Server server, SlashCommandInteraction interaction) {
        Optional<ServerSettings> serverSettingsOptional = serverSettingsController.get(server.getId());

        if (serverSettingsOptional.isEmpty()) {
            serverSettingsController.create(server.getId())
                    .thenAcceptAsync(serverSettings ->
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Stworzono serwerową konfigurację.")).respond())
                    .exceptionallyAsync(throwable -> {
                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas próby stworzenia ustawień serwerowych.").addField("Błąd", throwable.getMessage())).respond();
                        return null;
                    });
            return;
        }

        final ServerSettings settings = serverSettingsOptional.get();

        if (interaction.getOptionByName("show").isPresent()) {
            List<Long> welcomeRoles = settings.getWelcomeRoles();
            List<String> welcomeRolesList = new LinkedList<>();

            welcomeRoles.forEach(roleId -> server.getRoleById(roleId)
                    .ifPresentOrElse(role ->
                                    welcomeRolesList.add(role.getMentionTag()),
                            () -> welcomeRolesList.add(roleId + "")));

            AtomicReference<String> welcomeChannel = new AtomicReference<>((settings.getWelcomeChannel() == 0L ? "Brak" : "Nieprawidłowy"));
            AtomicReference<String> logsChannel = new AtomicReference<>((settings.getLogsChannel() == 0L ? "Brak" : "Nieprawidłowy"));
            AtomicReference<String> ticketsCategory = new AtomicReference<>((settings.getTicketsCategory() == 0L ? "Brak" : "Nieprawidłowy"));
            AtomicReference<String> ticketsRoleChecker = new AtomicReference<>((settings.getTicketsRoleChecker() == 0L ? "Brak" : "Nieprawidłowy"));

            server.getTextChannelById(settings.getWelcomeChannel())
                    .ifPresent(serverTextChannel -> welcomeChannel.set(serverTextChannel.getMentionTag()));

            server.getTextChannelById(settings.getLogsChannel())
                    .ifPresent(serverTextChannel -> logsChannel.set(serverTextChannel.getMentionTag()));

            server.getChannelCategoryById(settings.getTicketsCategory())
                    .ifPresent(channelCategory -> ticketsCategory.set(channelCategory.getName()));

            server.getRoleById(settings.getTicketsRoleChecker())
                    .ifPresent(role -> ticketsRoleChecker.set(role.getMentionTag()));

            interaction.createImmediateResponder()
                    .setFlags(MessageFlag.EPHEMERAL)
                    .addEmbed(new EmbedMessage(server).success()
                            .setTitle("⚙️ Aktualna konfiguracja serwera")
                            .addField("Kanał powitalny", welcomeChannel.get())
                            .addField("Kanał logów", logsChannel.get())
                            .addField("Maksymalna ilość ostrzeżeń", String.valueOf(settings.getMaximumWarns()))
                            .addField("Role powitalne", (welcomeRolesList.isEmpty() ? "Brak" : String.join(", ", welcomeRolesList)))
                            .addField("Kategoria zgłoszeń", ticketsCategory.get())
                            .addField("Rola administracyjna zgłoszeń", ticketsRoleChecker.get())
                    )
                    .respond();
            return;
        }

        SlashCommandInteractionOption editSettingType = interaction.getOptionByName("edit")
                .orElseThrow()
                .getOptionByIndex(0)
                .orElseThrow();

        switch (editSettingType.getName().toLowerCase()) {
            case "welcomechannel" -> {
                Optional<ServerChannel> serverChannel = editSettingType.getArgumentChannelValueByName("channel");

                if (serverChannel.isEmpty() || serverChannel.get().asServerTextChannel().isEmpty()) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Oznaczono nieprawidłowy kanał (Kanał musi być typem kanału tekstowego).")).respond();
                    return;
                }

                if (settings.getWelcomeChannel() == serverChannel.get().getId()) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Ten kanał jest już ustawiony jako kanał powitalny.")).respond();
                    return;
                }

                settings.setWelcomeChannel(serverChannel.get().getId());
                updateSettingsAndRespond(settings, server, interaction);
            }

            case "logschannel" -> {
                Optional<ServerChannel> serverChannel = editSettingType.getArgumentChannelValueByName("channel");

                if (serverChannel.isEmpty() || serverChannel.get().asServerTextChannel().isEmpty()) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Oznaczono nieprawidłowy kanał (Kanał musi być typem kanału tekstowego).")).respond();
                    return;
                }

                if (settings.getWelcomeChannel() == serverChannel.get().getId()) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Ten kanał jest już ustawiony jako kanał logów.")).respond();
                    return;
                }

                settings.setLogsChannel(serverChannel.get().getId());
                updateSettingsAndRespond(settings, server, interaction);
            }

            case "maxwarns" -> {
                int maxwarns = editSettingType.getArgumentLongValueByName("value").orElseThrow().intValue();

                if (settings.getMaximumWarns() == maxwarns) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Podana wartość " + maxwarns + " jest już ustawiona w konfiguracji.")).respond();
                    return;
                }

                settings.setMaximumWarns(maxwarns);
                updateSettingsAndRespond(settings, server, interaction);
            }

            case "welcomeroles" -> {
                String actionType = editSettingType.getArgumentStringValueByName("action").orElseThrow();
                Role role = editSettingType.getArgumentRoleValueByName("role").orElseThrow();

                if (role.isEveryoneRole() || role.isManaged()) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Oznaczono rolę domyślną lub automatycznie zarządzaną. Ta nie może być nadawana ani edytowana.")).respond();
                    return;
                }

                if (!discordApi.getYourself().canManageRole(role)) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam dostępu do zarządzania tą rolą.")).respond();
                    return;
                }

                switch (actionType.toLowerCase()) {
                    case "add" -> {
                        if (settings.getWelcomeRoles().contains(role.getId())) {
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("POdana rola istnieje już w konfiguracji.")).respond();
                            return;
                        }

                        settings.getWelcomeRoles().add(role.getId());
                        updateSettingsAndRespond(settings, server, interaction);
                    }

                    case "remove" -> {
                        if (!settings.getWelcomeRoles().contains(role.getId())) {
                            interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Podana rola w konfiguracji nie istnieje.")).respond();
                            return;
                        }

                        settings.getWelcomeRoles().remove(role.getId());
                        updateSettingsAndRespond(settings, server, interaction);
                    }
                }
            }

            case "ticketscategory" -> {
                String category = editSettingType.getArgumentStringValueByName("category").orElseThrow();

                if (!StringUtil.isLong(category)) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wprowadzono niepoprawne ID kategorii.")).respond();
                    return;
                }

                long categoryId = Long.parseLong(category);

                Optional<ChannelCategory> channelCategory = server.getChannelCategoryById(categoryId);
                if (channelCategory.isEmpty()) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Kategoria o podanym ID nie istnieje.")).respond();
                    return;
                }

                if (settings.getTicketsCategory() == categoryId) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Ta kategoria jest już ustawiona.")).respond();
                    return;
                }

                settings.setTicketsCategory(channelCategory.get().getId());
                updateSettingsAndRespond(settings, server, interaction);
            }

            case "ticketsrolechecker" -> {
                Role role = editSettingType.getArgumentRoleValueByName("role").orElseThrow();

                if (role.isEveryoneRole() || role.isManaged()) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Oznaczono rolę domyślną lub automatycznie zarządzaną. Ta nie może być nadawana ani edytowana.")).respond();
                    return;
                }

                if (!discordApi.getYourself().canManageRole(role)) {
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Nie posiadam dostępu do zarządzania tą rolą.")).respond();
                    return;
                }

                settings.setTicketsRoleChecker(role.getId());
                updateSettingsAndRespond(settings, server, interaction);
            }
        }
    }

    private void updateSettingsAndRespond(ServerSettings serverSettings, Server server, SlashCommandInteraction interaction) {
        serverSettingsController.update(serverSettings)
                .thenAcceptAsync(settings ->
                        interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).success().setDescription("Zaktualizowano ustawienia.")).respond())
                .exceptionallyAsync(throwable -> {
                    throwable.printStackTrace();
                    interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd podczas próbuy zaktualizowania ustawień").addField("Błąd", throwable.getMessage())).respond();
                    return null;
                });
    }
}
