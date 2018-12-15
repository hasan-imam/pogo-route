package pogo.assistance.route.planning.conditional.bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pogo.assistance.data.model.GeoPoint;

@RequiredArgsConstructor
public class BundleBuilder<U extends GeoPoint, V> {

    private final BundleValidator<U, V> validator;
    private final BiFunction<U, U, Double> costFcn;

    private final List<U> bundleElements = new ArrayList<>();
    private double cost = 0;

    public synchronized boolean canAddToBundle(@NonNull final U toBeAdded) {
        return validator.canAddToBundle(toBeAdded);
    }

    public synchronized void addToBundle(@NonNull final U toBeAdded) {
        validator.addToBundle(toBeAdded);
        cost += bundleElements.isEmpty() ? 0 : costFcn.apply(bundleElements.get(bundleElements.size() - 1), toBeAdded);
        bundleElements.add(toBeAdded);
    }

    public synchronized boolean isComplete() {
        return validator.isComplete();
    }

    public Bundle<U> build() {
        return ImmutableBundle.<U>builder().addAllElements(bundleElements).cost(cost).build();
    }

}
