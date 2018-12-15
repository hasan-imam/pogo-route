package pogo.assistance.ui.discord;

import javax.security.auth.login.LoginException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * Proof-of-concept class - Currently just sends msg to a toy server. Should be able to post various output to certain
 * discord channels.
 */
@Slf4j
public class PostMessage {

    private static final String M8M_BOT_TOKEN = "NTIxMjUzOTI2NzI5ODA5OTIw.Du6HQA.xdcmHk8AVJOSfsCAaQNtyLHgIXM";

    public void send(final String message) {
        try {
            final JDA discordApi = new JDABuilder(M8M_BOT_TOKEN).build().awaitReady();
            final TextChannel channel = discordApi.getTextChannelById(521262122919919636L);
            channel.sendMessage(message).queue();
        } catch (final InterruptedException | LoginException e) {
           log.error("Failed to initialize JDA", e);
        }
    }

}
