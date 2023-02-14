package me.dmk.app.listeners;

import lombok.AllArgsConstructor;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.GiveawayController;
import me.dmk.app.log.LogMessage;
import me.dmk.app.serversettings.ServerSettingsController;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.MessageEditListener;

import java.util.Optional;

/**
 * Created by DMK on 08.12.2022
 */

@AllArgsConstructor
public class MessageListener implements MessageEditListener, MessageDeleteListener {

    private final GiveawayController giveawayController;
    private final ServerSettingsController serverSettingsController;

    @Override
    public void onMessageEdit(MessageEditEvent event) {
        if (event.getServer().isEmpty()) {
            return;
        }

        if (event.getMessage().getContent().isEmpty()) {
            return;
        }

        if (event.getOldMessage().isEmpty()) {
            return;
        }

        Server server = event.getServer().get();
        Message message = event.getMessage();
        MessageAuthor messageAuthor = event.getMessageAuthor();
        Optional<Message> oldMessage = event.getOldMessage();

        if (!messageAuthor.isUser()) {
            return;
        }

        String oldContent = oldMessage.get().getContent();
        String newContent = event.getMessage().getContent();

        this.serverSettingsController.get(server.getId())
                .ifPresent(settings ->
                        new LogMessage(server, settings).send(new EmbedMessage(server).log()
                                .setDescription("Akcja: **Zedytowanie wiadomości**")
                                .addField("Użytkownik", "<@" + messageAuthor.getId() + ">")
                                .addField("Kanał", "<#" + message.getChannel().getId() + ">")
                                .addField("Wiadomość", oldContent + " -> " + newContent)
                        )
                );
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (event.getServer().isEmpty() || event.getMessage().isEmpty()) {
            return;
        }

        Server server = event.getServer().get();
        Message message = event.getMessage().get();

        this.giveawayController.get(message.getId()).ifPresent(this.giveawayController::delete);

        if (!message.getAuthor().isUser()) {
            return;
        }

        this.serverSettingsController.get(server.getId())
                .ifPresent(settings ->
                        new LogMessage(server, settings).send(new EmbedMessage(server).log()
                                .setDescription("Akcja: **Usunięcie wiadomości**")
                                .addField("Kanał", "<#" + message.getChannel().getId() + ">")
                                .addField("Wiadomość", message.getContent())
                        )
                );
    }
}
