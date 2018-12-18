package pogo.assistance.data.extraction.ninedb;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.ExponentialBackOff;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import pogo.assistance.data.model.Action;
import pogo.assistance.data.model.ImmutableAction;
import pogo.assistance.data.model.ImmutableQuest;
import pogo.assistance.data.model.ImmutableReward;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.model.Reward;
import pogo.assistance.data.quest.QuestDictionary;
import pogo.assistance.data.quest.QuestProvider;

@Slf4j
public class NineDBQuestProvider implements QuestProvider {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final Pattern JSON_EXTRACTION_PATTERN = Pattern.compile("var result = (.*?);");

    private static final Map<String, String> PREFECTURE_TO_URL = ImmutableMap.<String, String>builder()
            .put("Hokkaido : Hokkaido", "https://9db.jp/pokemongo/data/4147?pref=%E5%8C%97%E6%B5%B7%E9%81%93")
            .put("Tohoku : Aomori", "https://9db.jp/pokemongo/data/4147?pref=%E9%9D%92%E6%A3%AE%E7%9C%8C")
            .put("Tohoku : Iwate", "https://9db.jp/pokemongo/data/4147?pref=%E5%B2%A9%E6%89%8B%E7%9C%8C")
            .put("Tohoku : Akita", "https://9db.jp/pokemongo/data/4147?pref=%E7%A7%8B%E7%94%B0%E7%9C%8C")
            .put("Tohoku : Miyagi", "https://9db.jp/pokemongo/data/4147?pref=%E5%AE%AE%E5%9F%8E%E7%9C%8C")
            .put("Tohoku : Yamagata", "https://9db.jp/pokemongo/data/4147?pref=%E5%B1%B1%E5%BD%A2%E7%9C%8C")
            .put("Tohoku : Fukushima", "https://9db.jp/pokemongo/data/4147?pref=%E7%A6%8F%E5%B3%B6%E7%9C%8C")
            .put("Kanto : Ibaraki", "https://9db.jp/pokemongo/data/4147?pref=%E8%8C%A8%E5%9F%8E%E7%9C%8C")
            .put("Kanto : Tochigi", "https://9db.jp/pokemongo/data/4147?pref=%E6%A0%83%E6%9C%A8%E7%9C%8C")
            .put("Kanto : Gunma", "https://9db.jp/pokemongo/data/4147?pref=%E7%BE%A4%E9%A6%AC%E7%9C%8C")
            .put("Kanto : Saitama", "https://9db.jp/pokemongo/data/4147?pref=%E5%9F%BC%E7%8E%89%E7%9C%8C")
            .put("Kanto : Chiba", "https://9db.jp/pokemongo/data/4147?pref=%E5%8D%83%E8%91%89%E7%9C%8C")
            .put("Kanto : Kanagawa", "https://9db.jp/pokemongo/data/4147?pref=%E7%A5%9E%E5%A5%88%E5%B7%9D%E7%9C%8C")
            .put("Kanto : Tokyo", "https://9db.jp/pokemongo/data/4147?pref=%E6%9D%B1%E4%BA%AC%E9%83%BD")
            .put("Chubu : Niigata", "https://9db.jp/pokemongo/data/4147?pref=%E6%96%B0%E6%BD%9F%E7%9C%8C")
            .put("Chubu : Toyama", "https://9db.jp/pokemongo/data/4147?pref=%E5%AF%8C%E5%B1%B1%E7%9C%8C")
            .put("Chubu : Ishikawa", "https://9db.jp/pokemongo/data/4147?pref=%E7%9F%B3%E5%B7%9D%E7%9C%8C")
            .put("Chubu : Fukui", "https://9db.jp/pokemongo/data/4147?pref=%E7%A6%8F%E4%BA%95%E7%9C%8C")
            .put("Chubu : Yamanashi", "https://9db.jp/pokemongo/data/4147?pref=%E5%B1%B1%E6%A2%A8%E7%9C%8C")
            .put("Chubu : Nagano", "https://9db.jp/pokemongo/data/4147?pref=%E9%95%B7%E9%87%8E%E7%9C%8C")
            .put("Chubu : Gifu", "https://9db.jp/pokemongo/data/4147?pref=%E5%B2%90%E9%98%9C%E7%9C%8C")
            .put("Chubu : Shizuoka", "https://9db.jp/pokemongo/data/4147?pref=%E9%9D%99%E5%B2%A1%E7%9C%8C")
            .put("Chubu : Aichi", "https://9db.jp/pokemongo/data/4147?pref=%E6%84%9B%E7%9F%A5%E7%9C%8C")
            .put("Kinki : Mie", "https://9db.jp/pokemongo/data/4147?pref=%E4%B8%89%E9%87%8D%E7%9C%8C")
            .put("Kinki : Shiga", "https://9db.jp/pokemongo/data/4147?pref=%E6%BB%8B%E8%B3%80%E7%9C%8C")
            .put("Kinki : Kyoto", "https://9db.jp/pokemongo/data/4147?pref=%E4%BA%AC%E9%83%BD%E5%BA%9C")
            .put("Kinki : Osaka", "https://9db.jp/pokemongo/data/4147?pref=%E5%A4%A7%E9%98%AA%E5%BA%9C")
            .put("Kinki : Hyogo", "https://9db.jp/pokemongo/data/4147?pref=%E5%85%B5%E5%BA%AB%E7%9C%8C")
            .put("Kinki : Nara", "https://9db.jp/pokemongo/data/4147?pref=%E5%A5%88%E8%89%AF%E7%9C%8C")
            .put("Kinki : Wakayama", "https://9db.jp/pokemongo/data/4147?pref=%E5%92%8C%E6%AD%8C%E5%B1%B1%E7%9C%8C")
            .put("Chugoku : Tottori", "https://9db.jp/pokemongo/data/4147?pref=%E9%B3%A5%E5%8F%96%E7%9C%8C")
            .put("Chugoku : Shimane", "https://9db.jp/pokemongo/data/4147?pref=%E5%B3%B6%E6%A0%B9%E7%9C%8C")
            .put("Chugoku : Okayama", "https://9db.jp/pokemongo/data/4147?pref=%E5%B2%A1%E5%B1%B1%E7%9C%8C")
            .put("Chugoku : Hiroshima", "https://9db.jp/pokemongo/data/4147?pref=%E5%BA%83%E5%B3%B6%E7%9C%8C")
            .put("Chugoku : Yamaguchi", "https://9db.jp/pokemongo/data/4147?pref=%E5%B1%B1%E5%8F%A3%E7%9C%8C")
            .put("Shikoku : Tokushima", "https://9db.jp/pokemongo/data/4147?pref=%E5%BE%B3%E5%B3%B6%E7%9C%8C")
            .put("Shikoku : Kagawa", "https://9db.jp/pokemongo/data/4147?pref=%E9%A6%99%E5%B7%9D%E7%9C%8C")
            .put("Shikoku : Ehime", "https://9db.jp/pokemongo/data/4147?pref=%E6%84%9B%E5%AA%9B%E7%9C%8C")
            .put("Shikoku : Kochi", "https://9db.jp/pokemongo/data/4147?pref=%E9%AB%98%E7%9F%A5%E7%9C%8C")
            .put("Kyushu : Fukuoka", "https://9db.jp/pokemongo/data/4147?pref=%E7%A6%8F%E5%B2%A1%E7%9C%8C")
            .put("Kyushu : Saga", "https://9db.jp/pokemongo/data/4147?pref=%E4%BD%90%E8%B3%80%E7%9C%8C")
            .put("Kyushu : Nagasaki", "https://9db.jp/pokemongo/data/4147?pref=%E9%95%B7%E5%B4%8E%E7%9C%8C")
            .put("Kyushu : Kumamoto", "https://9db.jp/pokemongo/data/4147?pref=%E7%86%8A%E6%9C%AC%E7%9C%8C")
            .put("Kyushu : Oita", "https://9db.jp/pokemongo/data/4147?pref=%E5%A4%A7%E5%88%86%E7%9C%8C")
            .put("Kyushu : Miyazaki", "https://9db.jp/pokemongo/data/4147?pref=%E5%AE%AE%E5%B4%8E%E7%9C%8C")
            .put("Kyushu : Kagoshima", "https://9db.jp/pokemongo/data/4147?pref=%E9%B9%BF%E5%85%90%E5%B3%B6%E7%9C%8C")
            .put("Kyushu : Okinawa", "https://9db.jp/pokemongo/data/4147?pref=%E6%B2%96%E7%B8%84%E7%9C%8C")
            .build();

    @Override
    public List<Quest> getQuests() {
        return PREFECTURE_TO_URL.keySet().stream()
                .map(NineDBQuestProvider::fetchAndProcessPrefecture)
                .flatMap(List::stream)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private static List<Quest> fetchAndProcessPrefecture(final String prefecture) {
        final String pageSource = readFromUrl(PREFECTURE_TO_URL.get(prefecture));
        final Matcher matcher = JSON_EXTRACTION_PATTERN.matcher(pageSource);
        Verify.verify(matcher.find(), "Page source didn't match expected pattern.");
        final JsonElement element = new JsonParser().parse(matcher.group(1));
        final List<Quest> quests = new ArrayList<>();
        // When no quest data is available, parsing results in:
        //   - "var result = null;" in matcher.group(0)
        //   - "null" in matcher.group(1)
        if (element != null && element.isJsonArray()) {
            element.getAsJsonArray().forEach(jsonElement -> quests.add(jsonDataToQuest(jsonElement.getAsJsonObject())));
        } else {
            log.warn(String.format("Quest data unavailable for: %s", prefecture));
        }
        log.trace(String.format("Found %s quests in %s prefecture.", quests.size(), prefecture));
        return quests;
    }

    private static String readFromUrl(final String urlString) {
        final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        try {
            final HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(urlString));
            request.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()));
            final HttpHeaders headers = request.getHeaders();
            headers.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36");
            headers.set("referer", "https://9db.jp/pokemongo");
            headers.setCookie("lang=en");
            return request.execute().parseAsString();
        } catch (final IOException e) {
            throw new RuntimeException(String.format("Failed to get data from URL %s", urlString), e);
        }
    }

    @VisibleForTesting
    static Quest jsonDataToQuest(final JsonObject jsonObject) {
        final Action action = ImmutableAction.builder()
                .description(jsonObject.get("task_name").getAsString())
                .build();
        final Reward reward = ImmutableReward.builder()
                .description(getRewardFromCode(
                        jsonObject.get("data_id").getAsInt(),
                        jsonObject.get("num1").getAsInt()))
                .build();
        return ImmutableQuest.builder()
                .action(action)
                .reward(reward)
                .abbreviation(QuestDictionary.fromActionDescriptionToAbbreviation(
                        action.getDescription(),
                        reward.getDescription()))
                .latitude(jsonObject.get("lat").getAsDouble())
                .longitude(jsonObject.get("lng").getAsDouble())
                .build();
    }

    private static String getRewardFromCode(final int id, final int number) {
        final String reward;
        switch (id) {
            case 81:
                reward = "Bulbasaur"; break;
            case 109:
                reward = "Charmander"; break;
            case 133:
                reward = "Squirtle"; break;
            case 159:
                reward = "Caterpie"; break;
            case 233:
                reward = "Clefairy"; break;
            case 234:
                reward = "Vulpix"; break;
            case 237:
                reward = "Wigglytuff"; break;
            case 239:
                reward = "Zubat"; break;
            case 264:
                reward = "Golduck"; break;
            case 268:
                reward = "Mankey"; break;
            case 280:
                reward = "Polywag"; break;
            case 302:
                reward = "Machop"; break;
            case 399:
                reward = "Gastly"; break;
            case 408:
                reward = "Onix"; break;
            case 420: // ༽つ۞﹏۞༼つ ─=≡ΣO))
                reward = "Krabby"; break;
            case 429:
                reward = "Voltorb"; break;
            case 446:
                reward = "Exeggcute"; break;
            case 474:
                reward = "Chansey"; break;
            case 484:
                reward = "Scyther"; break;
            case 485:
                reward = "Jynx"; break;
            case 486:
                reward = "Electabuzz"; break;
            case 487:
                reward = "Magmar"; break;
            case 488:
                reward = "Pinsir"; break;
            case 490:
                reward = "Magikarp"; break;
            case 493:
                reward = "Ditto"; break;
            case 494:
                reward = "Eevee"; break;
            case 498:
                reward = "Porygon"; break;
            case 503:
                reward = "Aerodactyl"; break;
            case 508:
                reward = "Dratini"; break;
            case 1334:
                reward = "Lanturn"; break;
            case 1363:
                reward = "Misdreavus"; break;
            case 1367:
                reward = "Pineco"; break;
            case 1378:
                reward = "Sneasel"; break;
            case 1366:
                reward = "Girafarig"; break;
            case 1387:
                reward = "Octillery"; break;
            case 1409:
                reward = "Larvitar"; break;
            case 1996:
                reward = "PokeBall"; break;
            case 1997:
                reward = "Great Ball"; break;
            case 1998:
                switch (number) {
                    case 1: reward = "1 Ultra Ball"; break;
                    case 2: reward = "2 Ultra Balls"; break;
                    case 5: reward = "5 Ultra Balls"; break;
                    case 10: reward = "10 Ultra Balls"; break;
                    default: reward = null; break;
                }
                break;
            case 2000:
                reward = "Razz Berry"; break;
            case 2001:
                reward = "Nanab Berry"; break;
            case 2002:
                reward = "Pinap Berry"; break;
            case 2006:
                switch (number) {
                    case 200: reward = "200 Stardust"; break;
                    case 500: reward = "500 Stardust"; break;
                    case 1000: reward ="1000 Stardust"; break;
                    case 1500: reward = "1500 Stardust"; break;
                    default: reward = null; break;
                }
                break;
            case 2012:
                reward = "Potion"; break;
            case 2013:
                reward = "Super Potion"; break;
            case 2014:
                reward = "Hyper Potion"; break;
            case 2015:
                switch(number) {
                    case 3: reward = "3 Max Potions"; break;
                    default: reward = null; break;
                }
                break;
            case 2016:
                reward = "Revive"; break;
            case 2017:
                switch (number) {
                    case 1: reward = "1 Max Revive"; break;
                    case 3: reward = "3 Max Revives"; break;
                    default: reward = null; break;
                }
                break;
            case 2295:
                switch(number) {
                    case 2: reward = "2 Golden Razz Berries"; break;
                    default: reward = null; break;
                }
                break;
            case 2296:
                switch(number) {
                    case 1: reward = "1 Rare Candy"; break;
                    case 3: reward ="3 Rare Candies"; break;
                    default: reward = null; break;
                }
                break;
            case 2511:
                reward = "Nincada"; break;
            case 2523:
                reward = "Sableye"; break;
            case 2536:
                reward= "Roselia"; break;
            case 2541:
                reward = "Wailmer"; break;
            case 2543:
                reward = "Numel"; break;
            case 2548:
                reward = "Spinda"; break;
            case 2570:
                reward = "Feebas"; break;
            case 4193:
                reward = "Pokemon (Don't Exist)"; break;
            case 4518:
                switch(number) {
                    case 3: reward = "3 Silver Pinap Berries"; break;
                    default: reward = null; break;
                }
                break;
            default:
                reward = null; break;
        }

        Verify.verify(reward != null, String.format(
                "Encountered unknown id (%s) and number (%d) could not be mapped to a reward.", id, number));
        return reward;
    }

}
