package pogo.assistance.ui;

import static pogo.assistance.route.CooldownCalculator.getCooldown;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Nest;
import pogo.assistance.data.model.Quest;
import pogo.assistance.route.CooldownCalculator;
import pogo.assistance.route.planning.conditional.bundle.Bundle;
import pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory;

@Getter
public class TourDescriber {

    private static final Random RANDOM_GENERATOR = new Random();
    private static final Map<String, String> ID_TO_COLOR_MAP = new HashMap<>();

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
    public TourDescriber(@NonNull final List<? extends Bundle<? extends GeoPoint>> bundles) {
        Preconditions.checkArgument(!bundles.isEmpty());

        final List<? super GeoPoint> allPoints = new ArrayList<>();
        final List<Quest> quests = new ArrayList<>();
        final StringBuilder genericDescriptionBuilder = new StringBuilder();
        final StringBuilder discordPostWithMarkdownBuilder = new StringBuilder();
        final StringBuilder formattedForMapCustomizerBuilder = new StringBuilder();
        final StringBuilder formattedForMapMakerappBuilder = new StringBuilder();
        for (int i = 0; i < bundles.size(); i++) {
            final Bundle<? extends GeoPoint> bundle = bundles.get(i);
            final List<? extends GeoPoint> bundleElements = bundle.getElements();
            discordPostWithMarkdownBuilder.append("```").append(System.lineSeparator());
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

                final String description;
                if (currentPoint instanceof Quest) {
                    description = ((Quest) currentPoint).getAbbreviation()
                            .orElse(((Quest) currentPoint).getAction().getDescription());
                    quests.add((Quest) currentPoint);
                } else {
                    description = ((Nest) currentPoint).getDescription(); // Nest is the only other expected point type
                }
                allPoints.add(currentPoint);

                final String pointDescription = String.format(
                        "%s,%s, %s %s%s%n",
                        currentPoint.getLatitude(),
                        currentPoint.getLongitude(),
                        description,
                        (nextPoint == null) ? "END" : Duration.ofSeconds((long) getCooldown(currentPoint, nextPoint)),
                        (j + 1) < bundleElements.size() ? "" : String.format(" (set %d)%n", i + 1));
                genericDescriptionBuilder.append(pointDescription);
                discordPostWithMarkdownBuilder.append(pointDescription);
                formattedForMapCustomizerBuilder.append(String.format(
                        "%s,%s {%s %s}%s%n",
                        currentPoint.getLatitude(),
                        currentPoint.getLongitude(),
                        description,
                        (nextPoint == null) ? "END" : Duration.ofSeconds((long) getCooldown(currentPoint, nextPoint)),
                        (j + 1) < bundleElements.size() ? "" : String.format(" <green>", i + 1)));
                formattedForMapMakerappBuilder.append(String.format(
                        "%s,%s,%s %s%s%n", // lat,long,name cd[,color]
                        currentPoint.getLatitude(),
                        currentPoint.getLongitude(),
                        description,
                        (nextPoint == null) ? "END" : Duration.ofSeconds((long) getCooldown(currentPoint, nextPoint)),
                        getColorCodeFor(currentPoint).map(s -> "," + s).orElse("")));
            } // Described a bundle
            discordPostWithMarkdownBuilder.append("```").append(System.lineSeparator());
        } // Described all bundles

        this.genericSummary = new StringBuilder()
                .append(String.format("Total %d quests in %d bundles", quests.size(), bundles.size()))
                .append(System.lineSeparator())
                .append(describeTotalCooldown((List<? extends GeoPoint>) allPoints))
                .append(System.lineSeparator())
                .append(describeActions(quests))
                .append(System.lineSeparator())
                .append(describeRewards(quests))
                .append(System.lineSeparator())
                .toString();
        this.genericDescription = genericDescriptionBuilder.toString();
        this.discordPostWithMarkdown = discordPostWithMarkdownBuilder.toString();
        this.formattedForMapCustomizer = formattedForMapCustomizerBuilder.toString();
        this.formattedForMapMakerapp = formattedForMapMakerappBuilder.toString();
    }

    private static String describeActions(final List<Quest> quests) {
        return quests.stream().collect(Collectors.groupingBy(Quest::getAction)).entrySet().stream()
                .map(entry -> String.format(
                        "%d x %s",
                        entry.getValue().size(),
                        entry.getKey().getDescription()))
                .collect(Collectors.joining(", ", "Actions: ", System.lineSeparator()));
    }

    private static String describeRewards(final List<Quest> quests) {
        return quests.stream().collect(Collectors.groupingBy(Quest::getReward)).entrySet().stream()
                .map(entry -> String.format("%d x %s", entry.getValue().size(), entry.getKey().getDescription()))
                .collect(Collectors.joining(", ", "Rewards: ", System.lineSeparator()));
    }

    private static String describeTotalCooldown(final List<? extends GeoPoint> points) {
        return String.format(
                "Total time in cool down: %s%n",
                Duration.ofSeconds((long) CooldownCalculator.calculateCost(points, CooldownCalculator::getCooldown)));
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
