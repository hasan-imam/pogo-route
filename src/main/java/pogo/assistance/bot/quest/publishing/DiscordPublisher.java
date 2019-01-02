package pogo.assistance.bot.quest.publishing;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

@Slf4j
public class DiscordPublisher implements Closeable {

    private static final String M8M_BOT_TOKEN = "abc.xyz.some.token.that.should.not.be.shared";

    private final JDA discordApi;
    private final TextChannel channel;

    public DiscordPublisher() {
        try {
            discordApi = new JDABuilder(M8M_BOT_TOKEN).build().awaitReady();
            channel = discordApi.getTextChannelById(123L);
        } catch (final InterruptedException | LoginException e) {
            throw new RuntimeException("Failed to create JDA or channel");
        }
    }

    public synchronized void postMessage(@NonNull final Queue<Message> messages) {
        messages.forEach(message -> channel.sendMessage(message).complete());
    }

    @Override
    public void close() throws IOException {
        discordApi.shutdown();
    }
}
