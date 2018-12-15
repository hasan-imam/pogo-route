package pogo.assistance.data.model;

import org.immutables.value.Value;

@Value.Immutable(intern = true)
public interface Reward {

    String getDescription();

}
