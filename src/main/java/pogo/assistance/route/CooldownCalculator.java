package pogo.assistance.route;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.route.planning.conditional.bundle.Bundle;

// TODO: Fix the mix of Duration and Double representation of cooldown time
public class CooldownCalculator {

    private static final NavigableMap<Double, Double> KM_TO_SECOND_CD_TABLE;

    static {
        final NavigableMap<Double, Double> kmToSecondCd = new TreeMap<>();
        // Source: https://twitter.com/stardustpokmngo/status/873882137446957056
        kmToSecondCd.put(0D, 0D);
        kmToSecondCd.put(0.5D, 0D);
        kmToSecondCd.put(1D, 30D);
        kmToSecondCd.put(5D, 2 * 60D);
        kmToSecondCd.put(10D, 6 * 60D);
        kmToSecondCd.put(25D, 11 * 60D);
        kmToSecondCd.put(30D, 14 * 60D);
        kmToSecondCd.put(65D, 22 * 60D);
        kmToSecondCd.put(81D, 25 * 60D);
        kmToSecondCd.put(100D, 35 * 60D);
        kmToSecondCd.put(250D, 45 * 60D);
        kmToSecondCd.put(500D, 60 * 60D);
        kmToSecondCd.put(750D, 1.25 * 60 * 60D);
        kmToSecondCd.put(1000D, 1.50 * 60 * 60D);
        kmToSecondCd.put(1500D, 2 * 60 * 60D);
        KM_TO_SECOND_CD_TABLE = Collections.unmodifiableNavigableMap(kmToSecondCd);
    }

    private static final Map<Integer, Double> KM_DISTANCE_CACHE = new HashMap<>(10000);

    public static double calculateCost(
            @NonNull final List<? extends GeoPoint> geoPoints,
            @NonNull final BiFunction<? super GeoPoint, ? super GeoPoint, Double> costFcn) {
        double cost = 0;
        for (int i = 1; i < geoPoints.size(); i++) {
            cost += costFcn.apply(geoPoints.get(i - 1), geoPoints.get(i));
        }
        return cost;
    }

    public static <A extends GeoPoint, B extends GeoPoint> double getCooldown(final A a, final B b) {
        return cooldown(getDistance(a, b, DistanceUnit.KM), DistanceUnit.KM);
    }

    private static double cooldown(final double distance, final DistanceUnit unit) {
        switch (unit) {
            case KM:
                final Entry<Double, Double> entry = KM_TO_SECOND_CD_TABLE.ceilingEntry(distance);
                // TODO: need to update to make this more accurate
                return (entry == null) ? KM_TO_SECOND_CD_TABLE.lastEntry().getValue() : entry.getValue();
            default:
                throw new UnsupportedOperationException(String.format("Unit '%s' not supported", unit));
        }
    }

    public static <A extends GeoPoint, B extends GeoPoint> double getDistance(final A a, final B b) {
//        return KM_DISTANCE_CACHE.computeIfAbsent(Arrays.hashCode(new Object[]{a, b}), __ -> getDistance(a, b, DistanceUnit.KM));
        return getDistance(a, b, DistanceUnit.KM);
    }

    public static <A extends GeoPoint, B extends GeoPoint> double getDistance(
            @Nullable final A a,
            @Nullable final B b,
            @NonNull final DistanceUnit unit) {
        if (a == null || b == null || ((a.getLatitude() == b.getLatitude()) && (a.getLongitude() == b.getLongitude()))) {
            return 0;
        }
        else {
            final double theta = a.getLongitude() - b.getLongitude();
            double dist = sin(toRadians(a.getLatitude())) * sin(toRadians(b.getLatitude()))
                    + cos(toRadians(a.getLatitude())) * cos(toRadians(b.getLatitude())) * cos(toRadians(theta));
            dist = acos(dist);
            dist = toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit == DistanceUnit.KM) {
                dist = dist * 1.609344;
            } else if (unit == DistanceUnit.NAUTICAL_MILE) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }

    enum DistanceUnit {
        KM,
        MILE,
        NAUTICAL_MILE
    }

}