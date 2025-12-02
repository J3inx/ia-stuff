package app;

import java.util.List;

public class Startup {

    public static void main(String[] args) throws Exception {

        System.out.println("Loading Amtrak data...");
        List<ApiHandler.Route> routes = ApiHandler.loadRoutes();

        System.out.println("Finding routes from Washington to Baltimore...");
        List<ApiHandler.Route> found =
                RouteFinder.findRoutesByCity(routes, "Washington", "Baltimore");

        System.out.println("Found " + found.size() + " possible routes.");
    }
}
