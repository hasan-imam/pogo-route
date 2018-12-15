package pogo.assistance.route;

import java.util.List;
import java.util.function.BiFunction;
import pogo.assistance.data.model.GeoPoint;

interface Route {

    RoutePoint<?> getFirst();
    RoutePoint<?> getLast();
    List<RoutePoint<?>> getAsList();

    default Number getFullCost() {
        return getLast().getCostToThis();
    }

    default <P extends GeoPoint> void insert(
            final int position,
            final P geoPoint,
            final BiFunction<? extends GeoPoint, ? extends GeoPoint, Number> costFunction) { // TODO: need to make this work for all geo types
        final RoutePoint<P> routePoint = null; // Create one with the input
        if (position == getAsList().size()) {
            getLast().setNext(routePoint);
//            getLast().setCostFromThis(costFunction.apply(getLast().getGetPoint(), geoPoint));
        } else {
            getAsList().get(position); // TODO: index out of bound not handled
        }
    }

    interface RoutePoint<P extends GeoPoint> {
        RoutePoint<?> getPrevious();
        void setPrevious(final RoutePoint<?> previous);
        RoutePoint<?> getNext();
        void setNext(final RoutePoint<?> next);

        P getGetPoint();

        Number getCostToThis();
        void setCostToThis(final Number cost);
        Number getCostFromThis();
        void setCostFromThis(final Number cost);
    }

}
