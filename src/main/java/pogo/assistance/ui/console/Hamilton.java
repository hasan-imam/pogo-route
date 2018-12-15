package pogo.assistance.ui.console;

import static pogo.assistance.ui.console.BundleDefinitionPrompt.promptAndDefineBundles;
import static pogo.assistance.ui.console.ConsoleInputUtils.promptAndSelectOne;
import static pogo.assistance.ui.console.ConsoleOutputUtils.printAvailableQuestDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import pogo.assistance.data.extraction.NYCQuestProvider;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.quest.QuestProvider;
import pogo.assistance.route.CooldownCalculator;
import pogo.assistance.route.planning.conditional.bundle.Bundle;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;
import pogo.assistance.route.planning.conditional.bundle.BundledTourPlanner;

@Slf4j
public class Hamilton {

    private static final Map<String, QuestProvider> QUEST_PROVIDER_MAP;

    static {
        // TODO: consider not constructing these in static setter
        // TODO: make the map immutable
        final Map<String, QuestProvider> questProviderByLocation = new HashMap<>();
        questProviderByLocation.put("NYC", new NYCQuestProvider());
        QUEST_PROVIDER_MAP = questProviderByLocation;
    }

    public static void main(final String[] args) {
        // Temporary code - useful while people gets used to the app. TODO: remove once everyone is okay with the UI.
        final boolean isTrial = ConsoleInputUtils.promptBoolean("Do you want to do a trial run? [y/n]" +
                " Trial run gets the quest details from a dummy file" +
                " instead of querying real websites every time you run application.");
        if (isTrial) {
            QUEST_PROVIDER_MAP.put("NYC", NYCQuestProvider.createFileBasedProvider());
        }
        // Temporary code

        // TODO: Let user supply quests from an existing file.
        final String selected = promptAndSelectOne(
                "Select a map:",
                new ArrayList<>(QUEST_PROVIDER_MAP.keySet()),
                String::toString);
        final List<Quest> allAvailableQuests = getQuests(QUEST_PROVIDER_MAP.get(selected));
        verifyNonEmptyPoints(allAvailableQuests);
        printAvailableQuestDetails(allAvailableQuests);
        handleBundledPlanning(allAvailableQuests);
    }

    private static List<Quest> getQuests(final QuestProvider questProvider) {
        // TODO: need to add some form of 'caching' so we don't query websites every time this runs. May want to store
        // quest data in some local file and re-use it. Such mechanism may also be added to the providers themselves (?)
        // Need to think more about this.
        final List<Quest> quests = questProvider.getQuests();
        System.out.println(String.format("Got %d quests from map. Printing further details...", quests.size()));
        return quests;
    }

    private static void handleBundledPlanning(final List<Quest> allAvailableQuests) {
        final List<BundlePattern<GeoPoint, String>> bundlePatterns = promptAndDefineBundles(allAvailableQuests);
        final BundledTourPlanner planner = new BundledTourPlanner(
                CooldownCalculator::getDistance,
                allAvailableQuests,
                -1, // TODO: Let user select a max cooldown time
                bundlePatterns);
        System.out.println("Planning tour...");
        final List<? extends Bundle<? extends GeoPoint>> planned = planner.plan();
        if (planned.isEmpty()) {
            System.out.println("Planning produced no feasible tour.");
        } else {
            System.out.println("Planning completed!");
            ConsoleOutputUtils.promptAndOutputPlan(planned);
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

    private static <T extends GeoPoint> List<T> verifyNonEmptyPoints(final List<T> points) {
        if (points.isEmpty()) {
            System.out.println("No points to use for planning. Exiting application...");
            System.exit(0);
        }
        return points;
    }
}
