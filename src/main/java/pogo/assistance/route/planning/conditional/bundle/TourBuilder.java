package pogo.assistance.route.planning.conditional.bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.experimental.Delegate;
import pogo.assistance.data.model.GeoPoint;

public class TourBuilder {

    @Delegate
    private Tour tour = ImmutableTour.builder().bundles(Collections.emptyList()).build(); // TODO: Fix unsafe calls when null

    public void append(final Bundle<? extends GeoPoint> bundle) {
        if (tour == null) {
            tour = ImmutableTour.builder().bundles(Collections.singletonList(bundle)).build();
        } else {
            final List<? super Bundle<? extends GeoPoint>> bundles = new ArrayList<>(tour.getBundles());
            bundles.add(bundle);
            tour = ImmutableTour.builder().bundles((List<? extends Bundle<? extends GeoPoint>>) bundles).build();
        }
    }

    public Tour build() {
        return tour;
    }
}
