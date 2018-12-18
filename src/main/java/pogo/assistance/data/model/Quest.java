package pogo.assistance.data.model;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public interface Quest extends Task, GeoPoint {

}
