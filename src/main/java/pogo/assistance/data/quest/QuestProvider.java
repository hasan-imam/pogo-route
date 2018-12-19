package pogo.assistance.data.quest;

import java.util.List;
import javax.annotation.Nonnull;
import pogo.assistance.data.quest.QuestProviderFactory.QuestMap;
import pogo.assistance.data.model.Quest;

public interface QuestProvider {

    @Nonnull List<Quest> getQuests();

    @Nonnull QuestMap getMap();

}
