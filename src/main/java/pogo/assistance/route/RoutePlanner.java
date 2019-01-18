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
            costs[i][i]=0; // I don't remember if java's default is 0, if it is, this line is unnecessary
            for (int j = i+1; j < n; j++) {
                // since there is no loopback, we can ignore [i][i] and since graph is undirected [i][j] is the same as [j][i]
                costs[i][j] = costFunction.apply(geoPoints.get(i), geoPoints.get(j));
                costs[j][i] =  costs[i][j];
            }
        }
        return costs;
    }

}
