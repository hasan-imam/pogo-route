package pogo.assistance.ui;

import static pogo.assistance.route.CooldownCalculator.getCooldown;

import com.google.common.base.Preconditions;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.core.MessageBuilder.SplitPolicy;
import pogo.assistance.data.model.Action;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Nest;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.model.Reward;
import pogo.assistance.data.model.Reward.RewardObject;
import pogo.assistance.data.model.Task;
import pogo.assistance.data.quest.QuestDictionary;
import pogo.assistance.route.CooldownCalculator;
import pogo.assistance.route.planning.conditional.bundle.Bundle;
import pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory;
import pogo.assistance.route.planning.conditional.bundle.Tour;

@Getter
public class TourDescriber {

    private static final String ZWSP = "\u200b";
    private static final int DISCORD_MSG_BUNDLE_COUNT_LIMIT = 10;
    public static final SplitPolicy DISCORD_MARKDOWN_SPLIT_POLICY = SplitPolicy.onChars(ZWSP, true);

    private static final Random RANDOM_GENERATOR = new Random();
    private static final Map<String, String> ID_TO_COLOR_MAP = new HashMap<>();

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.##");

    private final String genericSummary;
    private final String genericDescription;
    private final String discordPostWithMarkdown;
    private final String formattedForMapCustomizer;
    private final String formattedForMapMakerapp;

    /**
     * @implNote
     *      This thing does all the work at construction time for the sake of not duplicating the tour traversal code.
     *      All message generation has the same traversal but different logic deciding what is printed on each line.
     */
    public TourDescriber(@NonNull final Tour tour) {
        final List<? extends Bundle<? extends GeoPoint>> bundles = tour.getBundles();
        Preconditions.checkArgument(!bundles.isEmpty());
        final boolean allPointsHaveSameName = doAllPointsHaveSameName(tour);
        final StringBuilder genericDescriptionBuilder = new StringBuilder();
        final StringBuilder discordPostWithMarkdownBuilder = new StringBuilder("```md" + System.lineSeparator());
        final StringBuilder formattedForMapCustomizerBuilder = new StringBuilder();
        final StringBuilder formattedForMapMakerappBuilder = new StringBuilder();
        for (int i = 0; i < bundles.size(); i++) {
            final Bundle<? extends GeoPoint> bundle = bundles.get(i);
            final List<? extends GeoPoint> bundleElements = bundle.getElements();
            if (i % DISCORD_MSG_BUNDLE_COUNT_LIMIT == 0 && i >= DISCORD_MSG_BUNDLE_COUNT_LIMIT) {
                discordPostWithMarkdownBuilder.append("```").append(ZWSP)
                        .append("```md").append(System.lineSeparator());
            }
            for (int j = 0; j < bundleElements.size(); j++) {
                final GeoPoint currentPoint = bundleElements.get(j);
                final GeoPoint nextPoint;
                if ((j + 1) < bundleElements.size()) {
                    nextPoint = bundleElements.get(j + 1);
                } else if ((i + 1) < bundles.size()) {
                    nextPoint = bundles.get(i + 1).getFirst();
                } else {
                    nextPoint = null;
                }

                final String formattedPointName = allPointsHaveSameName ? "" : String.format("%1$-8s", namePoint(currentPoint));

                final String pointDescription = String.format(
                        "%s,%s, %s %s%s%n",
                        currentPoint.getLatitude(),
                        currentPoint.getLongitude(),
                        formattedPointName,
                        (nextPoint == null) ? "END" : RenderingUtils.toString(Duration.ofSeconds((long) getCooldown(currentPoint, nextPoint))),
                        (j + 1) < bundleElements.size() ? "" : String.format(" (set %d)", i + 1));
                genericDescriptionBuilder.append(pointDescription);
                discordPostWithMarkdownBuilder.append(pointDescription);
                formattedForMapCustomizerBuilder.append(String.format(
                        "%s,%s {%s %s}%s%n",
                        currentPoint.getLatitude(),
                        currentPoint.getLongitude(),
                        formattedPointName,
                        (nextPoint == null) ? "END" : RenderingUtils.toString(Duration.ofSeconds((long) getCooldown(currentPoint, nextPoint))),
                        (j + 1) < bundleElements.size() ? "" : String.format(" <green>", i + 1)));
                formattedForMapMakerappBuilder.append(String.format(
                        "%s,%s,%s %s%s%n", // lat,long,formattedPointName cd[,color]
                        currentPoint.getLatitude(),
                        currentPoint.getLongitude(),
                        formattedPointName,
                        (nextPoint == null) ? "END" : RenderingUtils.toString(Duration.ofSeconds((long) getCooldown(currentPoint, nextPoint))),
                        getColorCodeFor(currentPoint).map(s -> "," + s).orElse("")));
            } // Described a bundle
        } // Described all bundles
        discordPostWithMarkdownBuilder.append("```").append(ZWSP);

        this.genericSummary = new StringBuilder()
                .append(String.format("Total %d stops in %d sets", tour.getElements().size(), bundles.size()))
                .append(System.lineSeparator())
                .append(describeTotalCooldown(tour.getElements()))
                .append(System.lineSeparator())
                .append(describeActions(tour.getQuests()))
                .append(System.lineSeparator())
                .append(describeRewards(tour.getQuests()))
                .append(System.lineSeparator())
                .append(describeAbbreviations(tour.getElements()))
                .toString();
        this.genericDescription = genericDescriptionBuilder.toString();
        this.discordPostWithMarkdown = discordPostWithMarkdownBuilder.toString();
        this.formattedForMapCustomizer = formattedForMapCustomizerBuilder.toString();
        this.formattedForMapMakerapp = formattedForMapMakerappBuilder.toString();
    }

    private static String describeActions(final List<? extends Quest> quests) {
        final List<String> descriptions = quests.stream()
                .map(Task::getAction)
                .map(Action::getDescription)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        actions -> RenderingUtils.describeClassification(actions, Function.identity())));
        return RenderingUtils.toBulletPoints("# Actions", descriptions, 0);
    }

    private static String describeRewards(final List<? extends Quest> quests) {
        final Map<RewardObject, List<Reward>> groupedByType = quests.stream()
                .map(Quest::getReward)
                .collect(Collectors.groupingBy(Reward::getRewardObject));
        final List<String> descriptions = new ArrayList<>();
        groupedByType.forEach((rewardObject, rewards) -> {
            if (rewardObject == RewardObject.UNKNOWN) {
                // Generic description
                rewards.stream()
                        .collect(Collectors.groupingBy(Reward::getDescription, Collectors.counting()))
                        .forEach((desc, count) -> descriptions.add(String.format("%d x %s", count, desc)));
            } else {
                rewards.stream().collect(Collectors.partitioningBy(reward -> reward.getQuantity().isPresent()))
                        .forEach((isQuantifiable, partitionedRewards) -> {
                            if (isQuantifiable && !partitionedRewards.isEmpty()) {
                                descriptions.add(String.format(
                                        "%s x %s",
                                        NUMBER_FORMAT.format(partitionedRewards.stream()
                                                .map(Reward::getQuantity)
                                                .mapToDouble(Optional::get)
                                                .sum()),
                                        rewardObject.getRewardName()));
                            } else {
                                // Generic description
                                partitionedRewards.stream()
                                        .collect(Collectors.groupingBy(Reward::getDescription, Collectors.counting()))
                                        .forEach((desc, count) -> descriptions.add(String.format("%d x %s", count, desc)));
                            }
                        });
            }
        });
        return RenderingUtils.toBulletPoints("# Rewards", descriptions, 0);
    }

    private static String describeAbbreviations(final List<? extends GeoPoint> points) {
        final List<String> descriptions = points.stream()
                .filter(point -> point instanceof Task)
                .map(point -> (Task) point)
                .map(Task::getAbbreviation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .sorted()
                .map(abbreviation -> QuestDictionary.lookupByAbbreviation(abbreviation))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(task -> String.format("%s: %s -> %s",
                        task.getAbbreviation().get(),
                        task.getAction().getDescription(),
                        task.getReward().getDescription()))
                .collect(Collectors.toList());
        return RenderingUtils.toBulletPoints("# Abbreviations", descriptions, 0);
    }

    private static boolean doAllPointsHaveSameName(final Tour tour) {
        return tour.getElements().stream().map(TourDescriber::namePoint).distinct().count() <= 1;
    }

    private static String namePoint(final GeoPoint geoPoint) {
        if (geoPoint instanceof Quest) {
            return ((Quest) geoPoint).getAbbreviation()
                    .orElse(((Quest) geoPoint).getAction().getDescription());
        } else {
            return ((Nest) geoPoint).getDescription(); // Nest is the only other expected point type
        }
    }

    private static String describeTotalCooldown(final List<? extends GeoPoint> points) {
        return String.format(
                "Total time in cool down: %s",
                RenderingUtils.toString(Duration.ofSeconds((long) CooldownCalculator.calculateCost(points, CooldownCalculator::getCooldown))));
    }

    private static Optional<String> getColorCodeFor(final GeoPoint point) {
        final Optional<String> mappedToId = Optional.ofNullable(BundlePatternFactory.getGenericMapper().apply(point));
        if (!mappedToId.isPresent()) {
            return Optional.empty();
        }

        return mappedToId.map(id -> ID_TO_COLOR_MAP.computeIfAbsent(
                id,
                __ -> String.format("#%06x", RANDOM_GENERATOR.nextInt(0xffffff + 1))));
    }

}
