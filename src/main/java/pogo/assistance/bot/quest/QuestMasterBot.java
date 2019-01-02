package pogo.assistance.bot.quest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import pogo.assistance.bot.quest.publishing.DiscordPublisher;
import pogo.assistance.bot.quest.recipe.RareCandyRecipeExecutor;
import pogo.assistance.bot.quest.recipe.RecipeExecutor;
import pogo.assistance.bot.quest.recipe.StardustRecipeExecutor;
import pogo.assistance.data.quest.QuestProviderFactory.QuestMap;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class QuestMasterBot {

    public static void main(final String[] args) throws IOException {
        final DiscordPublisher discordPublisher = new DiscordPublisher();
        final List<RecipeExecutor> recipeExecutors = Arrays.asList(
                // Stardust
                new StardustRecipeExecutor(QuestMap.JP, discordPublisher),
                new StardustRecipeExecutor(QuestMap.NYC, discordPublisher),
                new StardustRecipeExecutor(QuestMap.SG, discordPublisher),
                // Rare candy
                new RareCandyRecipeExecutor(QuestMap.JP, discordPublisher),
                new RareCandyRecipeExecutor(QuestMap.NYC, discordPublisher),
                new RareCandyRecipeExecutor(QuestMap.SG, discordPublisher)
        );
        recipeExecutors.forEach(RecipeExecutor::execute);
        discordPublisher.close();
        System.exit(0);
    }

}
