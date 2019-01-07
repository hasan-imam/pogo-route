package pogo.assistance.route.planning.conditional.bundle;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import pogo.assistance.data.model.GeoPoint;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.model.Reward;
import pogo.assistance.data.model.Reward.RewardObject;
import pogo.assistance.data.model.Task;
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

    @Value.Lazy
    default Map<RewardObject, Double> getQuantifiedRewards() {
        return getElements().stream()
                .filter(geoPoint -> geoPoint instanceof Task)
                .map(geoPoint -> (Task) geoPoint)
                .map(Task::getReward)
                .filter(reward -> !RewardObject.UNKNOWN.equals(reward.getRewardObject()))
                .filter(reward -> reward.getQuantity().isPresent())
                .collect(Collectors.toMap(
                        Reward::getRewardObject,
                        reward -> reward.getQuantity().get(),
                        (q1, q2) -> q1 + q2));
    }

    @Value.Lazy
    default List<? extends Quest> getQuests() {
        return getElements().stream()
                .filter(geoPoint -> geoPoint instanceof Quest)
                .map(geoPoint -> (Quest) geoPoint)
                .collect(Collectors.toList());
    }

}
