package app;

import java.util.*;
import app.ApiHandler.StationInfo;

public class RouteFinder {

    public static Map<String, StationInfo> stationsMap = new HashMap<>();

    // ----------------------------------------------------
    // Build full station-code map from ALL routes/trains
    // ----------------------------------------------------
    public static void populateStationsMap(List<ApiHandler.Route> routes) {
        stationsMap.clear();

        for (ApiHandler.Route r : routes) {
            for (ApiHandler.Train t : r.trains) {
                for (ApiHandler.Station s : t.stations) {
                    if (s.station != null && s.station.code != null) {
                        stationsMap.put(s.station.code, s.station);
                    }
                }
            }
        }

        System.out.println("Loaded station codes: " + stationsMap.size());
    }

    // ----------------------------------------------------
    // Distance formula
    // ----------------------------------------------------
    private static final double EARTH_RADIUS = 3958.8;

    public static double haversineDistance(double lat1, double lon1,
                                           double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    // ----------------------------------------------------
    // Price Formula
    // ----------------------------------------------------
    public static double calculateLowestPrice(String fromCode,
                                              String toCode,
                                              String classType) {

        StationInfo from = stationsMap.get(fromCode);
        StationInfo to = stationsMap.get(toCode);

        if (from == null || to == null) {
            throw new IllegalArgumentException("Station code not found.");
        }

        double distance = haversineDistance(from.lat, from.lon, to.lat, to.lon);

        double baseRate = 0.28;
        double classMultiplier = switch (classType.toLowerCase()) {
            case "business" -> 1.5;
            case "first" -> 1.7;
            case "private" -> 2.0;
            default -> 1.0;
        };

        return distance * baseRate * classMultiplier;
    }

    // ----------------------------------------------------
    // Route matching engine
    // ----------------------------------------------------
    public static List<ApiHandler.Route> findRoutesByCityAndState(
            List<ApiHandler.Route> routes,
            String startCity, String startState,
            String destCity, String destState
    ) {
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
                            s.station.state.equalsIgnoreCase(startState))
                            startIndex = i;

                        if (s.station.city.equalsIgnoreCase(destCity) &&
                            s.station.state.equalsIgnoreCase(destState))
                            destIndex = i;
                    }
                }

                if (startIndex != -1 && destIndex != -1 && startIndex < destIndex) {
                    validTrains.add(train);
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
