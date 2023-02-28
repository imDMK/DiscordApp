package me.dmk.app.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.dmk.app.commands.implementation.*;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.serversettings.ServerSettingsController;
import me.dmk.app.warn.WarnController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DMK on 07.12.2022
 */

@RequiredArgsConstructor
public class CommandController {

    @Getter
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    private final DiscordApi discordApi;
    private final GiveawayController giveawayController;
    private final ServerSettingsController serverSettingsController;
    private final WarnController warnController;

    public void registerCommands() {
        Command banCommand = new BanCommand("ban", "Zbanuj użytkownika");
        Command giveawayCommand = new GiveawayCommand("giveaway", "Stwórz konkurs", this.giveawayController);
        Command giveawayReRollCommand = new GiveawayReRollCommand("giveawayreroll", "Rozlosuj ponownie zwyciężców konkursu", this.giveawayController);
        Command messageCommand = new MessageCommand("message", "Wyślij wiadomość");
        Command settingsCommand = new SettingsCommand("settings", "Konfiguracja bota", this.serverSettingsController, this.discordApi);
        Command unBanCommand = new UnBanCommand("unban", "Odbanuj użytkownika");
        Command warnCommand = new WarnCommand("warn", "Nadaj ostrzeżenie dla użytkownika", this.warnController, this.serverSettingsController);
        Command warnListCommand = new WarnListCommand("warnlist", "Sprawdź ostrzeżenia użytkownika", this.warnController);

        this.putCommand(
                banCommand,
                giveawayCommand,
                giveawayReRollCommand,
                messageCommand,
                settingsCommand,
                unBanCommand,
                warnCommand,
                warnListCommand
        );

        SlashCommand.with(banCommand.getCommandName(), banCommand.getCommandDescription())
                .addOption(SlashCommandOption.createUserOption("user", "Wskaż użytkownika", true))
                .addOption(SlashCommandOption.createStringOption("reason", "Podaj powód", false))
                .addOption(SlashCommandOption.createBooleanOption("deleteMessages", "Czy usunąć wiadomości użytkownika?", false))
                .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
                .createGlobal(this.discordApi);

        SlashCommand.with(giveawayCommand.getCommandName(), giveawayCommand.getCommandDescription())
                .addOption(SlashCommandOption.createChannelOption("channel", "Wskaż kanał tekstowy", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL)))
                .addOption(SlashCommandOption.createStringOption("award", "Wpisz nagrodę", true))
                .addOption(SlashCommandOption.createLongOption("winners", "Podaj ilość zwyciężców", true))
                .addOption(SlashCommandOption.createStringOption("expire", "Podaj czas trwania konkursu (np. 7d)", true))
                .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
                .createGlobal(this.discordApi);

        SlashCommand.with(giveawayReRollCommand.getCommandName(), giveawayReRollCommand.getCommandDescription())
                .addOption(SlashCommandOption.createStringOption("messageId", "Podaj ID wiadomości z konkursem", true))
                .addOption(SlashCommandOption.createLongOption("winners", "Podaj ilość zwyciężców", true))
                .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
                .createGlobal(this.discordApi);

        SlashCommand.with(messageCommand.getCommandName(), messageCommand.getCommandDescription())
                .addOption(SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "action", "Wybierz rodzaj interakcji", true,
                        Arrays.asList(
                                SlashCommandOptionChoice.create("ticket", "ticket"),
                                SlashCommandOptionChoice.create("send", "send")
                        )))
                .addOption(SlashCommandOption.createChannelOption("channel", "Wskaż kanał tekstowy", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL)))
                .addOption(SlashCommandOption.createStringOption("message", "Wpisz treść wiadomości (użyj {NL} do nowej linii w wiadomości embed)", true))
                .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
                .createGlobal(this.discordApi);

        SlashCommand.with(settingsCommand.getCommandName(), settingsCommand.getCommandDescription())
                .addOption(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "show", "Pokaż aktualne ustawienia"))
                .addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "edit", "Edytuj ustawienia",
                        Arrays.asList(
                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "welcomechannel", "Zmień kanał powitalny",
                                        Collections.singletonList(
                                                SlashCommandOption.createChannelOption("channel", "Oznacz kanał", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL))
                                        )
                                ),

                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "logschannel", "Zmień kanał logów",
                                        Collections.singletonList(
                                                SlashCommandOption.createChannelOption("channel", "Oznacz kanał", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL)))
                                ),

                                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "maxwarns", "Zmień wartość maksymalnych ostrzeżeń",
                                        Collections.singletonList(
                                                SlashCommandOption.createLongOption("value", "Wpisz nową ilość maksymalnych ostrzeżeń", true)
                                        )
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
                ))
                .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
                .createGlobal(this.discordApi);

        SlashCommand.with(unBanCommand.getCommandName(), unBanCommand.getCommandDescription())
                .addOption(SlashCommandOption.createStringOption("user", "Wpisz ID lub NICK użytkownika", true))
                .setDefaultEnabledForPermissions(PermissionType.BAN_MEMBERS)
                .createGlobal(this.discordApi);

        SlashCommand.with(warnCommand.getCommandName(), warnListCommand.getCommandDescription())
                .addOption(SlashCommandOption.createUserOption("user", "Wskaż użytkownika", true))
                .addOption(SlashCommandOption.createStringOption("reason", "Podaj powód", false))
                .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
                .createGlobal(this.discordApi);

        SlashCommand.with(warnListCommand.getCommandName(), warnListCommand.getCommandDescription())
                .addOption(SlashCommandOption.createUserOption("user", "Wskaż użytkownika", true))
                .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
                .createGlobal(this.discordApi);
    }

    public void putCommand(Command... commands) {
        for (Command command : commands) {
            this.commands.put(command.getCommandName(), command);
        }
    }

    public Optional<Command> getCommand(String commandName) {
        return Optional.ofNullable(this.commands.get(commandName));
    }
}
