package me.dmk.app.listeners;

import lombok.AllArgsConstructor;
import me.dmk.app.ticket.TicketController;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;
import org.javacord.api.listener.channel.server.ServerChannelDeleteListener;

/**
 * Created by DMK on 24.12.2022
 */

@AllArgsConstructor
public class ChannelListener implements ServerChannelDeleteListener {

    private final TicketController ticketController;

    @Override
    public void onServerChannelDelete(ServerChannelDeleteEvent event) {
        ServerChannel serverChannel = event.getChannel();

        this.ticketController.get(serverChannel).ifPresent(ticketController::delete);
    }
}
