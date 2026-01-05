package app;

import java.util.*;
import app.ApiHandler.StationInfo;

public class RouteFinder {
    private static final int MAX_LEGS = 8; // maximum train legs allowed
    public static Map<String, StationInfo> stationsMap = new HashMap<>();
    static Map<String, List<Leg>> departures = new HashMap<>();
    static Map<String, Set<String>> connectivityGraph = new HashMap<>();
    static Map<String, Set<String>> arrivals = new HashMap<>();

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
    private static long lastLog = System.currentTimeMillis();

    private static void log(String msg) {
        long now = System.currentTimeMillis();
        System.out.println("[" + (now - lastLog) + "ms] " + msg);
        lastLog = now;
    }
    
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

    // -------------------- Leg Graph --------------------
    static class Leg {
        ApiHandler.Train train;
        ApiHandler.Station from;
        ApiHandler.Station to;
        long minutes;
    }

    public static void buildLegGraph(List<ApiHandler.Route> routes) {
        departures.clear();
        connectivityGraph.clear();
        arrivals.clear();

        log("starting leg graph build...");

        for (ApiHandler.Route r : routes) {
            for (ApiHandler.Train t : r.trains) {
                for (int i = 0; i < t.stations.size() - 1; i++) {
                    var a = t.stations.get(i);
                    var b = t.stations.get(i + 1);
                    if (a.station == null || b.station == null)
                        continue;

                    try {
                        var dep = java.time.ZonedDateTime.parse(
                                a.departureActual != null ? a.departureActual : a.departureScheduled);
                        var arr = java.time.ZonedDateTime.parse(
                                b.arrivalActual != null ? b.arrivalActual : b.arrivalScheduled);
                                

                        long mins = java.time.Duration.between(dep, arr).toMinutes();

                        Leg leg = new Leg();
                        leg.train = t;
                        leg.from = a;
                        leg.to = b;
                        leg.minutes = mins;

                        departures
                                .computeIfAbsent(a.station.code, k -> new ArrayList<>())
                                .add(leg);

                        connectivityGraph
                                .computeIfAbsent(a.station.code, k -> new HashSet<>())
                                .add(b.station.code);
                        arrivals
                            .computeIfAbsent(b.station.code, k -> new HashSet<>())
                            .add(a.station.code);
                            

                    } catch (Exception ignored) {
                    }
                }
            }
        }

        System.out.println("Leg graph built. Total departure points: " + departures.size());
    }

    // -------------------- Reachability (Reverse Graph BFS) --------------------
    static Set<String> computeReachableToTarget(String target) {
        Set<String> reachable = new HashSet<>();
        Deque<String> q = new ArrayDeque<>();
    
        reachable.add(target);
        q.add(target);
    
        while (!q.isEmpty()) {
            String cur = q.poll();
    
            for (String prev : arrivals.getOrDefault(cur, Set.of())) {
                if (reachable.add(prev)) {
                    q.add(prev);
                }
            }
        }
    
        return reachable;
    }
    

    // -------------------- Shortest Route (Pruned DFS) --------------------
    static class PathResult {
        long totalMinutes = Long.MAX_VALUE;
        List<Leg> bestPath = new ArrayList<>();
    }

    public static PathResult findShortestRoute(String fromCode, String toCode) {
        PathResult result = new PathResult();
    
        // -----------------------------
        // Step 1: Direct trains
        // -----------------------------
        List<Leg> directLegs = new ArrayList<>();
        for (Leg leg : departures.getOrDefault(fromCode, List.of())) {
            if (leg.to.station.code.equals(toCode)) {
                directLegs.add(leg);
            }
        }
    
        if (!directLegs.isEmpty()) {
            Leg fastest = directLegs.get(0);
            for (Leg leg : directLegs) {
                if (leg.minutes < fastest.minutes) fastest = leg;
            }
            result.bestPath.add(fastest);
            result.totalMinutes = fastest.minutes;
            return result;
        }
    
        // -----------------------------
        // Step 2: BFS for multi-leg paths (with train tracking)
        // -----------------------------
        class QueueNode {
            String stationCode;
            List<Leg> path;
            long totalMinutes;
            int legCount;
            int maxLegs;
            ApiHandler.Train currentTrain;
    
            QueueNode(String code, List<Leg> path, long mins, ApiHandler.Train train) {
                this.stationCode = code;
                this.path = path;
                this.totalMinutes = mins;
                this.legCount = path.size();
                this.maxLegs = MAX_LEGS;
                this.currentTrain = train;
            }
        }
    
        Queue<QueueNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
    
        queue.add(new QueueNode(fromCode, new ArrayList<>(), 0, null));
    
        while (!queue.isEmpty()) {
            QueueNode node = queue.poll();
    
            if (node.stationCode.equals(toCode)) {
                result.bestPath = node.path;
                result.totalMinutes = node.totalMinutes;
                return result;
            }
    
            if (node.legCount >= MAX_LEGS) continue;
    
            for (Leg leg : departures.getOrDefault(node.stationCode, List.of())) {
                String key = leg.to.station.code + "_" + leg.train.number;
    
                // Avoid cycles on same station + train
                if (visited.contains(key)) continue;
                visited.add(key);
    
                List<Leg> newPath = new ArrayList<>(node.path);
                newPath.add(leg);
    
                // Keep the train if it's the same, otherwise update
                queue.add(new QueueNode(leg.to.station.code, newPath, node.totalMinutes + leg.minutes, leg.train));

            }
        }
    
        return result;
    }
    
    

    // -------------------- DFS with Reachability Pruning --------------------
    private static void recursiveSearch(
            String current,
            String target,
            Set<String> reachable,
            Set<String> visited,
            List<Leg> path,
            int depth,
            int maxDepth,
            
            PathResult result) {
                

      
        List<Leg> options = new ArrayList<>(departures.getOrDefault(current, List.of()));
        options.sort(Comparator.comparingLong(l -> l.minutes));
        long currentTime = path.stream().mapToLong(l -> l.minutes).sum();
        if (currentTime >= result.totalMinutes) return;

        if (depth > maxDepth)
            return;

        if (!reachable.contains(current))
            return;

        if (result.totalMinutes != Long.MAX_VALUE)
            return;

        visited.add(current);

        for (Leg leg : departures.getOrDefault(current, List.of())) {
            
            String next = leg.to.station.code;

            // 🚫 prune dead ends
            if (!reachable.contains(next))
                continue;

            if (visited.contains(next))
                continue;

            path.add(leg);

            if (next.equals(target)) {
                result.bestPath = new ArrayList<>(path);
                result.totalMinutes = path.stream().mapToLong(l -> l.minutes).sum();
                path.remove(path.size() - 1);
                visited.remove(current);
                log("Path found: legs=" + result.bestPath.size() +
    ", time=" + result.totalMinutes + " mins");

                return;
            }

            recursiveSearch(next, target, reachable, visited, path, depth + 1, maxDepth, result);

            path.remove(path.size() - 1);
           
            if (depth == 0) {
                log("Exploring first-leg option to " + next);
            }
        }

        visited.remove(current);
    }
}
