package pogo.assistance.route.planning.conditional.bundle;

import static java.util.Collections.unmodifiableSet;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import pogo.assistance.data.model.GeoPoint;

public class BundledTourPlanner {

    private final BiFunction<? super GeoPoint, ? super GeoPoint, Double> costFcn;
    private final List<? extends GeoPoint> points;
    private final double costLimit;
    private final List<BundlePattern<GeoPoint, String>> patterns;

    private final AtomicReference<Double> minCost = new AtomicReference<>(Double.MAX_VALUE);
    private final AtomicReference<List<? extends Bundle<? extends GeoPoint>>> shortestTour =
            new AtomicReference<>(Collections.emptyList());

    public BundledTourPlanner(
            final BiFunction<? super GeoPoint, ? super GeoPoint, Double> costFcn,
            final List<? extends GeoPoint> points,
            final double costLimit,
            final List<BundlePattern<GeoPoint, String>> patterns) {
        this.costFcn = costFcn;
        this.points = points;
        this.costLimit = (costLimit == -1) ? Double.MAX_VALUE : costLimit;
        this.patterns = patterns;
    }

    public synchronized List<? extends Bundle<? extends GeoPoint>> plan() {
        if (points.isEmpty()) {
            return Collections.emptyList();
        }

        points.stream().parallel().forEach(this::planAndUpdateBestTour);

        minCost.set(Double.MAX_VALUE);
        return shortestTour.getAndSet(null);
    }

    private void planAndUpdateBestTour(final GeoPoint startingPoint) {
        final Set<? extends GeoPoint> possibilities = new HashSet<>(points);
        final List<Bundle<? super GeoPoint>> result = new ArrayList<>();
        final AtomicDouble cost = new AtomicDouble(0);
        final AtomicReference<GeoPoint> lastPoint = new AtomicReference<>();
        Optional<? extends Bundle<? extends GeoPoint>> cheapestNextBundle = Optional.empty();
        do {
            /*
             * Select next bundle:
             *  1. Sort possibilities by the cost of reaching them from lastPoint
             *  2. For each of the possibilities in that sorted order, get the cheapest bundle we can create with the
             *     starting point at that possibility.
             *  3. Pick the first bundle we get from above.
             *
             * This essentially gives us the bundle that can be created from the closest of the possibilities and is the
             * cheapest among the bundles possible to create from that point.
             *
             * Even if there are possibilities remaining, loop terminates if we don't manage to get a bundle with above
             * procedure.
             */
            cheapestNextBundle = possibilities.stream()
                    .sorted(Comparator.comparingDouble(point -> costFcn.apply((lastPoint.get() == null) ? point : lastPoint.get(), point)))
                    .map(point -> createCheapestNextBundle(
                            lastPoint.get(),
                            result.isEmpty() ? startingPoint : point, cost.get(), // If first bundle, needs to start
                                                                                  // from given startingPoint
                            unmodifiableSet(possibilities)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            cheapestNextBundle.ifPresent(bundle -> {
                cost.addAndGet(costToAddBundle(lastPoint.get(), bundle));
                result.add((Bundle<? super GeoPoint>) bundle);
                possibilities.removeAll(bundle.getElements());
                lastPoint.set(bundle.getLast());
            });
        } while (!possibilities.isEmpty() && cheapestNextBundle.isPresent());

        /*
         * Check and update the lowest cost in synchronized blocks since this method is expected to be called in
         * parallel.
         */
        synchronized (minCost) {
            if (result.size() > shortestTour.get().size() || (!result.isEmpty() && cost.get() < minCost.get())) {
                minCost.set(cost.get());
                shortestTour.set((List<? extends Bundle<? extends GeoPoint>>) result);
            }
        }
    }

    private Optional<? extends Bundle<? extends GeoPoint>> createCheapestNextBundle(
            @Nullable final GeoPoint lastPoint,
            final GeoPoint startingPoint,
            final double currentCost,
            final Set<? extends GeoPoint> possibilities) {
        return patterns.stream()
                .map(pattern -> planBundlesWithStartingPoint(startingPoint, possibilities, pattern))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(bundle -> currentCost + costToAddBundle(lastPoint, bundle) <= costLimit)
                .min(Comparator.comparingDouble(bundle -> costToAddBundle(lastPoint, bundle)));
    }

    private Optional<Bundle<? extends GeoPoint>> planBundlesWithStartingPoint(
            final GeoPoint startingPoint,
            final Set<? extends GeoPoint> possibilities,
            final BundlePattern<GeoPoint, String> pattern) {

        final BundleValidator<GeoPoint, String> validator = pattern.createValidator();
        if (!validator.canAddToBundle(startingPoint)) {
            return Optional.empty();
        }

        final BundleBuilder<GeoPoint, String> bundleBuilder =
                new BundleBuilder<>(validator, (BiFunction<GeoPoint, GeoPoint, Double>) costFcn);
        final Set<? super GeoPoint> ineligible = new HashSet<>();

        Optional<? extends GeoPoint> nextPoint = Optional.of(startingPoint);
        while (!bundleBuilder.isComplete() && nextPoint.isPresent()) {
            final GeoPoint np = nextPoint.get();
            bundleBuilder.addToBundle(np);
            ineligible.add(np);
            nextPoint = possibilities.stream()
                    .filter(p -> !ineligible.contains(p))
                    .filter(bundleBuilder::canAddToBundle)
                    .min(Comparator.comparingDouble(value -> costFcn.apply(np, value)));
        }

        return Optional.ofNullable(bundleBuilder.isComplete() ? bundleBuilder.build() : null);
    }

    private double costToAddBundle(@Nullable final GeoPoint fromPoint, final Bundle<? extends GeoPoint> bundle) {
        return (fromPoint == null ? 0 : costFcn.apply(fromPoint, bundle.getFirst())) + bundle.getCost();
    }
}
