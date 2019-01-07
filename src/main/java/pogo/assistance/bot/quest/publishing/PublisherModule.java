package pogo.assistance.bot.quest.publishing;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;

@Module
public class PublisherModule {

    public static final String CONSOLE_PUBLISHER = "console-publisher";
    public static final String DISCORD_PUBLISHER = "discord-publisher";

    private static final String M8M_BOT_TOKEN = "[redacted]";

    private static final long CHANNEL_TEST_LIST_ROUTE_PREVIEW = 123L;
    private static final long CHANNEL_AUTOMATED_LIST_ROUTE_PREVIEW = 123L;
    private static final long CHANNEL_LIST_ROUTE_PREVIEW = 123L;

//    @Provides
//    @Singleton
////    @Named(CONSOLE_PUBLISHER)
//    public static Publisher provideConsolePublisher() {
//        return new ConsolePublisher();
//    }

    @Provides
    @Singleton
//    @Named(DISCORD_PUBLISHER)
    public static Publisher provideDiscordPublisher(final TextChannel textChannel) {
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
        final long id = CHANNEL_TEST_LIST_ROUTE_PREVIEW;
//        final long id = CHANNEL_AUTOMATED_LIST_ROUTE_PREVIEW;
        final TextChannel textChannel = jda.getTextChannelById(id);
        if (textChannel == null) {
            throw new IllegalArgumentException(String.format("Text channel %s non-existent or inaccessible", id));
        }
        return textChannel;
    }

}
