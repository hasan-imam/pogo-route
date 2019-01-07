package pogo.assistance.bot.quest.recipe;

import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createNOfAKindPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createOrderIndependentPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.genericMapper;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.getGenericMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pogo.assistance.bot.quest.publishing.Publisher;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Map;
import pogo.assistance.data.model.Reward.RewardObject;
import pogo.assistance.data.quest.QuestProvider;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;
import pogo.assistance.route.planning.conditional.bundle.ImmutablePlannerConfig;
import pogo.assistance.route.planning.conditional.bundle.PlannerConfig;
import pogo.assistance.route.planning.conditional.bundle.Tour;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class RareCandyRecipeExecutor extends RecipeExecutor {

    private static final Set<String> PATTERN_ELEMENTS = ImmutableSet.of("3GC", "DRC", "DRT");
    private static final List<BundlePattern<GeoPoint, String>> PATTERNS = ImmutableList.of(
            createNOfAKindPattern(Arrays.asList("3GC"), 3, getGenericMapper()),
            createOrderIndependentPattern(Arrays.asList("DRC", "DRC", "DRT"), getGenericMapper()));

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
        return String.format("Rare candy route for %s", getMap());
    }

    @Override
    protected List<BundlePattern<GeoPoint, String>> getBundlePatterns(final List<? extends GeoPoint> points) {
        points.removeIf(point -> !PATTERN_ELEMENTS.contains(genericMapper(point)));
        return PATTERNS;
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
                    .maxStepDuration(Duration.ofMinutes(20))
                    .build();
        }
    }

    @Override
    protected Comparator<Tour> getTourComparator() {
        return Comparator.<Tour, Double>comparing(tour -> tour.getQuantifiedRewards().getOrDefault(RewardObject.RARE_CANDY, 0D))
                .thenComparing(Comparator.comparing(Tour::getTotalDuration).reversed());
    }
}
