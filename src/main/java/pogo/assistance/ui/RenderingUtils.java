package pogo.assistance.ui;

import com.google.common.base.Strings;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RenderingUtils {

    public static int getHoursLeftInDay(final pogo.assistance.data.model.Map map) {
        final ZoneId z;
        switch (map) {
            // Could have used a map here, but there can be a map that spans multiple time zones, making a 1:1 map:zone
            // mapping invalid. Plus the list of map is short enough. So going with a simple switch statement.
            case JP:
                z = ZoneId.of("Asia/Tokyo"); break;
            case NYC:
                z = ZoneId.of("-05:00"); break;
            case SG:
                z = ZoneId.of("+08:00"); break;
            default:
                throw new IllegalArgumentException();
        }
        final ZonedDateTime zdt = ZonedDateTime.now(z) ;
        long hoursIntoTheDay = ChronoUnit.HOURS.between(zdt, zdt.toLocalDate().atStartOfDay(z).plusDays(1));
        return (int) hoursIntoTheDay;
    }

    public static String toString(final Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

    public static String toBulletPoints(final String header, final Collection<String> lines, int level) {
        if (lines.isEmpty()) {
            return "";
        }

        final String linePrefix = ((level > 0) ? Strings.repeat(" ", level * 2) : "") + " * ";
        final StringBuilder sb = new StringBuilder();
        lines.forEach(line -> sb.append(System.lineSeparator()).append(linePrefix).append(line));
        final String bullets = lines.stream()
                .map(line -> linePrefix + line)
                .collect(Collectors.joining(System.lineSeparator()));
        if (header != null && !header.isEmpty()) {
            return ((level > 0) ? linePrefix.replaceFirst("  ", "") : "")
                    + header + System.lineSeparator() + bullets;
        } else {
            return bullets;
        }
    }

    public static <T> List<String> describeClassification(final List<? extends T> things, final Function<? super T, String> classifier) {
        final Map<String, Long> classifications = things.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));
        return classifications.entrySet().stream()
                .sorted(Comparator.comparing(Entry::getKey))
                .map(entry -> String.format("%d x %s", entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }
}
