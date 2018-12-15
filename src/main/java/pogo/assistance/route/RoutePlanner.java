package pogo.assistance.route;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import pogo.assistance.data.model.GeoPoint;

class RoutePlanner {

    public List<GeoPoint> getOptimalRoute(
            final List<GeoPoint> geoPoints,
            final BiFunction<GeoPoint, GeoPoint, Number> costFunction) {
        final List<GeoPoint> route = new LinkedList<>();
        for (final GeoPoint geoPoint : geoPoints) {

        }
        geoPoints.forEach(geoPoint -> {
            // TODO: Implement
        });
        return route;
    }

    private Number[][] calculatePairwiseCost(
            final List<GeoPoint> geoPoints,
            final BiFunction<GeoPoint, GeoPoint, Number> costFunction) {
        final int n = geoPoints.size();
        final Number[][] costs = new Number[n][n];
        // Half of these calculations are redundant since cost is same both ways
        // TODO: Optimize
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                costs[i][j] = costFunction.apply(geoPoints.get(i), geoPoints.get(j));
            }
        }
        return costs;
    }

}
