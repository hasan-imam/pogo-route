package pogo.assistance.data.model;

import org.immutables.value.Value;

@Value.Immutable
public interface GeoPoint {

    double getLatitude();
    double getLongitude();

}
