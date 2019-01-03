package pogo.assistance.bot.quest.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pogo.assistance.bot.quest.publishing.Publisher;
import pogo.assistance.data.di.PersistenceModule;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Map;
import pogo.assistance.data.model.Task;
import pogo.assistance.data.quest.QuestProvider;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;
import pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory;
import pogo.assistance.util.FileIOUtils;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class StardustRecipeExecutor extends RecipeExecutor {

    @Getter
    private final Map map;
    @Getter
    private final QuestProvider questProvider;
    @Getter
    private final Publisher publisher;

    @Override
    protected List<? extends GeoPoint> supplyPoints() {
        return getQuestProvider().getQuests(getMap());
    }

    @Override
    protected String getRecipeDescription() {
        return String.format("Stardust route for %s", getMap());
    }

    @Override
    protected List<BundlePattern<GeoPoint, String>> getBundlePatterns(final List<? extends GeoPoint> points) {
        final List<Task> stardustTasks = getStardustTasks();
        final Set<String> relevantElements = new HashSet<>();
        final List<String> patternElements = stardustTasks.stream()
                .map(BundlePatternFactory::genericMapper)
                .peek(relevantElements::add)
                .collect(Collectors.toList());
        points.removeIf(o -> !relevantElements.contains(BundlePatternFactory.genericMapper(o)));
        return Collections.singletonList(BundlePatternFactory.createNOfAKindPattern(patternElements, 3, BundlePatternFactory::genericMapper));
    }

    private static List<Task> getStardustTasks() {
        try {
            return Arrays.asList(PersistenceModule.provideGson().fromJson(
                    new String(Files.readAllBytes(
                            FileIOUtils.resolvePackageLocalFilePath("stardust-quests.json", StardustRecipeExecutor.class)),
                            StandardCharsets.UTF_8),
                    Task[].class));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read quest dictionary: quest-dictionary.json", e);
        }
    }

}
