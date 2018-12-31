package pogo.assistance.route.planning.conditional.bundle;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import org.immutables.value.Value;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.route.CooldownCalculator;

@Value.Immutable
public interface PlannerConfig {

    default BiFunction<? super GeoPoint, ? super GeoPoint, Double> costFunction() {
        return CooldownCalculator::getDistance;
    }

    default Comparator<Tour> tourComparator() {
        return Comparator.<Tour, Integer>comparing(tour -> tour.getBundles().size())
                .thenComparing(Comparator.comparing(Tour::getTotalDuration).reversed());
    }

    Optional<Double> maxTourDistance();

    Optional<Duration> maxTourDuration();

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

}
