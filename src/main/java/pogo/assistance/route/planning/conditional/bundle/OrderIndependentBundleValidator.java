package pogo.assistance.route.planning.conditional.bundle;

import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import pogo.assistance.data.model.GeoPoint;

class OrderIndependentBundleValidator<U extends GeoPoint, V> implements BundleValidator<U, V> {
    private final List<V> requiredElements;
    private final Function<U, V> elementToBundleElementConverter;

    public OrderIndependentBundleValidator(final Collection<? extends V> patternElements, final Function<U, V> mapper) {
        requiredElements = new ArrayList<>(patternElements);
        elementToBundleElementConverter = mapper;
    }

    @Override
    public synchronized void addToBundle(@Nonnull final U toBeAdded) {
        Verify.verify(requiredElements.remove(elementToBundleElementConverter.apply(toBeAdded)));
    }

    @Override
    public boolean isComplete() {
        return requiredElements.isEmpty();
    }

    @Override
    public synchronized boolean canAddToBundle(@Nonnull U toBeAdded) {
        return requiredElements.contains(elementToBundleElementConverter.apply(toBeAdded));
    }
}
