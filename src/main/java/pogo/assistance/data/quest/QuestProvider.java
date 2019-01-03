package pogo.assistance.data.quest;

import java.util.List;
import javax.annotation.Nonnull;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.model.Map;

public interface QuestProvider {

    @Nonnull List<Quest> getQuests(@Nonnull final Map map);

}
