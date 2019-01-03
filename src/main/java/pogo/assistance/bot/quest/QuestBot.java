package pogo.assistance.bot.quest;

import java.util.Set;
import javax.inject.Inject;
import pogo.assistance.bot.quest.di.DaggerQuestBotComponent;
import pogo.assistance.bot.quest.recipe.RecipeExecutor;

//@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class QuestBot {

    private final Set<RecipeExecutor> recipeExecutors;

    @Inject
    public QuestBot(final Set<RecipeExecutor> recipeExecutors) {
        this.recipeExecutors = recipeExecutors;
    }

    public static void main(final String[] args) {
        final QuestBot bot = DaggerQuestBotComponent.create().getQuestBot();
        bot.recipeExecutors.forEach(RecipeExecutor::execute);
        System.exit(0);
    }

}
