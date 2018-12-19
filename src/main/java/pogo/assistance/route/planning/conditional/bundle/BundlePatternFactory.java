package pogo.assistance.route.planning.conditional.bundle;

import java.util.Collection;
import java.util.function.Function;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Nest;
import pogo.assistance.data.model.Quest;

public class BundlePatternFactory {

    public static <U extends GeoPoint, V> BundlePattern<U, V> createAnyOfASetPattern(
            final Collection<? extends V> set,
            final int bundleSize,
            final Function<U, V> mapper) {
        return () -> new AnyOfASetBundleValidator<>(set, bundleSize, mapper);
    }

    public static <U extends GeoPoint, V> BundlePattern<U, V> createNOfAKindPattern(
            final Collection<? extends V> patternElements,
            final int bundleSize,
            final Function<U, V> mapper) {
        return () -> new NOfAKindBundleValidator<>(patternElements, bundleSize, mapper);
    }

    public static <U extends GeoPoint, V> BundlePattern<U, V> createOrderIndependentPattern(
            final Collection<? extends V> patternElements,
            final Function<U, V> mapper) {
        return () -> new OrderIndependentBundleValidator<>(patternElements, mapper);
    }

    public static <U extends GeoPoint, V> BundlePattern<U, V> createOrderDependentPattern(
            final Collection<? extends V> patternElements,
            final Function<U, V> mapper) {
        return () -> new OrderDependentBundleValidator<>(patternElements, mapper);
    }

    public static Function<GeoPoint, String> getGenericMapper() {
        return BundlePatternFactory::genericMapper;
    }

    private static String genericMapper(final GeoPoint geoPoint) {
        if (geoPoint instanceof Quest) {
            final Quest quest = (Quest) geoPoint;
            return quest.getAbbreviation()
                    .orElseGet(() -> String.format(
                            "%s - %s",
                            quest.getAction().getDescription(),
                            quest.getReward().getDescription()));
        } else if (geoPoint instanceof Nest) {
            return ((Nest) geoPoint).getDescription();
        } else {
            throw new IllegalArgumentException(
                    String.format("Generic mapper cannot process type: %s", geoPoint.getClass().getSimpleName()));
        }
    }
}
