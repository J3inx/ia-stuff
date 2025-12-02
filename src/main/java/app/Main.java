package app;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Main {

    private List<ApiHandler.Route> allRoutes;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Main().createGUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void createGUI() throws Exception {
        allRoutes = ApiHandler.loadRoutes();

        // --------------------
        // Build state & city data
        // --------------------
        List<String> states = getAllStates(allRoutes);
        Collections.sort(states);
        String[] allStates = states.toArray(new String[0]);

        List<String> cities = getAllCities(allRoutes);
        Collections.sort(cities);
        String[] allCities = cities.toArray(new String[0]);

        // --------------------
        // GUI Setup
        // --------------------
        JFrame gui = new JFrame("Train Planner");
        gui.setSize(750, 500);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.setLayout(new BorderLayout());

        JLabel mapPanel = new JLabel("MAP", SwingConstants.CENTER);
        mapPanel.setPreferredSize(new Dimension(350, 500));
        gui.add(mapPanel, BorderLayout.WEST);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JComboBox<String> startStateBox = new JComboBox<>(allStates);
        JComboBox<String> startCityBox = new JComboBox<>(allCities);
        JComboBox<String> destStateBox = new JComboBox<>(allStates);
        JComboBox<String> destCityBox = new JComboBox<>(allCities);

        JButton findButton = new JButton("Find Route");

        JTextArea results = new JTextArea(15, 30);
        results.setEditable(false);
        JScrollPane scroll = new JScrollPane(results);

        // Add components
        controls.add(new JLabel("Start State:"));
        controls.add(startStateBox);
        controls.add(new JLabel("Start City:"));
        controls.add(startCityBox);
        controls.add(Box.createVerticalStrut(10));

        controls.add(new JLabel("Destination State:"));
        controls.add(destStateBox);
        controls.add(new JLabel("Destination City:"));
        controls.add(destCityBox);
        controls.add(Box.createVerticalStrut(20));

        controls.add(findButton);
        controls.add(Box.createVerticalStrut(20));
        controls.add(new JLabel("Results:"));
        controls.add(scroll);

        gui.add(controls, BorderLayout.EAST);

        // --------------------
        // Dynamic city filtering
        // --------------------
        startStateBox.addActionListener(e -> {
            String selectedState = (String) startStateBox.getSelectedItem();
            List<String> filtered = getCitiesByState(selectedState);
            startCityBox.setModel(new DefaultComboBoxModel<>(filtered.toArray(new String[0])));
        });

        destStateBox.addActionListener(e -> {
            String selectedState = (String) destStateBox.getSelectedItem();
            List<String> filtered = getCitiesByState(selectedState);
            destCityBox.setModel(new DefaultComboBoxModel<>(filtered.toArray(new String[0])));
        });

        // --------------------
        // Button action
        // --------------------
        findButton.addActionListener(e -> {
            String startCity = (String) startCityBox.getSelectedItem();
            String startState = (String) startStateBox.getSelectedItem();
            String destCity = (String) destCityBox.getSelectedItem();
            String destState = (String) destStateBox.getSelectedItem();

            List<ApiHandler.Route> matches =
                    RouteFinder.findRoutesByCityAndState(allRoutes, startCity, startState, destCity, destState);

            if (matches.isEmpty()) {
                results.setText("No direct trains from " + startCity + ", " + startState +
                        " to " + destCity + ", " + destState);
                return;
            }

            StringBuilder out = new StringBuilder();

            for (ApiHandler.Route route : matches) {
                out.append("Route: ").append(route.route).append("\n");

                Set<String> printedTrains = new HashSet<>();

                for (ApiHandler.Train train : route.trains) {
                    String trainKey = train.number + "-" + train.heading;
                    if (printedTrains.contains(trainKey)) continue;

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

                        ApiHandler.Station startStation = stations.get(startIndex);
                        ApiHandler.Station destStation = stations.get(destIndex);

                        String departureTime = startStation.departureActual != null
                                ? startStation.departureActual
                                : startStation.departureScheduled;

                        String arrivalTime = destStation.arrivalActual != null
                                ? destStation.arrivalActual
                                : destStation.arrivalScheduled;

                        out.append("  Train #").append(train.number)
                                .append(" (").append(train.heading).append(")\n");
                        out.append("    Departure from ").append(startCity).append("\n");
                        out.append("    Arrival at ").append(destCity).append("\n");

                        // List all stations between start and dest
                        for (int i = startIndex; i <= destIndex; i++) {
                            ApiHandler.Station s = stations.get(i);
                            out.append("      • ").append(s.station.state)
                                    .append(" — ").append(s.station.city)
                                    .append(" — ").append(s.station.name)
                                    .append("\n");
                        }

                        // --------------------------
                        // Compute estimated travel time
                        // --------------------------
                        try {
                            java.time.ZonedDateTime dep = java.time.ZonedDateTime.parse(departureTime);
                            java.time.ZonedDateTime arr = java.time.ZonedDateTime.parse(arrivalTime);
                            java.time.Duration duration = java.time.Duration.between(dep, arr);

                            long hours = duration.toHours();
                            long minutes = duration.toMinutes() % 60;
                            out.append("-----------------------\n");
                            out.append("EST: ").append(hours).append(" hours");
                            if (minutes > 0) {
                               
                                out.append(" ").append(minutes).append(" mins");
                            }
                            out.append("\n");
                        } catch (Exception ex) {
                            out.append("-----------------------\n");
                            out.append("EST: N/A (can't computer or not found)\n"); // fallback if times are missing
                        }
                    }
                }

                out.append("-----------------------\n");
            }

            results.setText(out.toString());
        });

        gui.setVisible(true);
    }

    private List<String> getAllStates(List<ApiHandler.Route> routes) {
        Set<String> set = new HashSet<>();
        for (ApiHandler.Route r : routes) {
            for (ApiHandler.Train t : r.trains) {
                for (ApiHandler.Station s : t.stations) {
                    if (s.station != null && s.station.state != null) {
                        set.add(s.station.state);
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    private List<String> getAllCities(List<ApiHandler.Route> routes) {
        Set<String> set = new HashSet<>();
        for (ApiHandler.Route r : routes) {
            for (ApiHandler.Train t : r.trains) {
                for (ApiHandler.Station s : t.stations) {
                    if (s.station != null && s.station.city != null) {
                        set.add(s.station.city);
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    private List<String> getCitiesByState(String state) {
        Set<String> set = new HashSet<>();
        for (ApiHandler.Route r : allRoutes) {
            for (ApiHandler.Train t : r.trains) {
                for (ApiHandler.Station s : t.stations) {
                    if (s.station != null && state.equalsIgnoreCase(s.station.state)) {
                        set.add(s.station.city);
                    }
                }
            }
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }
}
