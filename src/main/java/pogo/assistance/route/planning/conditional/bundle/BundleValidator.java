package pogo.assistance.route.planning.conditional.bundle;

import javax.annotation.Nonnull;
import pogo.assistance.data.model.GeoPoint;

interface BundleValidator<U extends GeoPoint, V> {

    boolean canAddToBundle(@Nonnull final U toBeAdded);

    void addToBundle(@Nonnull final U toBeAdded);

    boolean isComplete();

}
