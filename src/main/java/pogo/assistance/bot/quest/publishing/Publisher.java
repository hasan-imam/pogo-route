package pogo.assistance.bot.quest.publishing;

import java.util.List;
import java.util.Queue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import net.dv8tion.jda.core.entities.Message;

/**
 * Interface for publishing things to Discord.
 *
 * This interface is pretty tied to JDA because we don't plan to use an alternative lib for publishing to Discord. Plus,
 * using JDA POJOs (like {@link Message}) frees us from adding our own interfaces that ties to various Discord
 * constructs.
 */
@ThreadSafe
public interface Publisher {

    List<Message> publish(@Nonnull final Queue<Message> messages);

}
