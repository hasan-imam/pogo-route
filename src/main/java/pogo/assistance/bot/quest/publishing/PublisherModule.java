package pogo.assistance.bot.quest.publishing;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;

@Module
public class PublisherModule {

    private static final String M8M_BOT_TOKEN = "[REDACTED]";
    private static final long QUEST_OUTPUT_CHANNEL = 123L;

    @Provides
    @Singleton
    public static Publisher providePublisher(final TextChannel textChannel) {
        return new DiscordPublisher(textChannel);
    }

    @Provides
    @Singleton
    public static JDA provideJda() {
        final JDABuilder jdaBuilder = new JDABuilder()
                .setToken(M8M_BOT_TOKEN)
                .setAutoReconnect(true)
                .setEnableShutdownHook(true);
        try {
            return jdaBuilder.build().awaitReady();
        } catch (final InterruptedException | LoginException e) {
            throw new RuntimeException("Failed to setup JDA", e);
        }
    }

    @Provides
    @Singleton
    public static TextChannel provideTextChannel(final JDA jda) {
        return jda.getTextChannelById(QUEST_OUTPUT_CHANNEL);
    }

}
