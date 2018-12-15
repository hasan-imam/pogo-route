package pogo.assistance.data.model;

import java.util.Optional;
import org.immutables.value.Value;

/**
 * TODO: this description bit may need refactor:
 *  - make this work with different languages; currently only have English ones
 *  - put these in a config file so users can change/add translations with touching code
 */
@Value.Immutable(intern = true)
public interface Action {

    String getDescription();

    Optional<String> getAbbreviatedName();

}
