package pogo.assistance.data.model;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.immutables.value.Value;

/**
 * Some code depends on the interning being turned on.
 */
@Value.Immutable(intern = true)
public interface Task {

    @Nonnull Action getAction();

    @Nonnull Reward getReward();

    @Nonnull Optional<String> getAbbreviation();

}
