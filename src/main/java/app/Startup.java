package app;

import java.util.List;

public class Startup {

    public static void main(String[] args) throws Exception {
        String startCity = "Washington";
        String startState = "DC";
        String destCity = "Roanoke";
        String destState = "VA";

        System.out.println("Loading Amtrak data...");
        List<ApiHandler.Route> routes = ApiHandler.loadRoutes();

        // Build stations map
        RouteFinder.populateStationsMap(routes);

        // Build leg graph
        System.out.println("Building leg graph...");
        RouteFinder.buildLegGraph(routes);

        // Resolve station codes
        String fromCode = RouteFinder.stationsMap.entrySet().stream()
                .filter(e -> e.getValue().city.equalsIgnoreCase(startCity) &&
                             e.getValue().state.equalsIgnoreCase(startState))
                .map(e -> e.getKey())
                .findFirst()
                .orElse(null);

        String toCode = RouteFinder.stationsMap.entrySet().stream()
                .filter(e -> e.getValue().city.equalsIgnoreCase(destCity) &&
                             e.getValue().state.equalsIgnoreCase(destState))
                .map(e -> e.getKey())
                .findFirst()
                .orElse(null);

        if (fromCode == null || toCode == null) {
            System.out.println("Invalid station codes, aborting.");
            return;
        }

        System.out.println("Finding shortest route...");
        RouteFinder.PathResult result = RouteFinder.findShortestRoute(fromCode, toCode);

        if (result.bestPath.isEmpty()) {
            System.out.println("No route found.");
            return;
        }

        long h = result.totalMinutes / 60;
        long m = result.totalMinutes % 60;

        System.out.println("Optimal Route:");
        ApiHandler.Train lastTrain = null;
        for (RouteFinder.Leg leg : result.bestPath) {
            if (lastTrain != null && leg.train != lastTrain) {
                System.out.println("🔁 Switch trains at " +
                        leg.from.station.city + ", " +
                        leg.from.station.state +
                        " → Train #" + leg.train.number);
            }

            System.out.println("\t🚆 Train #" + leg.train.number +
                    " → " + leg.to.station.city + ", " +
                    leg.to.station.state);
            lastTrain = leg.train;
        }

        System.out.println("EST: " + h + " hours" + (m > 0 ? " " + m + " mins" : ""));
        double price = RouteFinder.calculateLowestPrice(fromCode, toCode, "economy");
        System.out.println("Lowest possible price: $" + String.format("%.2f", price));
    }
}
