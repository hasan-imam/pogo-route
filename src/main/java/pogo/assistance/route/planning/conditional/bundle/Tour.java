package pogo.assistance.route.planning.conditional.bundle;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.route.CooldownCalculator;

@Value.Immutable
public interface Tour {

    List<? extends Bundle<? extends GeoPoint>> getBundles();

    @Value.Lazy
    default List<? extends GeoPoint> getElements() {
        return getBundles().stream().map(Bundle::getElements).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Value.Lazy
    default double getTotalDistance() {
        return CooldownCalculator.calculateCost(getElements(), CooldownCalculator::getDistance);
    }

    @Value.Lazy
    default Duration getTotalDuration() {
        return Duration.ofSeconds((long) Math.ceil(CooldownCalculator.calculateCost(getElements(), CooldownCalculator::getCooldown)));
    }

}
