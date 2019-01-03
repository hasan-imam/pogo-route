package pogo.assistance.data.quest;

import java.util.List;
import lombok.NonNull;
import pogo.assistance.data.model.Map;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.extraction.source.ninedb.NineDBDataExtractor;
import pogo.assistance.data.extraction.source.pokemap.PokemapDataExtractor;

public class BaseQuestProvider implements QuestProvider {

    @Override
    public List<Quest> getQuests(@NonNull final Map map) {
        switch (map) {
            case JP:
                return new NineDBDataExtractor().getQuests();
            case NYC:
            case SG:
                return new PokemapDataExtractor(map).getQuests();
            default:
                throw new IllegalArgumentException();
        }
    }

}
