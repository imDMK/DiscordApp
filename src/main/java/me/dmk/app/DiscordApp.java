package me.dmk.app;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.hjson.HjsonConfigurer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.commands.CommandService;
import me.dmk.app.configuration.ClientConfiguration;
import me.dmk.app.database.MongoService;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.listeners.ButtonListener;
import me.dmk.app.listeners.ChannelListener;
import me.dmk.app.listeners.CommandListener;
import me.dmk.app.listeners.MessageListener;
import me.dmk.app.schedulers.GiveawayScheduler;
import me.dmk.app.serversettings.ServerSettingsController;
import me.dmk.app.ticket.TicketController;
import me.dmk.app.warn.WarnController;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.UserStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by DMK on 06.12.2022
 */

@Slf4j
@Getter
public class DiscordApp {

    private final ClientConfiguration configuration;
    private final DiscordApi discordApi;

    private final MongoService mongoService;

    private final GiveawayController giveawayController;
    private final ServerSettingsController serverSettingsController;
    private final TicketController ticketController;
    private final WarnController warnController;
    private final CommandService commandService;

    private final ScheduledExecutorService executorService;

    protected DiscordApp() {
        long start = System.currentTimeMillis();

        this.configuration = ConfigManager.create(ClientConfiguration.class, (it) -> {
            it.withConfigurer(new HjsonConfigurer());
            it.withBindFile("config.hjson");
            it.saveDefaults();
            it.load(true);
        });

        this.discordApi = new DiscordApiBuilder()
                .setToken(this.configuration.getToken())
                .setAllIntents()
                .setWaitForUsersOnStartup(true)
                .setWaitForServersOnStartup(true)
                .login().join();

        /* Services */
        this.mongoService = new MongoService(this.configuration.databaseConfiguration);
        this.mongoService.connect();

        /* Controllers */
        this.giveawayController = new GiveawayController(this.mongoService, this.discordApi);
        this.giveawayController.load();

        this.serverSettingsController = new ServerSettingsController(this.mongoService, this.discordApi);
        this.serverSettingsController.load();

        this.ticketController = new TicketController(this.mongoService, this.discordApi);
        this.ticketController.load();

        this.warnController = new WarnController(this.mongoService, this.discordApi);
        this.warnController.load();

        log.info("Loaded all services. Loading commands...");

        this.commandService = new CommandService(this.discordApi, this.giveawayController, this.serverSettingsController, this.warnController);
        this.commandService.registerCommands();

        log.info("Loaded all commands.");

        /* Listeners */
        this.discordApi.addSlashCommandCreateListener(new CommandListener(this.commandService));
        this.discordApi.addButtonClickListener(new ButtonListener(this.giveawayController, this.serverSettingsController, this.ticketController, this.warnController));
        this.discordApi.addServerChannelDeleteListener(new ChannelListener(this.ticketController));
        this.discordApi.addMessageEditListener(new MessageListener(this.giveawayController, this.serverSettingsController));
        this.discordApi.addMessageDeleteListener(new MessageListener(this.giveawayController, this.serverSettingsController));

        /* Schedulers */
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.scheduleAtFixedRate(new GiveawayScheduler(this.discordApi, this.giveawayController), 1L, 3L, TimeUnit.SECONDS);

        long elapsedTime = start - System.currentTimeMillis();
        log.info("The bot has been successfully turned on (time elapsed: " + elapsedTime + " ms).");

        this.discordApi.updateStatus(UserStatus.ONLINE);
        if (this.discordApi.getServers().isEmpty()) {
            log.info("I'm not on any server! Invite me using: " + discordApi.createBotInvite());
        }
    }
}
