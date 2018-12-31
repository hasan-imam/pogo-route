package pogo.assistance.route.planning.conditional.bundle;

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pogo.assistance.data.model.GeoPoint;

@RequiredArgsConstructor
public class BundleBuilder<U extends GeoPoint, V> {

    private final BundleValidator<U, V> validator;

    private final List<U> bundleElements = new ArrayList<>();

    public synchronized boolean canAddToBundle(@NonNull final U toBeAdded) {
        return validator.canAddToBundle(toBeAdded);
    }

    public synchronized void addToBundle(@NonNull final U toBeAdded) {
        validator.addToBundle(toBeAdded);
        bundleElements.add(toBeAdded);
    }

    public synchronized boolean isComplete() {
        return validator.isComplete();
    }

    public Bundle<U> build() {
        return ImmutableBundle.<U>builder().addAllElements(bundleElements).build();
    }

}
