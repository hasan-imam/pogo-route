package pogo.assistance.route.planning.conditional.bundle;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.immutables.value.Value;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.route.CooldownCalculator;

@Value.Immutable
public interface PlannerConfig {

    @Value.Default
    default BiFunction<? super GeoPoint, ? super GeoPoint, Double> costFunction() {
        return CooldownCalculator::getDistance;
    }

    /**
     * Planner will use this comparator to select the best tour.
     */
    @Value.Default
    default Comparator<Tour> tourComparator() {
        return Comparator.<Tour, Integer>comparing(tour -> tour.getBundles().size())
                .thenComparing(Comparator.comparing(Tour::getTotalDuration).reversed());
    }

    Optional<Double> maxTourDistance();

    Optional<Duration> maxTourDuration();

    Optional<Double> maxBundleDistance();

    Optional<Duration> maxBundleDuration();

    Optional<Double> maxStepDistance();

    Optional<Duration> maxStepDuration();

    @Value.Default
    default Optional<Double> maxBundleToBundleDistance() {
        return maxStepDistance();
    }

    @Value.Default
    default Optional<Duration> maxBundleToBundleDuration() {
        return maxStepDuration();
    }

    /**
     * @return
     *      List of strings, each describing one configuration element. Some configuration elements are not describable
     *      (e.g. {@link #tourComparator()}) and are not captured in this output.
     */
    @Value.Derived
    default List<String> describe() {
        final DecimalFormat numberFormat = new DecimalFormat("#.##");
        final List<String> description = new ArrayList<>();

        if (maxStepDistance().isPresent() && maxStepDuration().isPresent()) {
            description.add(String.format(
                    "Any two points are within %s KM distance and %s cool down",
                    numberFormat.format(maxStepDistance().get()),
                    maxStepDuration().get()));
        } else {
            maxStepDistance().map(numberFormat::format).ifPresent(distance ->
                    description.add(String.format("Any two points are within %s KM distance", distance)));
            maxStepDuration().ifPresent(duration ->
                    description.add(String.format("Any two points are within %s cool down", duration)));
        }

        if (maxBundleToBundleDistance().isPresent() && maxBundleToBundleDuration().isPresent()) {
            description.add(String.format(
                    "Any two sets are within %s KM distance and %s cool down",
                    numberFormat.format(maxBundleToBundleDistance().get()),
                    maxBundleToBundleDuration().get()));
        } else {
            maxBundleToBundleDistance().map(numberFormat::format).ifPresent(distance ->
                    description.add(String.format("Any two sets are within %s KM distance", distance)));
            maxBundleToBundleDuration().ifPresent(duration ->
                    description.add(String.format("Any two sets are within %s cool down", duration)));
        }

        if (maxBundleDistance().isPresent() && maxBundleDuration().isPresent()) {
            description.add(String.format(
                    "Each set is within %s KM distance and %s cool down",
                    numberFormat.format(maxBundleDistance().get()),
                    maxBundleDuration().get()));
        } else {
            maxBundleDistance().map(numberFormat::format).ifPresent(distance ->
                    description.add(String.format("Each set is within %s KM distance", distance)));
            maxBundleDuration().ifPresent(duration ->
                    description.add(String.format("Each set is within %s cool down", duration)));
        }

        if (maxTourDistance().isPresent() && maxTourDuration().isPresent()) {
            description.add(String.format(
                    "Route is less than %s KM long and has less than %s cool down",
                    numberFormat.format(maxTourDistance().get()),
                    maxTourDuration().get()));
        } else {
            maxTourDuration().ifPresent(duration ->
                    description.add(String.format("Route has less than %s cool down", duration)));
            maxTourDistance().map(numberFormat::format).ifPresent(distance ->
                    description.add(String.format("Route is less than %s KM long", distance)));
        }

        return description;
    }

}
