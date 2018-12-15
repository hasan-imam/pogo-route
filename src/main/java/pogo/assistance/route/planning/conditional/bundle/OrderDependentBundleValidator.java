package pogo.assistance.route.planning.conditional.bundle;

import com.google.common.base.Verify;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
import javax.annotation.Nonnull;
import pogo.assistance.data.model.GeoPoint;

class OrderDependentBundleValidator<U extends GeoPoint, V> implements BundleValidator<U, V> {
    private final Queue<V> requiredElements;
    private final Function<U, V> elementToBundleElementConverter;

    public OrderDependentBundleValidator(final Collection<? extends V> patternElements, final Function<U, V> mapper) {
        requiredElements = new LinkedList<>(patternElements);
        elementToBundleElementConverter = mapper;
    }

    @Override
    public synchronized void addToBundle(@Nonnull final U toBeAdded) {
        Verify.verify(canAddToBundle(toBeAdded));
        requiredElements.remove();
    }

    @Override
    public boolean isComplete() {
        return requiredElements.isEmpty();
    }

    @Override
    public synchronized boolean canAddToBundle(@Nonnull final U toBeAdded) {
        return Optional.ofNullable(requiredElements.peek())
                .filter(v -> elementToBundleElementConverter.apply(toBeAdded).equals(v))
                .isPresent();
    }
}
