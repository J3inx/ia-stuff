package app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.List;

public class ApiHandler {

    private static final Gson gson = new Gson();

    /** ---------------------- */
    /**  JSON data structures  */
    /** ---------------------- */

    public static class Route {
        public String route;
        public List<Train> trains;
    }

    public static class Train {
        public long id;
        public int number;
        public String heading;
        public String route;
        public List<Station> stations;
    }

    public static class Station {
        public String code;
        public boolean bus;
        public String arrivalActual;
        public String arrivalScheduled;
        public String departureActual;
        public String departureScheduled;
        public String status;
        public StationInfo station;
    }

    public static class StationInfo {
        public String code;
        public String name;
        public String city;
        public String state;
        public String address1;
        public String address2;
        public String zip;
        public double lat;
        public double lon;
    }

    /** ---------------------- */
    /**    JSON file loaders   */
    /** ---------------------- */

    public static List<Route> loadRoutes() throws Exception {
        Type listType = new TypeToken<List<Route>>(){}.getType();
        return gson.fromJson(
                new FileReader("src/amtrak-api/_site/routes.json"),
                listType
        );
    }

    public static List<StationInfo> loadAllStations() throws Exception {
        Type listType = new TypeToken<List<StationInfo>>(){}.getType();
        return gson.fromJson(
                new FileReader("src/amtrak-api/_site/stations.json"),
                listType
        );
    }
}
