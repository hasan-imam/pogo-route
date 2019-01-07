package pogo.assistance.bot.quest.recipe;

import static pogo.assistance.ui.RenderingUtils.describeClassification;
import static pogo.assistance.ui.RenderingUtils.getHoursLeftInDay;
import static pogo.assistance.ui.RenderingUtils.toBulletPoints;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import pogo.assistance.bot.quest.publishing.Publisher;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Map;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;
import pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory;
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
@Slf4j
@RequiredArgsConstructor
public abstract class RecipeExecutor {

    private final StringBuilder executionNotes = new StringBuilder();

    public void execute() {
        log.info("Running executor: " + getRecipeDescription());
        logExecutionNote(String.format("%d hours left in the day for %s", getHoursLeftInDay(getMap()), getMap()));

        // Get points from map/source
        final List<? extends GeoPoint> points = supplyPoints();
        if (points.isEmpty()) {
            logExecutionNote("No point available for planning");
            publish(Collections.emptyList());
            return;
        }

        // Refine points to keep the relevant ones
        final List<? extends GeoPoint> mutableList = new ArrayList<>(points);
        final List<BundlePattern<GeoPoint, String>> bundlePatterns = getBundlePatterns(mutableList);
        if (mutableList.isEmpty()) {
            logExecutionNote("Available points are irrelevant for this recipe");
            publish(Collections.emptyList());
            return;
        }
        logPoints(String.format("Considered following %d points for planning:", mutableList.size()), mutableList);

        // Plan
        final List<Tour> tours = plan(mutableList, bundlePatterns);

        // Publish
        publish(tours);
    }

    protected abstract List<? extends GeoPoint> supplyPoints();

    protected List<Tour> plan(
            final List<? extends GeoPoint> points,
            final List<BundlePattern<GeoPoint, String>> bundlePatterns) {
        Preconditions.checkArgument(!points.isEmpty());
        final List<? extends GeoPoint> mutableList = new ArrayList<>(points);
        final List<Tour> tours = new ArrayList<>();
        final PlannerConfig plannerConfig = getPlannerConfig();
        final TourPlanner planner = new TourPlanner(plannerConfig);
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

        logExecutionNote(String.format("Planning produced %s route(s)", tours.size()));
        if (tours.removeIf(tour -> tour.getBundles().size() < 3)) {
            logExecutionNote("Discarded one/more route(s) because they were too short (< 3 sets)");
        }
        if (!mutableList.isEmpty()) {
            logPoints(String.format("%d points that didn't fit into route:", mutableList.size()), mutableList);
        }
        logPlannerConfig(plannerConfig);

        return tours;
    }

    protected void publish(final List<Tour> tours) {
        final Queue<Message> messages = new LinkedList<>();

        messages.add(new MessageBuilder().appendCodeBlock(getRecipeDescription()
                + System.lineSeparator()
                + Strings.repeat("=", getRecipeDescription().length()), "md")
                .build());

        tours.forEach(tour -> {
            final TourDescriber tourDescriber = new TourDescriber(tour);
            messages.add(new MessageBuilder().appendCodeBlock(tourDescriber.getGenericSummary(), "md").build());
            messages.addAll(new MessageBuilder().append(tourDescriber.getDiscordPostWithMarkdown()).buildAll(TourDescriber.DISCORD_MARKDOWN_SPLIT_POLICY));
        });

        getExecutionNotes().ifPresent(notes -> messages.add(new MessageBuilder().appendCodeBlock(notes, "fix").build()));

        getPublisher().publish(messages);
//        // Put some emotes on the last message
//        final List<Message> published = getPublisher().publish(messages);
//        Streams.findLast(published.stream().filter(Objects::nonNull)).ifPresent(lastMessage -> {
//            lastMessage.addReaction("\uD83E\uDD16").queue();
//            lastMessage.addReaction("\uD83D\uDC4D").queue();
//            lastMessage.addReaction("\uD83D\uDC4E").complete();
//        });
    }

    protected void logExecutionNote(final String note) {
        if (executionNotes.length() > 0) {
            executionNotes.append(System.lineSeparator());
        }
        executionNotes.append(" * ").append(note);
    }

    protected void logPoints(final String header, final List<? extends GeoPoint> points) {
        if (executionNotes.length() > 0) {
            executionNotes.append(System.lineSeparator());
        }
        executionNotes.append(toBulletPoints(
                header,
                describeClassification(points, BundlePatternFactory.getGenericMapper()),
                1));
    }

    protected void logPlannerConfig(final PlannerConfig plannerConfig) {
        final List<String> descriptions = plannerConfig.describe();
        if (descriptions.isEmpty()) {
            return;
        }

        if (executionNotes.length() > 0) {
            executionNotes.append(System.lineSeparator());
        }
        executionNotes.append(toBulletPoints("Planner configuration:", descriptions, 1));
    }

    protected Optional<String> getExecutionNotes() {
        return Optional.of(executionNotes)
                .filter(notes -> notes.length() > 0)
                .map(notes -> "Notes:" + System.lineSeparator() + notes.toString());
    }

    protected PlannerConfig getPlannerConfig() {
        return ImmutablePlannerConfig.builder()
                .maxStepDuration(Duration.ofMinutes(20))
                .maxBundleToBundleDuration(Optional.of(Duration.ofMinutes(30)))
                .build();
    }

    protected Comparator<Tour> getTourComparator() {
        return Comparator.<Tour, Integer>comparing(tour -> tour.getBundles().size())
                .thenComparing(Comparator.comparing(Tour::getTotalDuration).reversed());
    }

    protected abstract Map getMap();
    protected abstract String getRecipeDescription();
    protected abstract Publisher getPublisher();

    /**
     * @param points
     *      Mutable list of points. This method should remove all elements from this list that are not relevant to the
     *      bundle patterns returned by this method.
     * @return
     *      List of patterns that should be used for planning.
     */
    protected abstract List<BundlePattern<GeoPoint, String>> getBundlePatterns(final List<? extends GeoPoint> points);

}
