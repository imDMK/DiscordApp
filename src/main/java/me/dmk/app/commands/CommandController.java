package me.dmk.app.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.dmk.app.commands.implementation.*;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.serversettings.ServerSettingsController;
import me.dmk.app.warn.WarnController;
import org.javacord.api.DiscordApi;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DMK on 07.12.2022
 */

@RequiredArgsConstructor
public class CommandController {

    private final DiscordApi discordApi;
    private final GiveawayController giveawayController;
    private final ServerSettingsController serverSettingsController;
    private final WarnController warnController;

    @Getter
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void registerCommands() {
        Command banCommand = new BanCommand("ban", "Zbanuj użytkownika");
        Command giveawayCommand = new GiveawayCommand("giveaway", "Stwórz konkurs", this.giveawayController);
        Command giveawayReRollCommand = new GiveawayReRollCommand("giveawayreroll", "Rozlosuj ponownie zwyciężców konkursu", this.giveawayController);
        Command messageCommand = new MessageCommand("message", "Wyślij wiadomość");
        Command settingsCommand = new SettingsCommand("settings", "Konfiguracja bota", this.serverSettingsController, this.discordApi);
        Command unBanCommand = new UnBanCommand("unban", "Odbanuj użytkownika");
        Command warnCommand = new WarnCommand("warn", "Nadaj ostrzeżenie dla użytkownika", this.warnController, this.serverSettingsController);
        Command warnListCommand = new WarnListCommand("warnlist", "Sprawdź ostrzeżenia użytkownika", this.warnController);

        this.registerCommand(
                banCommand,
                giveawayCommand,
                giveawayReRollCommand,
                messageCommand,
                settingsCommand,
                unBanCommand,
                warnCommand,
                warnListCommand
        );
    }

    public void registerCommand(Command... commands) {
        for (Command command : commands) {
            this.commands.put(command.getName(), command);
        }

        this.discordApi.bulkOverwriteGlobalApplicationCommands(
                Set.of(commands)
        );
    }

    public Optional<Command> getCommand(String commandName) {
        return Optional.ofNullable(this.commands.get(commandName));
    }
}
