package pogo.assistance.route.planning.conditional.bundle;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.NonNull;
import pogo.assistance.data.model.GeoPoint;

public class AnyOfASetBundleValidator<U extends GeoPoint, V> implements BundleValidator<U, V> {

    private final Set<V> possibilities;
    private final Function<U, V> elementToBundleElementConverter;

    private int requiredCount;

    public AnyOfASetBundleValidator(
            @NonNull final Collection<? extends V> patternElements,
            final int bundleSize,
            @NonNull final Function<U, V> mapper) {
        possibilities = Collections.unmodifiableSet(new HashSet<>(patternElements));
        requiredCount = bundleSize;
        elementToBundleElementConverter = mapper;
    }

    @Override
    public synchronized boolean canAddToBundle(@Nonnull final U toBeAdded) {
        return !isComplete() && possibilities.contains(elementToBundleElementConverter.apply(toBeAdded));
    }

    @Override
    public synchronized void addToBundle(@Nonnull final U toBeAdded) {
        Preconditions.checkState(canAddToBundle(toBeAdded));
        requiredCount--;
    }

    @Override
    public synchronized boolean isComplete() {
        return requiredCount == 0;
    }

}
