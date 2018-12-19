package pogo.assistance.data.model;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * Some code depends on the interning being turned on.
 */
@Gson.TypeAdapters
@Value.Immutable(intern = true)
public interface Task {

    @Nonnull Action getAction();

    @Nonnull Reward getReward();

    @Nonnull Optional<String> getAbbreviation();

}
