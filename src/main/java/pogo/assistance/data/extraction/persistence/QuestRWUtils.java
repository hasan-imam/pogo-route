package pogo.assistance.data.extraction.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import pogo.assistance.data.model.Quest;

@Slf4j
@UtilityClass
public class QuestRWUtils {

    private static final int MAX_FILE_COUNT_PER_MAP = 3;
    private static final String QUEST_DIR_ROOT = "quests";
    private static final String QUEST_FILE_NAME_PREFIX = "quests-";
    private static final String QUEST_FILE_NAME_SUFFIX = ".json";
    private static final Pattern DATE_EXTRACTION_PATTERN =
            Pattern.compile(QUEST_FILE_NAME_PREFIX + "(.*?)" + QUEST_FILE_NAME_SUFFIX);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

    private static final Gson GSON = buildGson();

    public void writeQuests(@NonNull final List<Quest> quests, @NonNull final String map) {
        if (quests.isEmpty()) {
            return;
        }

        final Path path = getMapDirectory(map).resolve(
                QUEST_FILE_NAME_PREFIX + DATE_FORMAT.format(new Date()) + QUEST_FILE_NAME_SUFFIX);
        try {
            Files.write(path, GSON.toJson(quests, new TypeToken<List<Quest>>(){}.getType()).getBytes());
            deleteOldFiles(map);
        } catch (final IOException e) {
            throw new RuntimeException(String.format("Failed to write JSON quest data to file: %s", path), e);
        }
    }

    public List<Quest> getLatestQuests(final String map) {
        return getLatestQuestFile(map)
                .map(path -> {
                    try {
                        return Files.readAllBytes(path);
                    } catch (final IOException e) {
                        log.warn(String.format("Failed to read quest file: %s", path), e);
                        return null;
                    }
                }).map(String::new)
                .map(jsonString -> GSON.fromJson(jsonString, Quest[].class))
                .map(Arrays::asList)
                .map(Collections::unmodifiableList)
                .orElse(Collections.emptyList());
    }

    public Optional<Date> getLatestDataFetchTime(final String map) {
        return getLatestQuestFile(map).map(QuestRWUtils::getDateFromQuestFileName);
    }

    private void deleteOldFiles(final String map) {
        final Path mapDirectory = getMapDirectory(map);
        try {
            Files.walk(mapDirectory)
                    .filter(path -> DATE_EXTRACTION_PATTERN.matcher(path.getFileName().toString()).find())
                    .filter(path -> getDateFromQuestFileName(path) != null)
                    .sorted(Comparator.comparing(QuestRWUtils::getDateFromQuestFileName).reversed())
                    .skip(MAX_FILE_COUNT_PER_MAP)
                    .peek(path -> log.trace(String.format("Deleting file: %s", path)))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (final IOException e) {
                            log.warn(String.format("Failed to delete old file: %s", path));
                        }
                    });
        } catch (final IOException e) {
            log.warn(String.format("Failed to read map directory: %s", mapDirectory));
        }
    }

    private Optional<Path> getLatestQuestFile(final String map) {
        final Path mapDirectory = getMapDirectory(map);
        try {
            return Files.walk(mapDirectory)
                    .filter(path -> DATE_EXTRACTION_PATTERN.matcher(path.getFileName().toString()).find())
                    .filter(path -> getDateFromQuestFileName(path) != null)
                    .max(Comparator.comparing(QuestRWUtils::getDateFromQuestFileName));
        } catch (final IOException e) {
            log.warn(String.format("Failed to read map directory: %s", mapDirectory));
            return Optional.empty();
        }
    }

    @Nullable
    private static Date getDateFromQuestFileName(final Path questFilePath) {
        return Optional.of(questFilePath.getFileName().toString())
                .map(DATE_EXTRACTION_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .map(source -> {
                    try {
                        return DATE_FORMAT.parse(source);
                    } catch (final ParseException e) {
                        log.warn(String.format("Encountered file name '%s' with unexpected pattern", questFilePath), e);
                        return null;
                    }
                }).orElse(null);
    }

    private static Path getMapDirectory(final String map) {
        final Path mapDirectory = Paths.get(QUEST_DIR_ROOT, map);
        try {
            return Files.createDirectories(mapDirectory);
        } catch (final IOException e) {
            throw new RuntimeException(
                    String.format("Failed to create directory (%s) for %s map", mapDirectory, map));
        }
    }

    private static Gson buildGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        // Register adapters for GSON ser-de (https://immutables.github.io/json.html#type-adapter-registration)
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);

        gsonBuilder.setPrettyPrinting();
        return gsonBuilder.create();
    }

}
