package pogo.assistance.ui.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import pogo.assistance.data.model.Action;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.model.Reward;
import pogo.assistance.data.model.Task;
import pogo.assistance.data.quest.QuestDictionary;
import pogo.assistance.route.planning.conditional.bundle.Bundle;
import pogo.assistance.ui.TourDescriber;

@UtilityClass
public class ConsoleOutputUtils {

    public static void printAvailableQuestDetails(final List<Quest> quests) {
        final Map<Action, List<Quest>> groupedByAction = new HashMap<>();
        final Map<Reward, List<Quest>> groupedByReward = new HashMap<>();
        final Map<String, List<Quest>> groupedByAbbreviation = new HashMap<>();
        quests.forEach(quest -> {
            groupedByAction.computeIfAbsent(quest.getAction(), __ -> new ArrayList<>()).add(quest);
            groupedByReward.computeIfAbsent(quest.getReward(), __ -> new ArrayList<>()).add(quest);
            groupedByAbbreviation
                    .computeIfAbsent(
                            quest.getAbbreviation().orElse("Unknown abbreviation"),
                            __ -> new ArrayList<>())
                    .add(quest);
        });
        final List<Action> availableActions = new ArrayList<>(groupedByAction.keySet());
        availableActions.sort(Comparator.comparing(Action::getDescription));
        final List<Reward> availableRewards = new ArrayList<>(groupedByReward.keySet());
        availableRewards.sort(Comparator.comparing(Reward::getDescription));

        final StringBuilder availableQuestSummary = new StringBuilder();
        availableQuestSummary.append(String.format("Total %d quests%n.", quests.size()));

        availableQuestSummary.append(String.format("%nAvailable quests by abbreviation:%n"));
        groupedByAbbreviation.keySet().stream().sorted().forEach(abbr -> {
            // Note: we may get abbr that are not really an abbreviation, such as the 'Unknown abbreviation' added above
            final Optional<Task> lookedup = QuestDictionary.lookupByAbbreviation(abbr);
            lookedup.ifPresent(task -> {
                availableQuestSummary.append(String.format(
                        "%5s x %5s : %s -> %s%n",
                        groupedByAbbreviation.get(abbr).size(),
                        abbr,
                        task.getAction().getDescription(),
                        task.getReward().getDescription()));
            });
            if (!lookedup.isPresent()) {
                availableQuestSummary.append(String.format("%5s x %5s%n", groupedByAbbreviation.get(abbr).size(), abbr));
            }
        });

        availableQuestSummary.append(String.format("%nAvailable quests by action:%n"));
        availableActions.stream().sorted(Comparator.comparing(Action::getDescription)).forEach(action -> {
            groupedByAction.get(action).stream()
                    .collect(Collectors.groupingBy(Quest::getReward, Collectors.counting()))
                    .forEach((reward, count) -> {
                        availableQuestSummary.append(String.format(
                                "%5s x %s -> %s%n",
                                count,
                                action.getDescription(),
                                reward.getDescription()));
                    });
        });

        availableQuestSummary.append(String.format("%nAvailable quests by reward:%n"));
        availableRewards.stream().sorted(Comparator.comparing(Reward::getDescription)).forEach(reward -> {
            groupedByReward.get(reward).stream()
                    .collect(Collectors.groupingBy(Quest::getAction, Collectors.counting()))
                    .forEach((action, count) -> {
                        availableQuestSummary.append(String.format(
                                "%5s x %s <- %s%n",
                                count,
                                reward.getDescription(),
                                action.getDescription()));
                    });
        });

        System.out.println(availableQuestSummary.toString());
    }

    public static void promptAndOutputPlan(final List<? extends Bundle<? extends GeoPoint>> planned) {
        final List<String> visualizationOptions = Arrays.asList(
                "Give the general output",
                "Print markdown formatted so I can post on Discord",
                "Format so I can bulk enter on mapcustomizer.com",
                "Format so I can copy-paste on mapmakerapp.com",
                "No thanks, I'm done with the results");
        final TourDescriber tourDescriber = new TourDescriber(planned);
        boolean isDone = false;
        while (!isDone) {
            final String selected = ConsoleInputUtils.promptAndSelectOne(
                    "How would you like your results?",
                    visualizationOptions,
                    String::toString);

            switch (selected) {
                case "Give the general output":
                    System.out.println(tourDescriber.getGenericSummary());
                    System.out.println(tourDescriber.getGenericDescription());
                    break;
                case "Print markdown formatted so I can post on Discord":
                    System.out.println(tourDescriber.getDiscordPostWithMarkdown());
                    System.out.println();
                    break;
                case "Format so I can bulk enter on mapcustomizer.com":
                    System.out.println(tourDescriber.getFormattedForMapCustomizer());
                    break;
                case "Format so I can copy-paste on mapmakerapp.com":
                    System.out.println(tourDescriber.getFormattedForMapMakerapp());
                    break;
                case "No thanks, I'm done with the results":
                    isDone = true;
                    break;
            }
        }
    }

}
