package pogo.assistance.bot.quest.publishing;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.core.entities.Message;

/**
 * When you don't want to make real Discord posts, use this to output to console. Useful for testing, trial runs etc.
 */
public class ConsolePublisher implements Publisher {
    @Override
    public List<Message> publish(@Nonnull final Queue<Message> messages) {
        messages.forEach(message -> {
            System.out.println(message.getContentRaw());
        });
        return Collections.emptyList();
    }
}
