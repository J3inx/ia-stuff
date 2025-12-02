package app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RouteFinder {

    public static List<ApiHandler.Route> findRoutesByCityAndState(List<ApiHandler.Route> routes,
                                                                  String startCity, String startState,
                                                                  String destCity, String destState) {
        List<ApiHandler.Route> result = new ArrayList<>();

        for (ApiHandler.Route route : routes) {
            List<ApiHandler.Train> validTrains = new ArrayList<>();

            for (ApiHandler.Train train : route.trains) {
                int startIndex = -1, destIndex = -1;
                List<ApiHandler.Station> stations = train.stations;

                for (int i = 0; i < stations.size(); i++) {
                    ApiHandler.Station s = stations.get(i);
                    if (s.station != null) {
                        if (s.station.city.equalsIgnoreCase(startCity) &&
                            s.station.state.equalsIgnoreCase(startState)) {
                            startIndex = i;
                        }
                        if (s.station.city.equalsIgnoreCase(destCity) &&
                            s.station.state.equalsIgnoreCase(destState)) {
                            destIndex = i;
                        }
                    }
                }

                if (startIndex != -1 && destIndex != -1 && startIndex < destIndex) {
                    validTrains.add(train); // train passes through start → destination
                }
            }

            if (!validTrains.isEmpty()) {
                ApiHandler.Route copy = new ApiHandler.Route();
                copy.route = route.route;
                copy.trains = validTrains;
                result.add(copy);
            }
        }

        return result;
    }
}
