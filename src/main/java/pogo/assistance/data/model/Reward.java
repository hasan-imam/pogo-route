package pogo.assistance.data.model;

import com.google.common.base.CaseFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable(intern = true)
public interface Reward {

    String getDescription();

    @Value.Derived
    default Optional<Double> getQuantity() {
        final Pattern numberExtractionPattern = Pattern.compile("[\\D]*(\\d+).*");
        final Matcher matcher = numberExtractionPattern.matcher(getDescription());
        if (matcher.find()) {
            try {
                return Optional.of(Double.parseDouble(matcher.group(1)));
            } catch (final RuntimeException ignored) {}
        }

        return Optional.empty();
    }

    @Value.Derived
    default RewardObject getRewardObject() {
        return Stream.of(RewardObject.values())
                .filter(rewardObject -> getDescription().matches(rewardObject.pattern))
                .findFirst()
                .orElse(RewardObject.UNKNOWN);
    }

    @RequiredArgsConstructor
    enum RewardObject {
        // Items
        GOLDEN_RAZZ_BERRY(".*([G|g]olden [R|r]azz).*"),
        SILVER_PINAP_BERRY(".*([S|s]ilver [P|p]inap).*"),
        STARDUST(".*([S|s]tardust).*"),
        RARE_CANDY(".*([R|r]are [C|c]and[\\w]*).*"),

        // Pokemons
        CHANSEY(".*([C|c]hansey).*"),
        DRATINI(".*([D|d]ratini).*"),
        MISDREAVUS(".*([M|m]isdreavus).*"),
        SPINDA(".*([S|s]pinda).*"),

        UNKNOWN(".*");

        /*
         * The above list is order dependent. Patterns are tried for match on the reward description in the order the
         * enums are defined. If there's a pattern that matches "pinap" and another that matches "silver pinap", the
         * more specific pattern (i.e. one that matches "silver pinap") needs to be earlier.
         */

        private final String pattern;

        public String getRewardName() {
            // Basically doing 'SOME_ENUM_NAME' -> 'Some Enum Name'
            return Arrays.stream(name().split("_"))
                    .map(String::toLowerCase)
                    .map(s -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, s))
                    .collect(Collectors.joining(" "));
        }
    }

}
