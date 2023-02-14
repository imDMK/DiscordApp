package me.dmk.app.utils;

import lombok.experimental.UtilityClass;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

/**
 * Created by DMK on 07.12.2022
 */

@UtilityClass
public class StringUtil {

    public static boolean isLong(String string) {
        try {
            Long.parseLong(string);
            return true;
        } catch (NumberFormatException numberFormatException) {
            return false;
        }
    }

    public static EmbedBuilder getGiveawayMessageTemplate(Server server, Giveaway giveaway) {
        return new EmbedMessage(server).giveaway()
                .addField("Nagroda", "**" + giveaway.getWinners() + "x** " + giveaway.getAward())
                .addField("Zakończy się", "<t:" + giveaway.getExpire().toInstant().getEpochSecond() + ":R>")
                .addField("Uczestnicy", (giveaway.getUsers().isEmpty() ? "Brak" : String.valueOf(giveaway.getUsers().size())));
    }

    public static String createJumpMessageUrl(Server server, Message message) {
        return String.format("https://discordapp.com/channels/%s/%s/%s", server.getIdAsString(), message.getChannel().getIdAsString(), message.getIdAsString());
    }
}
