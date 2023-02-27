package me.dmk.app.embed;

import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.awt.*;
import java.util.Optional;

/**
 * Created by DMK on 06.12.2022
 */

public class EmbedMessage {

    private final Server server;
    private final EmbedBuilder embedBuilder;

    private final Color defaultColor = new Color(255, 255, 255);
    private final Color successColor = new Color(50, 255, 0);
    private final Color warningColor = new Color(255, 150, 0);
    private final Color errorColor = new Color(255, 0, 0);
    private final Color logColor = new Color(255, 255, 0);
    private final Color giveawayColor = new Color(255, 0, 240);

    public EmbedMessage(Server server) {
        this.server = server;
        this.embedBuilder = new EmbedBuilder().setTimestampToNow();

        server.getIcon().ifPresentOrElse(icon -> this.embedBuilder.setFooter(server.getName(), icon),
                () -> this.embedBuilder.setFooter(server.getName()));
    }

    public EmbedBuilder defaultEmbed() {
        this.embedBuilder.setColor(this.defaultColor);
        return this.embedBuilder;
    }

    public EmbedBuilder success() {
        Optional<KnownCustomEmoji> successEmoji = this.server.getCustomEmojisByNameIgnoreCase("success").stream().findFirst();

        this.embedBuilder.setTitle((successEmoji.map(KnownCustomEmoji::getMentionTag).orElse("✅")) + " Wykonano!");
        this.embedBuilder.setColor(this.successColor);

        return this.embedBuilder;
    }

    public EmbedBuilder warning() {
        Optional<KnownCustomEmoji> successEmoji = this.server.getCustomEmojisByNameIgnoreCase("warning").stream().findFirst();

        this.embedBuilder.setTitle((successEmoji.map(KnownCustomEmoji::getMentionTag).orElse("⚠")) + " Ostrzeżenie!");
        this.embedBuilder.setColor(this.warningColor);

        return this.embedBuilder;
    }

    public EmbedBuilder error() {
        Optional<KnownCustomEmoji> successEmoji = this.server.getCustomEmojisByNameIgnoreCase("error").stream().findFirst();

        this.embedBuilder.setTitle((successEmoji.map(KnownCustomEmoji::getMentionTag).orElse("❌")) + " Błąd!");
        this.embedBuilder.setColor(this.errorColor);

        return this.embedBuilder;
    }

    public EmbedBuilder log() {
        Optional<KnownCustomEmoji> logEmoji = this.server.getCustomEmojiById("log").stream().findFirst();

        this.embedBuilder.setTitle((logEmoji.map(KnownCustomEmoji::getMentionTag).orElse("\uD83D\uDCDA")) + " Nowe zdarzenie!");
        this.embedBuilder.setColor(this.logColor);

        return this.embedBuilder;
    }

    public EmbedBuilder giveaway() {
        Optional<KnownCustomEmoji> successEmoji = this.server.getCustomEmojisByNameIgnoreCase("giveaway").stream().findFirst();

        this.embedBuilder.setTitle((successEmoji.map(KnownCustomEmoji::getMentionTag).orElse("\uD83C\uDF89")) + " Konkurs!");
        this.embedBuilder.setColor(this.giveawayColor);

        return this.embedBuilder;
    }
}
