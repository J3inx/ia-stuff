package app;

import java.util.ArrayList;
import java.util.List;

public class RouteFinder {

    public static List<ApiHandler.Route> findRoutesByCity(
            List<ApiHandler.Route> routes,
            String fromCity,
            String toCity
    ) {
        List<ApiHandler.Route> results = new ArrayList<>();

        for (ApiHandler.Route r : routes) {
            for (ApiHandler.Train t : r.trains) {
                boolean hasFrom = false;
                boolean hasTo = false;

                for (ApiHandler.Station s : t.stations) {
                    if (s.station.city.equalsIgnoreCase(fromCity)) hasFrom = true;
                    if (s.station.city.equalsIgnoreCase(toCity)) hasTo = true;
                }

                if (hasFrom && hasTo) {
                    results.add(r);
                    break;
                }
            }
        }

        return results;
    }
}
