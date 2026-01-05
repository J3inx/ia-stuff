package app;

import java.util.*;
import app.ApiHandler.StationInfo;

public class RouteFinder {

    private static final int MAX_LEGS = 8; // maximum train legs allowed
    public static Map<String, StationInfo> stationsMap = new HashMap<>();
    static Map<String, List<Leg>> departures = new HashMap<>();

    // -------------------- Station Map --------------------
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

    // -------------------- Distance / Price --------------------
    private static final double EARTH_RADIUS = 3958.8;

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public static double calculateLowestPrice(String fromCode, String toCode, String classType) {
        StationInfo from = stationsMap.get(fromCode);
        StationInfo to = stationsMap.get(toCode);
        if (from == null || to == null)
            throw new IllegalArgumentException("Station code not found.");

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

    // -------------------- Leg --------------------
    static class Leg {
        ApiHandler.Train train;
        ApiHandler.Station from;
        ApiHandler.Station to;
        long minutes;
    }

    // -------------------- Build Full Leg Graph --------------------
    public static void buildLegGraph(List<ApiHandler.Route> routes) {
        departures.clear();

        // For every train, create legs from every station to all stations after it
        for (ApiHandler.Route r : routes) {
            for (ApiHandler.Train t : r.trains) {
                List<ApiHandler.Station> stops = t.stations;
                for (int i = 0; i < stops.size() - 1; i++) {
                    for (int j = i + 1; j < stops.size(); j++) {
                        ApiHandler.Station from = stops.get(i);
                        ApiHandler.Station to = stops.get(j);
                        if (from.station == null || to.station == null) continue;

                        long mins = 0;
                        try {
                            var dep = java.time.ZonedDateTime.parse(
                                    from.departureActual != null ? from.departureActual : from.departureScheduled);
                            var arr = java.time.ZonedDateTime.parse(
                                    to.arrivalActual != null ? to.arrivalActual : to.arrivalScheduled);
                            mins = java.time.Duration.between(dep, arr).toMinutes();
                        } catch (Exception ignored) {}

                        Leg leg = new Leg();
                        leg.train = t;
                        leg.from = from;
                        leg.to = to;
                        leg.minutes = mins;

                        departures.computeIfAbsent(from.station.code, k -> new ArrayList<>()).add(leg);
                    }
                }
            }
        }

        System.out.println("Leg graph built. Total departure points: " + departures.size());
    }

    // -------------------- Shortest Route --------------------
    public static PathResult findShortestRoute(List<ApiHandler.Route> allRoutes, String fromCode, String toCode) {
        PathResult result = new PathResult();

        // ---------- Step 1: Old logic - direct train ----------
        for (ApiHandler.Route r : allRoutes) {
            for (ApiHandler.Train t : r.trains) {
                int startIndex = -1, destIndex = -1;
                for (int i = 0; i < t.stations.size(); i++) {
                    ApiHandler.Station s = t.stations.get(i);
                    if (s.station == null) continue;
                    if (s.station.code.equals(fromCode)) startIndex = i;
                    if (s.station.code.equals(toCode)) destIndex = i;
                }

                if (startIndex != -1 && destIndex != -1 && startIndex < destIndex) {
                    // direct train found
                    for (int i = startIndex; i < destIndex; i++) {
                        Leg leg = new Leg();
                        leg.train = t;
                        leg.from = t.stations.get(i);
                        leg.to = t.stations.get(i + 1);
                        try {
                            var dep = java.time.ZonedDateTime.parse(
                                    leg.from.departureActual != null ? leg.from.departureActual : leg.from.departureScheduled);
                            var arr = java.time.ZonedDateTime.parse(
                                    leg.to.arrivalActual != null ? leg.to.arrivalActual : leg.to.arrivalScheduled);
                            leg.minutes = java.time.Duration.between(dep, arr).toMinutes();
                        } catch (Exception ignored) {
                            leg.minutes = 0;
                        }
                        result.bestPath.add(leg);
                        result.totalMinutes += leg.minutes;
                    }
                    return result;
                }
            }
        }

        // ---------- Step 2: BFS / multi-leg search ----------
        class QueueNode {
            String stationCode;
            List<Leg> path;
            long totalMinutes;
            ApiHandler.Train currentTrain;

            QueueNode(String code, List<Leg> path, long mins, ApiHandler.Train train) {
                this.stationCode = code;
                this.path = path;
                this.totalMinutes = mins;
                this.currentTrain = train;
            }
        }

        Queue<QueueNode> queue = new LinkedList<>();
        // use station+train as visited key to allow transfers
        Set<String> visited = new HashSet<>();
        queue.add(new QueueNode(fromCode, new ArrayList<>(), 0, null));

        while (!queue.isEmpty()) {
            QueueNode node = queue.poll();

            if (node.stationCode.equals(toCode)) {
                result.bestPath = node.path;
                result.totalMinutes = node.totalMinutes;
                return result;
            }

            if (node.path.size() >= MAX_LEGS) continue;

            for (Leg leg : departures.getOrDefault(node.stationCode, List.of())) {
                String key = leg.to.station.code + "_" + leg.train.number + "_" + node.path.size();
                if (visited.contains(key)) continue;
                visited.add(key);

                List<Leg> newPath = new ArrayList<>(node.path);
                newPath.add(leg);

                ApiHandler.Train nextTrain = (node.currentTrain != null && node.currentTrain.number == leg.train.number)
                        ? node.currentTrain
                        : leg.train;

                queue.add(new QueueNode(leg.to.station.code, newPath, node.totalMinutes + leg.minutes, nextTrain));
            }
        }

        return result;
    }

    // -------------------- PathResult --------------------
    public static class PathResult {
        public long totalMinutes = 0; // fixed from Long.MAX_VALUE
        public List<Leg> bestPath = new ArrayList<>();
    }
}
