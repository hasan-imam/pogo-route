package pogo.assistance.bot.quest.recipe;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import pogo.assistance.bot.quest.publishing.DiscordPublisher;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.quest.QuestProviderFactory;
import pogo.assistance.data.quest.QuestProviderFactory.QuestMap;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;
import pogo.assistance.route.planning.conditional.bundle.ImmutablePlannerConfig;
import pogo.assistance.route.planning.conditional.bundle.PlannerConfig;
import pogo.assistance.route.planning.conditional.bundle.Tour;
import pogo.assistance.route.planning.conditional.bundle.TourPlanner;
import pogo.assistance.ui.TourDescriber;

/**
 * Base structure of a recipe executor.
 *
 * A recipe executor basically produces certain route. Specification of the route as well as the execution is
 * implemented in the executor. This base class pretty much does all the heavy lifting in a generic way such that
 * specific recipe classes can just override things to customize things specific to the recipe.
 */
@RequiredArgsConstructor
public abstract class RecipeExecutor {

    @Getter(value = AccessLevel.PROTECTED)
    private final DiscordPublisher publisher;
    private final List<String> executionNotes = new ArrayList<>();

    public void execute() {
        // Get points from map/source
        final List<? extends GeoPoint> points = supplyPoints();
        if (points.isEmpty()) {
            logExecutionNote("No point available for planning.");
            publish(Collections.emptyList());
            return;
        }

        // Refine points to keep the relevant ones
        final List<? extends GeoPoint> mutableList = new ArrayList<>(points);
        final List<BundlePattern<GeoPoint, String>> bundlePatterns = getBundlePatterns(mutableList);
        if (mutableList.isEmpty()) {
            logExecutionNote("Available points are irrelevant for this recipe.");
            publish(Collections.emptyList());
            return;
        }
        logExecutionNote(String.format("Considered %s/%s quests for planning.", mutableList.size(), points.size()));

        // Plan
        final List<Tour> tours = plan(mutableList, bundlePatterns);

        // Publish
        publish(tours);
    }

    protected List<? extends GeoPoint> supplyPoints() {
        return QuestProviderFactory.getPersistanceBackedQuestProvider(getMap()).getQuests();
    }

    protected List<Tour> plan(
            final List<? extends GeoPoint> points,
            final List<BundlePattern<GeoPoint, String>> bundlePatterns) {
        Preconditions.checkArgument(!points.isEmpty());
        final List<? extends GeoPoint> mutableList = new ArrayList<>(points);
        final List<Tour> tours = new ArrayList<>();
        final TourPlanner planner = new TourPlanner(getPlannerConfig());
        boolean noAcceptableTour = false;
        while (!mutableList.isEmpty() && !noAcceptableTour) {
            final Optional<Tour> planned = planner.plan(Collections.unmodifiableList(mutableList), bundlePatterns);
            if (planned.isPresent() && !planned.get().getBundles().isEmpty()) {
                final Tour tour = planned.get();
                mutableList.removeAll(tour.getElements());
                tours.add(tour);
            } else {
                noAcceptableTour = true;
            }
        }

        logExecutionNote(String.format("Planning produced %s route(s).", tours.size()));
        if (tours.removeIf(tour -> tour.getBundles().size() < 3)) {
            logExecutionNote("Discarded one/more route(s) because they were too short (< 3 sets).");
        }
        if (!mutableList.isEmpty()) {
            logExecutionNote(String.format("%s points didn't fit into any route.", mutableList.size()));
        }

        return tours;
    }

    protected void publish(final List<Tour> tours) {
        final Queue<Message> messages = new LinkedList<>();

        messages.add(new MessageBuilder().appendCodeBlock(getRecipeDescription()
                + System.lineSeparator()
                + Strings.repeat("=", getRecipeDescription().length()), "md")
                .build());

        tours.forEach(tour -> {
            final TourDescriber tourDescriber = new TourDescriber(tour.getBundles());
            messages.add(new MessageBuilder().appendCodeBlock(tourDescriber.getGenericSummary(), "md").build());
            messages.addAll(new MessageBuilder().append(tourDescriber.getDiscordPostWithMarkdown()).buildAll(TourDescriber.DISCORD_MARKDOWN_SPLIT_POLICY));
        });
        getExecutionNotes().ifPresent(notes -> messages.add(new MessageBuilder().appendCodeBlock(notes, "fix").build()));

        publisher.postMessage(messages);
    }

    protected void logExecutionNote(final String note) {
        executionNotes.add(note);
    }

    protected Optional<String> getExecutionNotes() {
        if (executionNotes.isEmpty()) {
            return Optional.empty();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("Notes:");
        executionNotes.forEach(note -> sb.append(System.lineSeparator()).append("  * ").append(note));
        return Optional.of(sb.toString());
    }

    protected PlannerConfig getPlannerConfig() {
        return ImmutablePlannerConfig.builder()
                .maxStepDuration(Duration.ofMinutes(20))
                .build();
    }

    protected abstract QuestMap getMap();
    protected abstract String getRecipeDescription();

    /**
     * @param points
     *      Mutable list of points. This method should remove all elements from this list that are not relevant to the
     *      bundle patterns returned by this method.
     * @return
     *      List of patterns that should be used for planning.
     */
    protected abstract List<BundlePattern<GeoPoint, String>> getBundlePatterns(final List<? extends GeoPoint> points);

}
