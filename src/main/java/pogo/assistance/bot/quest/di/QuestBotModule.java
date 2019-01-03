package pogo.assistance.bot.quest.di;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import pogo.assistance.bot.quest.publishing.Publisher;
import pogo.assistance.bot.quest.recipe.RareCandyRecipeExecutor;
import pogo.assistance.bot.quest.recipe.RecipeExecutor;
import pogo.assistance.bot.quest.recipe.StardustRecipeExecutor;
import pogo.assistance.data.model.Map;
import pogo.assistance.data.quest.QuestProvider;

@Module
public class QuestBotModule {

    @Provides
    public static Set<Map> provideQuestMaps() {
        return EnumSet.allOf(Map.class);
    }

    @Provides
    @ElementsIntoSet
    public static Set<RecipeExecutor> provideStardustRecipeExecutors(
            final Set<Map> maps,
            final QuestProvider questProvider,
            final Publisher publisher) {
        return maps.stream()
                .map(map -> new StardustRecipeExecutor(map, questProvider, publisher))
                .collect(Collectors.toSet());
    }

    @Provides
    @ElementsIntoSet
    public static Set<RecipeExecutor> provideRareCandyRecipeExecutors(
            final Set<Map> maps,
            final QuestProvider questProvider,
            final Publisher publisher) {
        return maps.stream()
                .map(map -> new RareCandyRecipeExecutor(map, questProvider, publisher))
                .collect(Collectors.toSet());
    }

}
