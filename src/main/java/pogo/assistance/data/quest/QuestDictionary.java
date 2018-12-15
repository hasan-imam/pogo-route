package pogo.assistance.data.quest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import pogo.assistance.data.model.ImmutableAction;
import pogo.assistance.data.model.ImmutableReward;
import pogo.assistance.data.model.ImmutableTask;
import pogo.assistance.data.model.Task;
import pogo.assistance.util.FileIOUtils;

public class QuestDictionary {

    private static final String MAPPING_DESC_FORMAT = "%s - %s";
    private static Map<String, String> DESC_TO_ABBR;
    private static Map<String, Task> ABBR_TO_TASK;

    public static Optional<String> fromActionDescriptionToAbbreviation(final String action, final String reward) {
        loadAbbreviations();
        return Optional.ofNullable(DESC_TO_ABBR.get(toDescToAbbrMapKey(action, reward)));
    }

    public static Optional<Task> lookupByAbbreviation(final String abbreviation) {
        loadAbbreviations();
        return Optional.ofNullable(ABBR_TO_TASK.get(abbreviation));
    }

    private static void loadAbbreviations() {
        if (DESC_TO_ABBR != null && ABBR_TO_TASK != null) {
            return;
        }

        final JsonObject abbreviationEntries = getDictionaryJson();
        final Map<String, String> descToAbbr = new HashMap<>();
        final Map<String, Task> abbrToTask = new HashMap<>();
        abbreviationEntries.entrySet().forEach(entry -> {
            final JsonObject mappingBody = entry.getValue().getAsJsonObject();
            mappingBody.getAsJsonArray("actions").forEach(action -> {
                mappingBody.getAsJsonArray("rewards").forEach(reward -> {
                    descToAbbr.put(
                            toDescToAbbrMapKey(action.getAsString(), reward.getAsString()),
                            entry.getKey());
                    // Create a task for the abbr with the first pair of action and reward string
                    abbrToTask.computeIfAbsent(entry.getKey(), __ -> ImmutableTask.builder()
                            .action(ImmutableAction.builder().description(action.getAsString()).build())
                            .reward(ImmutableReward.builder().description(reward.getAsString()).build())
                            .build());
                });
            });
        });
        DESC_TO_ABBR = Collections.unmodifiableMap(descToAbbr);
        ABBR_TO_TASK = Collections.unmodifiableMap(abbrToTask);
    }

    private static String toDescToAbbrMapKey(final String action, final String reward) {
        return String.format(MAPPING_DESC_FORMAT, action, reward);
    }

    private static JsonObject getDictionaryJson() {
        try {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setLenient(); // because this json file contains comments
            return gsonBuilder.create().fromJson(
                    new String(Files.readAllBytes(
                            FileIOUtils.resolvePackageLocalFilePath("quest-dictionary.txt", QuestDictionary.class)),
                            StandardCharsets.UTF_8),
                    JsonObject.class);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read quest dictionary: quest-dictionary.json", e);
        }
    }

}
