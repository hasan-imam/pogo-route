package pogo.assistance.bot.quest.recipe;

import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createAnyOfASetPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createNOfAKindPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createOrderIndependentPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.genericMapper;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.getGenericMapper;
import static pogo.assistance.ui.RenderingUtils.getHoursLeftInDay;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pogo.assistance.bot.quest.publishing.Publisher;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Map;
import pogo.assistance.data.model.Reward.RewardObject;
import pogo.assistance.data.quest.QuestProvider;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;
import pogo.assistance.route.planning.conditional.bundle.ImmutablePlannerConfig;
import pogo.assistance.route.planning.conditional.bundle.PlannerConfig;
import pogo.assistance.route.planning.conditional.bundle.Tour;
import pogo.assistance.route.planning.conditional.bundle.TourPlanner;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class StardustRecipeExecutor extends RecipeExecutor {

    private static final Set<String> PRIMARY_PATTERN_ELEMENTS = ImmutableSet.of(
            "3G15", // "Make 3 Great Curveball Throws in a row - 1500 Stardust", // "3G15"
            "3G10", // "Make 3 Great Throws in a row - 1000 Stardust", // "3G10"
            "3GCS10", // "Make 3 Great Curveball Throws - 1000 Stardust",
            "DR15", "DRT");
    private static final List<BundlePattern<GeoPoint, String>> PRIMARY_PATTERNS = ImmutableList.of(
            createAnyOfASetPattern(
                    Arrays.asList(
                            "3G15", // "Make 3 Great Throws in a row - 1000 Stardust",
                            "3G10", // "Make 3 Great Curveball Throws in a row - 1500 Stardust",
                            "3GCS10"), // "Make 3 Great Curveball Throws - 1000 Stardust"
                    3, getGenericMapper()),
            createOrderIndependentPattern(Arrays.asList("DR15", "DR15", "DRT"), getGenericMapper()));

    private static final Set<String> SECONDARY_PATTERN_ELEMENTS = ImmutableSet.<String>builder()
            .addAll(PRIMARY_PATTERN_ELEMENTS)
            .add("3B10")
            .build();
    private static final List<BundlePattern<GeoPoint, String>> SECONDARY_PATTERNS = ImmutableList.<BundlePattern<GeoPoint, String>>builder()
            .addAll(PRIMARY_PATTERNS)
            .add(createNOfAKindPattern(Arrays.asList("3B10"), 3, getGenericMapper()))
            .build();

    @Getter
    private final Map map;
    @Getter
    private final QuestProvider questProvider;
    @Getter
    private final Publisher publisher;

    public void execute() {
        log.info("Running executor: " + getRecipeDescription());
        logExecutionNote(String.format("%d hours left in the day for %s", getHoursLeftInDay(getMap()), getMap()));

        // Get points from map/source
        final List<? extends GeoPoint> points = supplyPoints();
        if (points.isEmpty()) {
            logExecutionNote("No point available for planning.");
            publish(Collections.emptyList());
            return;
        }

        // Plan
        final List<Tour> tours = plan(points, null);

        // Publish
        publish(tours);
    }

    protected List<Tour> plan(
            final List<? extends GeoPoint> points,
            final List<BundlePattern<GeoPoint, String>> __) {
        Preconditions.checkArgument(!points.isEmpty());
        final List<? extends GeoPoint> mutableList = new ArrayList<>(points);
        final List<Tour> tours = new ArrayList<>();
        boolean noAcceptableTour = false;
        while (!mutableList.isEmpty() && !noAcceptableTour) {
            final Optional<Tour> planned = planRoute(mutableList);
            if (planned.isPresent()) {
                tours.add(planned.get());
            } else {
                noAcceptableTour = true;
            }
        }

        logExecutionNote(String.format("Planning produced %s route(s) in total.", tours.size()));

        return tours;
    }

    /**
     * @param mutableList
     *      Points to be used for planning. Points that are actually used in the returned tour are removed from this
     *      mutable list.
     * @return
     *      A tour produced using the given points.
     */
    protected Optional<Tour> planRoute(final List<? extends GeoPoint> mutableList) {
        // First, plan with "high value" (primary) quests
        final Optional<Tour> primaryPlanned = planRoute(
                Collections.unmodifiableList(mutableList),
                PRIMARY_PATTERN_ELEMENTS,
                PRIMARY_PATTERNS,
                getPlannerConfig(),
                "primary",
                qualityFilter(40000, 5.56, "primary"));
        if (primaryPlanned.isPresent()) {
            // Try with secondary options, but with constrained duration based on primary results
            final Duration tourDurationLimit = Duration.ofSeconds(
                    900 * (long) Math.ceil(primaryPlanned.get().getTotalDuration().getSeconds() / 900.0));
            final Optional<Tour> aggressivelyPlanned = planRoute(
                    Collections.unmodifiableList(mutableList),
                    SECONDARY_PATTERN_ELEMENTS,
                    SECONDARY_PATTERNS,
                    ImmutablePlannerConfig.builder()
                            .from(getPlannerConfig())
                            .maxTourDuration(tourDurationLimit)
                            .build(),
                    "primary-constrained",
                    qualityFilter(getStardustAmount(primaryPlanned.get()), 0.9 * getStardustRate(primaryPlanned.get()), "primary-constrained"));
            if (aggressivelyPlanned.isPresent()) {
                aggressivelyPlanned.ifPresent(tour -> mutableList.removeAll(tour.getElements()));
                return aggressivelyPlanned;
            }

            // Attempt with secondary failed - so just use what we have with primary
            primaryPlanned.ifPresent(tour -> mutableList.removeAll(tour.getElements()));
            return primaryPlanned;
        }

        // Attempt with primary failed - so just plan the best possible route with all quest options (secondary)
        final Optional<Tour> secondaryPlanned = planRoute(
                Collections.unmodifiableList(mutableList),
                SECONDARY_PATTERN_ELEMENTS,
                SECONDARY_PATTERNS,
                getPlannerConfig(),
                "secondary",
                qualityFilter(20000, 1.5, "secondary"));
        secondaryPlanned.ifPresent(tour -> mutableList.removeAll(tour.getElements()));
        return secondaryPlanned;
    }

    protected Optional<Tour> planRoute(
            final List<? extends GeoPoint> points,
            final Set<String> patternElements,
            final List<BundlePattern<GeoPoint, String>> patterns,
            final PlannerConfig plannerConfig,
            final String tag,
            final Predicate<Tour> qualityCheck) {
        final List<? extends GeoPoint> mutableList = new ArrayList<>(points);
        mutableList.removeIf(point -> !patternElements.contains(genericMapper(point)));
        if (mutableList.isEmpty()) {
            logExecutionNote(String.format("[%s] Insufficient pattern elements available: %s", tag, String.join(", ", patternElements)));
            return Optional.empty();
        }

        final Optional<Tour> planned = new TourPlanner(plannerConfig)
                .plan(Collections.unmodifiableList(mutableList), patterns)
                .filter(qualityCheck);
        if (planned.isPresent()) {
            logPoints(String.format("[%s] Considered following %d points for planning:", tag, mutableList.size()), mutableList);
        }
        return planned;
    }

    protected Predicate<Tour> qualityFilter(
            final double desiredStardustAmount,
            final double desiredStardustRate,
            final String tag) {
        return tour -> {
            if (tour.getBundles().isEmpty()) {
                return false;
            }
            if (getStardustAmount(tour) >= desiredStardustAmount && getStardustRate(tour) >= desiredStardustRate) {
                return true;
            }
            final DecimalFormat decimalFormat = new DecimalFormat("#.##");
            logExecutionNote(String.format(
                    "[%s] Tour failed quality gate. Amount: %s (expected %s+), rate: %s (expected %s+).",
                    tag,
                    decimalFormat.format(getStardustAmount(tour)),
                    decimalFormat.format(desiredStardustAmount),
                    decimalFormat.format(getStardustRate(tour)),
                    decimalFormat.format(desiredStardustRate)));
            return false;
        };
    }

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
        throw new UnsupportedOperationException("Stardust recipe execution doesn't use the general pattern supplier");
    }

    @Override
    protected PlannerConfig getPlannerConfig() {
        if (getMap() == Map.JP) {
            return ImmutablePlannerConfig.builder()
                    .maxBundleDuration(Duration.ofMinutes(20))
                    .maxBundleToBundleDuration(Optional.of(Duration.ofMinutes(30)))
                    .tourComparator(getTourComparator())
                    .build();
        } else {
            return ImmutablePlannerConfig.builder()
                    .maxStepDuration(Duration.ofMinutes(15))
                    .maxTourDuration(Duration.ofHours(3))
                    .tourComparator(getTourComparator())
                    .build();
        }
    }

    @Override
    protected Comparator<Tour> getTourComparator() {
        return Comparator.<Tour, Double>comparing(tour -> tour.getQuantifiedRewards().getOrDefault(RewardObject.STARDUST, 0D))
                .thenComparing(Comparator.comparing(Tour::getTotalDuration).reversed());
    }

    private static double getStardustAmount(final Tour tour) {
        return tour.getQuantifiedRewards().getOrDefault(RewardObject.STARDUST, 0D);
    }

    private static double getStardustRate(final Tour tour) {
        return (tour.getQuantifiedRewards().getOrDefault(RewardObject.STARDUST, 0D) / tour.getTotalDuration().getSeconds());
    }

}
