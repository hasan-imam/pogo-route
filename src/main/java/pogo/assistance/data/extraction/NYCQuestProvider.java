package pogo.assistance.data.extraction;

import static pogo.assistance.data.quest.QuestDictionary.fromActionDescriptionToAbbreviation;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import pogo.assistance.data.model.ImmutableAction;
import pogo.assistance.data.model.ImmutableQuest;
import pogo.assistance.data.model.ImmutableReward;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.quest.QuestProvider;

public class NYCQuestProvider implements QuestProvider {

    static final Path TEST_RESOURCES_DIR =
            Paths.get("src", "test", "resources", "pogo", "assistance", "data", "extraction", "nycpokemap");

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String BASE_URL = "https://nycpokemap.com/quests.php";

    @Override
    public List<Quest> getQuests() {
        return Optional.of(readFromUrl(BASE_URL))
                .map(NYCQuestProvider::prepareQueryStringFromMetadata)
                .map(queryString -> BASE_URL + "?" + queryString)
                .map(NYCQuestProvider::readFromUrl)
                .map(NYCQuestProvider::parseQuestsFromQuestData)
                .orElseGet(Collections::emptyList);
    }

    @VisibleForTesting
    static String prepareQueryStringFromMetadata(final String metadataJson) {
        final JsonObject metaJson = new JsonParser().parse(metadataJson).getAsJsonObject();
        final JsonObject filters = metaJson.get("filters").getAsJsonObject();

        final List<String> questParams = new ArrayList<>();
        StreamSupport.stream(filters.getAsJsonArray("t2").spliterator(), false)
                .map(JsonElement::getAsString)
                .map(s -> "2,0," + s)
                .forEach(questParams::add);
        StreamSupport.stream(filters.getAsJsonArray("t3").spliterator(), false)
                .map(JsonElement::getAsString)
                .map(s -> "3," + s + ",0")
                .forEach(questParams::add);
        StreamSupport.stream(filters.getAsJsonArray("t7").spliterator(), false)
                .map(JsonElement::getAsString)
                .map(s -> "7,0," + s)
                .forEach(questParams::add);
        return questParams.stream()
                .map(s -> new BasicNameValuePair("quests[]", s))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        params -> URLEncodedUtils.format(params, StandardCharsets.UTF_8.name())));
    }

    @VisibleForTesting
    static List<Quest> parseQuestsFromQuestData(final String questDataJson) {
        return StreamSupport.stream(new JsonParser().parse(questDataJson).getAsJsonObject().getAsJsonArray("quests").spliterator(), true)
                .map(JsonElement::getAsJsonObject)
                .map(jsonObject -> {
                    final String actionDescription = jsonObject.get("conditions_string").getAsString();
                    final String rewardDescription = jsonObject.get("rewards_string").getAsString();
                    final ImmutableQuest.Builder questBuilder = ImmutableQuest.builder()
                            .latitude(jsonObject.get("lat").getAsDouble())
                            .longitude(jsonObject.get("lng").getAsDouble())
                            .action(ImmutableAction.builder().description(actionDescription).build())
                            .reward(ImmutableReward.builder().description(rewardDescription).build());
                    fromActionDescriptionToAbbreviation(actionDescription, rewardDescription)
                            .ifPresent(questBuilder::abbreviation);
                    return questBuilder.build();
                }).collect(Collectors.toList());
    }

    @VisibleForTesting
    static String readFromUrl(final String urlString) {
        final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        try {
            final HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(urlString));
            final HttpHeaders headers = request.getHeaders();
            headers.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36");
            headers.set("Time-Zone", "Europe/Amsterdam");
            headers.set("referer", "https://nycpokemap.com/quest.html");
            return request.execute().parseAsString();
        } catch (final IOException e) {
            throw new RuntimeException(String.format("Failed to get JSON from URL %s", urlString), e);
        }
    }

    public static QuestProvider createFileBasedProvider() {
        try {
            final byte[] bytes = Files.readAllBytes(TEST_RESOURCES_DIR.resolve("quest-query-output.json"));
            return () -> NYCQuestProvider.parseQuestsFromQuestData(new String(
                    bytes,
                    StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
