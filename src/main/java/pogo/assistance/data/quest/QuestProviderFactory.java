package pogo.assistance.data.quest;

import lombok.NonNull;
import pogo.assistance.data.quest.extraction.source.ninedb.NineDBQuestProvider;
import pogo.assistance.data.quest.extraction.source.pokemap.PokemapQuestProvider;

public class QuestProviderFactory {

    public enum QuestMap {
        JP,
        NYC,
        SG
    }

    public static QuestProvider getExtractingQuestProvider(@NonNull final QuestMap map) {
        final QuestProvider questProvider;
        switch (map) {
            case NYC:
            case SG:
                questProvider = new PokemapQuestProvider(map);
                break;
            case JP:
                questProvider = new NineDBQuestProvider();
                break;
            default:
                throw new IllegalArgumentException();
        }
        return questProvider;
    }

    public static QuestProvider getPersistanceBackedQuestProvider(@NonNull final QuestMap map) {
        return new QuestProviderPersistenceWrapper(null, getExtractingQuestProvider(map));
    }

}
