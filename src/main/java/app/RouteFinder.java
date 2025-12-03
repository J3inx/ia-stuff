package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.ApiHandler.Station;

public class RouteFinder {

    public static Map<String, ApiHandler.StationInfo> stationsMap = new HashMap<>();


    // Populates stationsMap
    public static void populateStationsMap(List<ApiHandler.Station> stations) {
        stationsMap.clear(); 
        for (ApiHandler.Station s : stations) {
    if (s.station != null && s.station.code != null) {
        stationsMap.put(s.station.code, s.station); // now types match
    }
}

    }
       private static final double EARTH_RADIUS = 3958.8; // miles

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
   public static double calculateLowestPrice(String fromCode, String toCode, String classType) {
    ApiHandler.StationInfo from = stationsMap.get(fromCode);
    ApiHandler.StationInfo to = stationsMap.get(toCode);

    if (from == null || to == null) {
        throw new IllegalArgumentException("Station code not found.");
    }

    // Use the Haversine formula from RouteFinder itself
    double distance = haversineDistance(from.lat, from.lon, to.lat, to.lon);
    double baseRate = 0.28;
    double classMultiplier = switch (classType.toLowerCase()) {
        case "business" -> 1.5;
        case "first" -> 1.7;
        case "private" -> 2.0;
        default -> 1.0;
    };

    return baseRate * distance * classMultiplier;
}

// Add this method if it isn't already in RouteFinder
public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
    final int EARTH_RADIUS_MILES = 3959;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
               Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_MILES * c;
}


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
