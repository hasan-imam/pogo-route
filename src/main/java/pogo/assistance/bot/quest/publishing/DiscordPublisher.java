package pogo.assistance.bot.quest.publishing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

@Slf4j
@ThreadSafe
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DiscordPublisher implements Publisher {

    private final TextChannel channel;

    @Override
    public synchronized List<Message> publish(@NonNull final Queue<Message> messages) {
        final List<Message> posted = new ArrayList<>(messages.size());
        // Blocks until each message. Being async may result in program exiting before all messages being sent out.
        messages.forEach(message -> {
            posted.add(channel.sendMessage(message).complete());
        });
        return posted;
    }

}
