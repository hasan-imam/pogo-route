package pogo.assistance.route.planning.conditional.bundle;

import static java.util.Collections.unmodifiableSet;
import static pogo.assistance.route.CooldownCalculator.getCooldown;
import static pogo.assistance.route.CooldownCalculator.getDistance;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.route.CooldownCalculator;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TourPlanner {

    private final PlannerConfig config;

    public synchronized Optional<Tour> plan(
            final List<? extends GeoPoint> points,
            final List<BundlePattern<GeoPoint, String>> patterns) {
        return points.stream()
                .parallel()
                .map(startingPoint -> planTour(startingPoint, points, patterns))
//                // Uncomment to print out some stats about the generated routes
//                // Lets you verify that the best/expected route was selected
//                .peek(tour -> System.out.println(String.format(
//                        "[route] Duration: %s, Elements: %s, Reward: %s",
//                        tour.getTotalDuration(),
//                        tour.getElements().size(),
//                        tour.getQuantifiedRewards().getOrDefault(RewardObject.STARDUST, -1D))))
                .max(config.tourComparator());
    }

    private Tour planTour(
            final GeoPoint startingPoint,
            final List<? extends GeoPoint> points,
            final List<BundlePattern<GeoPoint, String>> patterns) {
        final Set<? extends GeoPoint> possibilities = new HashSet<>(points);
        final TourBuilder tourBuilder = new TourBuilder();
        final AtomicReference<GeoPoint> lastPoint = new AtomicReference<>();
        Optional<? extends Bundle<? extends GeoPoint>> cheapestNextBundle;
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
                    .filter(distanceLimitFilter(config.maxBundleToBundleDistance().orElse(null), lastPoint.get()))
                    .filter(durationLimitFilter(config.maxBundleToBundleDuration().orElse(null), lastPoint.get()))
                    .sorted(comparingCostFrom(lastPoint.get()))
                    .map(point -> createBundle(
                            lastPoint.get(),
                            tourBuilder.getBundles().isEmpty() ? startingPoint : point,
                            tourBuilder.build(),
                            unmodifiableSet(possibilities),
                            patterns))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            cheapestNextBundle.ifPresent(bundle -> {
                tourBuilder.append(bundle);
                possibilities.removeAll(bundle.getElements());
                lastPoint.set(bundle.getLast());
            });
        } while (!possibilities.isEmpty() && cheapestNextBundle.isPresent());

        return tourBuilder.build();
    }

    private Optional<? extends Bundle<? extends GeoPoint>> createBundle(
            @Nullable final GeoPoint lastPoint,
            final GeoPoint startingPoint,
            final Tour currentTour,
            final Set<? extends GeoPoint> possibilities,
            final List<BundlePattern<GeoPoint, String>> patterns) {
        return patterns.stream()
                .map(pattern -> createBundle(startingPoint, possibilities, pattern))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(bundle -> isWithinMaxDistance(currentTour.getTotalDistance(), lastPoint, bundle))
                .filter(bundle -> isWithinMaxDuration(currentTour.getTotalDuration(), lastPoint, bundle))
                .min(Comparator.comparingDouble(bundle -> costToAddBundle(lastPoint, bundle)));
    }

    /**
     * @return
     *      Greedily created bundle starting at {@code startingPoint} and matching the {@code pattern}, built using the
     *      points supplied in {@code possibilities}. It doesn't matter if {@code possibilities} contains the
     *      {@code startingPoint} or not. Returns empty if no bundle could be created with this input.
     */
    private Optional<? extends Bundle<? extends GeoPoint>> createBundle(
            final GeoPoint startingPoint,
            final Set<? extends GeoPoint> possibilities,
            final BundlePattern<GeoPoint, String> pattern) {

        final BundleValidator<GeoPoint, String> validator = pattern.createValidator();
        if (!validator.canAddToBundle(startingPoint) || possibilities.isEmpty()) {
            return Optional.empty();
        }

        final BundleBuilder<? super GeoPoint, String> bundleBuilder = new BundleBuilder<>(validator);
        final Set<? super GeoPoint> ineligible = new HashSet<>();

        Optional<? extends GeoPoint> nextPoint = Optional.of(startingPoint);
        while (!bundleBuilder.isComplete() && nextPoint.isPresent()) {
            final GeoPoint np = nextPoint.get();
            bundleBuilder.addToBundle(np);
            ineligible.add(np);
            nextPoint = possibilities.stream()
                    .filter(p -> !ineligible.contains(p))
                    .filter(bundleBuilder::canAddToBundle)
                    .filter(distanceLimitFilter(config.maxStepDistance().orElse(null), np))
                    .filter(durationLimitFilter(config.maxStepDuration().orElse(null), np))
                    .min(Comparator.comparing(value -> config.costFunction().apply(np, value)));
        }

        return Optional.ofNullable(bundleBuilder.isComplete() ? bundleBuilder.build() : null)
                .filter(bundle -> Double.compare(
                        bundle.getDistance(),
                        config.maxBundleDistance().orElse(Double.MAX_VALUE)) <= 0)
                .filter(bundle -> bundle.getDuration() <= config.maxBundleDuration().map(Duration::getSeconds).orElse(Long.MAX_VALUE));
    }

    private Comparator<? super GeoPoint> comparingCostFrom(@Nullable final GeoPoint lastPoint) {
        return Comparator.comparingDouble(point -> (lastPoint == null) ? 0 : config.costFunction().apply(lastPoint, point));
    }

    private boolean isWithinMaxDistance(
            final double currentDistance,
            @Nullable final GeoPoint fromPoint,
            final Bundle<? extends GeoPoint> bundle) {
        if (!config.maxTourDistance().isPresent()) {
            return true;
        }
        final double distanceAfterAddingBundle = currentDistance
                + (fromPoint == null ? 0 : getDistance(fromPoint, bundle.getFirst()))
                + bundle.getDistance();
        return Double.compare(distanceAfterAddingBundle, config.maxTourDistance().get()) <= 0;
    }

    private boolean isWithinMaxDuration(
            final Duration currentDuration,
            @Nullable final GeoPoint fromPoint,
            final Bundle<? extends GeoPoint> bundle) {
        if (!config.maxTourDuration().isPresent()) {
            return true;
        }
        final Duration durationAfterAddingBundle = currentDuration
                .plusSeconds(fromPoint == null ? 0 : (long) getCooldown(fromPoint, bundle.getFirst()))
                .plusSeconds((long) bundle.getDuration());
        return durationAfterAddingBundle.compareTo(config.maxTourDuration().get()) <= 0;
    }

    private static Predicate<? super GeoPoint> distanceLimitFilter(
            @Nullable final Double limit,
            @Nullable final GeoPoint lastPoint) {
        return (limit == null || lastPoint == null) ?
                (geoPoint -> true) : (geoPoint -> Double.compare(getDistance(lastPoint, geoPoint), limit) <= 0);
    }

    private static Predicate<? super GeoPoint> durationLimitFilter(
            @Nullable Duration limit,
            @Nullable final GeoPoint lastPoint) {
        return (limit == null || lastPoint == null) ?
                (geoPoint -> true) : (geoPoint -> Double.compare(getCooldown(lastPoint, geoPoint), limit.getSeconds()) <= 0);
    }

    private double costToAddBundle(@Nullable final GeoPoint fromPoint, final Bundle<? extends GeoPoint> bundle) {
        return (fromPoint == null ? 0 : config.costFunction().apply(fromPoint, bundle.getFirst()))
                + CooldownCalculator.calculateCost(bundle.getElements(), config.costFunction());
    }
}
