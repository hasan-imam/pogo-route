package pogo.assistance.data.di;

import dagger.Module;
import dagger.Provides;
import java.time.Duration;
import javax.inject.Named;
import pogo.assistance.data.persistence.QuestRWUtils;
import pogo.assistance.data.quest.BaseQuestProvider;
import pogo.assistance.data.quest.QuestProvider;
import pogo.assistance.data.quest.QuestProviderPersistenceWrapper;

@Module
public class QuestModule {

    public static final String QUEST_PERSISTENCE_TTL = "quest-persistence-ttl";

    @Provides
    public static QuestProvider provideQuestProvider(
            @Named(QUEST_PERSISTENCE_TTL) final Duration ttl,
            final QuestRWUtils questRWUtils) {
        return new QuestProviderPersistenceWrapper(ttl, new BaseQuestProvider(), questRWUtils);
    }

    @Provides
    @Named(QUEST_PERSISTENCE_TTL)
    public static Duration provideQuestPersistenceTtl() {
        return Duration.ofHours(1);
    }

}
