package pogo.assistance.route.planning.conditional.bundle;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.NonNull;
import pogo.assistance.data.model.GeoPoint;

public class NOfAKindBundleValidator<U extends GeoPoint, V> implements BundleValidator<U, V> {

    private final Set<V> possibilities;
    private final Function<U, V> elementToBundleElementConverter;

    private int requiredCount;
    private V chosenPossibility = null;

    public NOfAKindBundleValidator(
            @NonNull final Collection<? extends V> patternElements,
            final int bundleSize,
            @NonNull final Function<U, V> mapper) {
        possibilities = new HashSet<>(patternElements);
        requiredCount = bundleSize;
        elementToBundleElementConverter = mapper;
    }

    @Override
    public synchronized boolean canAddToBundle(@Nonnull final U toBeAdded) {
        if (isComplete()) {
            return false;
        }

        if (chosenPossibility == null) {
            return possibilities.contains(elementToBundleElementConverter.apply(toBeAdded));
        } else {
            return chosenPossibility == elementToBundleElementConverter.apply(toBeAdded);
        }
    }

    @Override
    public synchronized void addToBundle(@Nonnull final U toBeAdded) {
        Preconditions.checkState(canAddToBundle(toBeAdded));
        if (chosenPossibility == null) {
            chosenPossibility = elementToBundleElementConverter.apply(toBeAdded);
        }
        requiredCount--;
    }

    @Override
    public synchronized boolean isComplete() {
        return requiredCount == 0;
    }
}
