package pogo.assistance.route.planning.conditional.bundle;

import com.google.common.base.Preconditions;
import java.util.List;
import org.immutables.value.Value;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.route.CooldownCalculator;

@Value.Immutable(copy = false)
public interface Bundle<U extends GeoPoint> {

    List<U> getElements();

    default U getFirst() {
        return getElements().get(0);
    }

    default U getLast() {
        final List<U> elements = getElements();
        return elements.get(elements.size() - 1);
    }

    default double getDistance() {
        return CooldownCalculator.calculateCost(getElements(), CooldownCalculator::getDistance);
    }

    default double getDuration() {
        return CooldownCalculator.calculateCost(getElements(), CooldownCalculator::getCooldown);
    }

    @Value.Check
    default void check() {
        Preconditions.checkState(!getElements().isEmpty());
    }

}
