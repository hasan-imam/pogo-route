package pogo.assistance.route.planning.conditional.bundle;

import javax.annotation.Nonnull;
import pogo.assistance.data.model.GeoPoint;

public interface BundlePattern<U extends GeoPoint, V> {

    @Nonnull BundleValidator<U, V> createValidator();

}
