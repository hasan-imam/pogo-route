package pogo.assistance.route;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.NonNull;
import pogo.assistance.data.model.GeoPoint;

// TODO: Fix the mix of Duration and Double representation of cooldown time
public class CooldownCalculator {

    private static final NavigableMap<Double, Double> KM_TO_SECOND_CD_TABLE;

    static {
        final NavigableMap<Double, Double> kmToSecondCd = new TreeMap<>();
        kmToSecondCd.put(0.5D,   0 * 60D);
        kmToSecondCd.put(1D,   0.1 * 60D);
        kmToSecondCd.put(2D,   1.5 * 60D);
        kmToSecondCd.put(3D,   2.5 * 60D);
        kmToSecondCd.put(4D,     3 * 60D);
        kmToSecondCd.put(5.5D,   4 * 60D);
        kmToSecondCd.put(6D,   4.5 * 60D);
        kmToSecondCd.put(7D,     5 * 60D);
        kmToSecondCd.put(10D,    6 * 60D);
        kmToSecondCd.put(11D,    7 * 60D);
        kmToSecondCd.put(12.7D,  8 * 60D);
        kmToSecondCd.put(15D,    9 * 60D);
        kmToSecondCd.put(18D,   10 * 60D);
        kmToSecondCd.put(20.5D, 11 * 60D);
        kmToSecondCd.put(22D,   13 * 60D);
        kmToSecondCd.put(24D,   14 * 60D);
        kmToSecondCd.put(26D,   15 * 60D);
        kmToSecondCd.put(28D,   16 * 60D);
        kmToSecondCd.put(30.5D, 17 * 60D);
        kmToSecondCd.put(40D,   18 * 60D);
        kmToSecondCd.put(42D,   19 * 60D);
        kmToSecondCd.put(53D,   21 * 60D);
        kmToSecondCd.put(73D,   22 * 60D);
        kmToSecondCd.put(78D,   23 * 60D);
        kmToSecondCd.put(88.5D, 24 * 60D);
        kmToSecondCd.put(93D,   26 * 60D);
        kmToSecondCd.put(105D,  27 * 60D);
        kmToSecondCd.put(113D,  28 * 60D);
        kmToSecondCd.put(133D,  31 * 60D);
        /*
         * Following numbers are odd because the original numbers had "less-than" condition. For example:
         *     Original mapping condition: "< 500 KM -> 62 mins cooldown"
         *     Map entry: key: 499, value: 62 * 60
         * This is necessary because we lookup cooldown get querying ceiling entry in this navigable map
         */
        kmToSecondCd.put(499D,  62 * 60D);
        kmToSecondCd.put(549D,  66 * 60D);
        kmToSecondCd.put(599D,  70 * 60D);
        kmToSecondCd.put(649D,  74 * 60D);
        kmToSecondCd.put(699D,  77 * 60D);
        kmToSecondCd.put(750D,  82 * 60D);
        kmToSecondCd.put(801D,  84 * 60D);
        kmToSecondCd.put(838D,  88 * 60D);
        kmToSecondCd.put(898D,  90 * 60D);
        kmToSecondCd.put(899D,  91 * 60D);
        kmToSecondCd.put(947D,  95 * 60D);
        kmToSecondCd.put(1006D, 98 * 60D);
        kmToSecondCd.put(1019D, 102 * 60D);
        kmToSecondCd.put(1099D, 104 * 60D);
        kmToSecondCd.put(1179D, 109 * 60D);
        kmToSecondCd.put(1199D, 111 * 60D);
        kmToSecondCd.put(1220D, 113 * 60D);
        kmToSecondCd.put(1299D, 117 * 60D);
        kmToSecondCd.put(1343D, 119 * 60D);
        kmToSecondCd.put(1500D, 2 * 60 * 60D);
        kmToSecondCd.put(Double.MAX_VALUE, 2 * 60 * 60D);
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
        return KM_DISTANCE_CACHE.computeIfAbsent(Arrays.hashCode(new Object[]{a, b}), __ -> getDistance(a, b, DistanceUnit.KM));
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