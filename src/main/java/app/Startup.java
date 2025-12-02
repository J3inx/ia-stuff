package app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Startup {

    public static void main(String[] args) throws Exception {
        String startCity = "Washington";
        String startState = "DC"; // optional, add state if you want more accuracy
        String destCity = "Roanoke";
        String destState = "VA"; // optional

        System.out.println("Loading Amtrak data...");
        List<ApiHandler.Route> routes = ApiHandler.loadRoutes();

        System.out.println("Finding routes from " + startCity + ", " + startState +
                           " to " + destCity + ", " + destState + "...");
        List<ApiHandler.Route> found = RouteFinder.findRoutesByCityAndState(
                routes, startCity, startState, destCity, destState);

        System.out.println("Found " + found.size() + " possible routes.\n");

        for (ApiHandler.Route route : found) {
            System.out.println("Route: " + route.route);

            Set<String> printedTrains = new HashSet<>();

            for (ApiHandler.Train train : route.trains) {
                String trainKey = train.number + "-" + train.heading;
                if (printedTrains.contains(trainKey)) continue; // skip duplicates

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
                    printedTrains.add(trainKey);

                    System.out.println("  Train #" + train.number + " (" + train.heading + ")");
                    for (int i = startIndex; i <= destIndex; i++) {
                        ApiHandler.Station s = stations.get(i);
                        System.out.println("      • " + s.station.state + " — " +
                                           s.station.city + " — " + s.station.name);
                    }
                }
            }

            System.out.println("-----------------------");
            
        }
    }
}
