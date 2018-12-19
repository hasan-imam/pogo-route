package pogo.assistance.ui.console;

import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createAnyOfASetPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createNOfAKindPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createOrderDependentPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.createOrderIndependentPattern;
import static pogo.assistance.route.planning.conditional.bundle.BundlePatternFactory.getGenericMapper;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.route.planning.conditional.bundle.BundlePattern;

@Slf4j
class BundleDefinitionPrompt {

    /**
     * @param points
     *      TODO: Implement validation of input pattern elements so that user is warned if mentioned element is absent
     *      among the available points.
     */
    public static List<BundlePattern<GeoPoint, String>> promptAndDefineBundles(final List<? extends GeoPoint> points) {
        final List<BundlePattern<GeoPoint, String>> patterns = new ArrayList<>();
        final Set<String> allPatternElements = new HashSet<>();
        boolean done = false;
        while (!done) {
            final BundleDefinitionOptions selected = ConsoleInputUtils.promptAndSelectOne(
                    "Define one or more bundles. Choose one of the following type of definitions:",
                    Arrays.asList(BundleDefinitionOptions.values()),
                    opt -> opt.getName() + ": " + opt.getDescription());
            switch (selected) {
                case N_OF_A_KIND:
                case ANY_OF_A_SET:
                    patterns.add(ConsoleInputUtils.readStringToObject(selected.getPrompt(), input -> {
                        try {
                            final String[] args = input.split("[xX]", 2);
                            final int bundleSize = Integer.parseInt(args[0].trim());
                            final List<String> patternElements = new ArrayList<>();
                            new JsonParser().parse(args[1]).getAsJsonArray().forEach(patternElement -> {
                                patternElements.add(patternElement.getAsString());
                            });
                            if (patternElements.isEmpty() || bundleSize <= 0) {
                                return null;
                            }
                            allPatternElements.addAll(patternElements);
                            if (selected == BundleDefinitionOptions.N_OF_A_KIND) {
                                return createNOfAKindPattern(patternElements, bundleSize, getGenericMapper());
                            } else {
                                return createAnyOfASetPattern(patternElements, bundleSize, getGenericMapper());
                            }
                        } catch (final RuntimeException e) {
                            return null;
                        }
                    }));
                    break;
                case ORDERED_SET:
                case UNORDERED_SET:
                    patterns.add(ConsoleInputUtils.readStringToObject(selected.getPrompt(), input -> {
                        try {
                            final List<String> patternElements = new ArrayList<>();
                            new JsonParser().parse(input).getAsJsonArray()
                                    .forEach(patternElement -> patternElements.add(patternElement.getAsString()));
                            if (patternElements.isEmpty()) {
                                return null;
                            }
                            allPatternElements.addAll(patternElements);
                            if (selected == BundleDefinitionOptions.ORDERED_SET) {
                                return createOrderDependentPattern(patternElements, getGenericMapper());
                            } else {
                                return createOrderIndependentPattern(patternElements, getGenericMapper());
                            }
                        } catch (final RuntimeException e) {
                            return null;
                        }
                    }));
                    break;
            }

            if (!ConsoleInputUtils.promptBoolean("Want to define more bundles? [y/n]")) {
                done = true;
            }
        }

        log.trace("Starting removal of points irrelevant to the patterns defined. Number of points: " + points.size());
        points.removeIf(point -> !allPatternElements.contains(getGenericMapper().apply(point)));
        log.trace("Number of points left after removing the ones irrelevant to defined patterns: " + points.size());

        return patterns;
    }

    @Getter
    @RequiredArgsConstructor
    private enum BundleDefinitionOptions {
        N_OF_A_KIND(
                "N of a kind",
                "Each bundle will contain N elements of the same type. You can define a set of valid types.",
                "3 x ['3B10', '3G15', 'DT15']"),
        ANY_OF_A_SET(
                "Any of a set of things",
                "Each bundle will contain N elements of mixed types. You can define a set of valid types.",
                "3 x ['3GC', '3GT']"),
        ORDERED_SET(
                "Ordered set of things",
                "Each bundle will contain elements of type and order you define.",
                "['DRC', 'DRC', 'DRT']"),
        UNORDERED_SET(
                "Unordered set of things",
                "Each bundle will contain one element of each type you mention," +
                        " ignoring the order in which you define them.",
                "['DRC', 'DRC', 'DRT']");

        private final String name;
        private final String description;
        private final String example;

        public String getPrompt() {
            return String.format("Define bundle of type: %s. Example input: %s", name, example);
        }
    }

}
