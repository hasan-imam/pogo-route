package pogo.assistance.bot.quest.di;

import dagger.Component;
import javax.inject.Singleton;
import pogo.assistance.bot.quest.QuestBot;
import pogo.assistance.bot.quest.publishing.PublisherModule;
import pogo.assistance.data.di.PersistenceModule;
import pogo.assistance.data.di.QuestModule;

@Singleton
@Component(modules = {
        PersistenceModule.class, PublisherModule.class,
        QuestBotModule.class, QuestModule.class})
public interface QuestBotComponent {

    QuestBot getQuestBot();

}
