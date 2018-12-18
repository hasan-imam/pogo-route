package pogo.assistance.ui.console;

import static pogo.assistance.ui.console.BundleDefinitionPrompt.promptAndDefineBundles;
import static pogo.assistance.ui.console.ConsoleInputUtils.promptAndSelectOne;
import static pogo.assistance.ui.console.ConsoleInputUtils.promptBoolean;
import static pogo.assistance.ui.console.ConsoleOutputUtils.printAvailableQuestDetails;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import pogo.assistance.data.extraction.ninedb.NineDBQuestProvider;
import pogo.assistance.data.extraction.persistence.QuestRWUtils;
import pogo.assistance.data.extraction.pokemap.PokemapQuestProvider;
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
        questProviderByLocation.put("NYC", new PokemapQuestProvider("NYC"));
        questProviderByLocation.put("SG", new PokemapQuestProvider("SG"));
        questProviderByLocation.put("JP", new NineDBQuestProvider());

        QUEST_PROVIDER_MAP = questProviderByLocation;
    }

    public static void main(final String[] args) {
        // Temporary code - useful while people gets used to the app. TODO: remove once everyone is okay with the UI.
        final boolean isTrial = ConsoleInputUtils.promptBoolean("Do you want to do a trial run? [y/n]" +
                System.lineSeparator() +
                "Trial run gets the quest details from a dummy file" +
                " instead of querying real websites every time you run application.");
        if (isTrial) {
            final QuestProvider staticFileBasedProvider = PokemapQuestProvider.createFileBasedProvider();
            QUEST_PROVIDER_MAP.put("NYC", staticFileBasedProvider);
            QUEST_PROVIDER_MAP.put("SG", staticFileBasedProvider);
            QUEST_PROVIDER_MAP.put("JP", staticFileBasedProvider);
        }
        // Temporary code

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
        final BundledTourPlanner planner = new BundledTourPlanner(
                CooldownCalculator::getDistance,
                points,
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

    private static List<Quest> promptAndPreparePoints() {
        final String selectedMap = promptAndSelectOne(
                "Select a map:",
                new ArrayList<>(QUEST_PROVIDER_MAP.keySet()),
                String::toString);
        final Optional<Date> latestFileTime = QuestRWUtils.getLatestDataFetchTime(selectedMap);
        if (latestFileTime.isPresent()) {
            final boolean doResuse = promptBoolean(String.format(
                    "Found previously fetched quest data from date: %s. Reuse? [y/n]", latestFileTime.get()));
            if (doResuse) {
                System.out.println(String.format("Reusing pre-fetched data for %s...", selectedMap));
                return QuestRWUtils.getLatestQuests(selectedMap);
            }
        }

        System.out.println(String.format("Fetching data for %s...", selectedMap));
        final List<Quest> quests = QUEST_PROVIDER_MAP.get(selectedMap).getQuests();
        QuestRWUtils.writeQuests(quests, selectedMap);
        return quests;
    }
}
