package pogo.assistance.ui.console;

import static pogo.assistance.ui.console.BundleDefinitionPrompt.promptAndDefineBundles;
import static pogo.assistance.ui.console.ConsoleInputUtils.promptAndSelectOne;
import static pogo.assistance.ui.console.ConsoleInputUtils.promptBoolean;
import static pogo.assistance.ui.console.ConsoleInputUtils.readStringToObject;
import static pogo.assistance.ui.console.ConsoleOutputUtils.printAvailableQuestDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Map;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.persistence.QuestRWUtils;
import pogo.assistance.data.quest.QuestProvider;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;
import pogo.assistance.route.planning.conditional.bundle.ImmutablePlannerConfig;
import pogo.assistance.route.planning.conditional.bundle.ImmutablePlannerConfig.Builder;
import pogo.assistance.route.planning.conditional.bundle.Tour;
import pogo.assistance.route.planning.conditional.bundle.TourPlanner;
import pogo.assistance.ui.console.di.DaggerConsoleAppComponent;

@Slf4j
public class ConsoleAppRunner {

    private final QuestProvider questProvider;
    private final QuestRWUtils questRWUtils;

    @Inject
    public ConsoleAppRunner(@NonNull final QuestProvider questProvider, @NonNull final QuestRWUtils questRWUtils) {
        this.questProvider = questProvider;
        this.questRWUtils = questRWUtils;
    }

    public static void main(final String[] args) {
        DaggerConsoleAppComponent.builder().build().getConsoleAppRunner().run();
    }

    private void run() {
        boolean isDoneWithApplication = false;
        do {
            final List<Quest> allAvailableQuests = promptAndPreparePoints();
            if (!allAvailableQuests.isEmpty()) {
                printAvailableQuestDetails(allAvailableQuests);
                boolean isDoneWithMap = false;
                do {
                    handleBundledPlanning(allAvailableQuests);
                    if (!promptBoolean("Would you like to do another planning with the same map? [y/n]")) {
                        isDoneWithMap = true;
                    }
                } while (!isDoneWithMap);
            }

            if (!promptBoolean("Would you like to try a different map? [y/n]")) {
                isDoneWithApplication = true;
                System.out.println("Great! Exiting application...");
            }
        } while (!isDoneWithApplication);
    }

    private static void handleBundledPlanning(final List<Quest> allAvailableQuests) {
        final List<? extends GeoPoint> points = new ArrayList<>(allAvailableQuests);
        final List<BundlePattern<GeoPoint, String>> bundlePatterns = promptAndDefineBundles(points);
        final Builder configBuilder = ImmutablePlannerConfig.builder();
        if (promptBoolean("Do you want to define a max tour distance?")) {
            configBuilder.maxTourDistance(
                    readStringToObject("Enter the max tour distance in kilometers:", Double::parseDouble));
        }
        final TourPlanner planner = new TourPlanner(configBuilder.build());
        System.out.println("Planning tour...");
        final Optional<Tour> planned = planner.plan(points, bundlePatterns);
        if (!planned.isPresent() || planned.get().getBundles().isEmpty()) {
            System.out.println("Planning produced no feasible tour.");
        } else {
            System.out.println("Planning completed!");
            ConsoleOutputUtils.promptAndOutputPlan(planned.get());
        }
    }

    /**
     * Filters {@code quests} if the user requests to only keep quests for whom abbreviations exist.
     */
    private static List<Quest> promptAndFilterAbbreviated(final List<Quest> quests) {
        if (ConsoleInputUtils.promptBoolean("Filter out any quest not in dictionary? [y/n]")) {
            final List<Quest> results = quests.stream()
                    .filter(quest -> quest.getAbbreviation().isPresent())
                    .collect(Collectors.toList());
            System.out.println(String.format("Filtered %d quests down to %d!", quests.size(), results.size()));
            return results;
        }

        return quests;
    }

    private List<Quest> promptAndPreparePoints() {
        final Map selectedMap = promptAndSelectOne(
                "Select a map:",
                Arrays.asList(Map.values()),
                Map::toString);
        final Optional<Date> latestFileTime = questRWUtils.getLatestDataFetchTime(selectedMap);
        if (latestFileTime.isPresent()) {
            final boolean doResuse = promptBoolean(String.format(
                    "Found previously fetched quest data from date: %s. Reuse? [y/n]", latestFileTime.get()));
            if (doResuse) {
                System.out.println(String.format("Reusing pre-fetched data for %s...", selectedMap));
                return questRWUtils.getLatestQuests(selectedMap);
            }
        }

        System.out.println(String.format("Fetching data for %s...", selectedMap));
        final List<Quest> quests = questProvider.getQuests(selectedMap);
        questRWUtils.writeQuests(quests, selectedMap);
        return quests;
    }
}
