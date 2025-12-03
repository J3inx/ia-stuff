package app;

import javax.swing.*;
import javax.swing.text.*;
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

        // --------------------
        // jpanel setup and formatting
        // --------------------
        JLabel mapPanel = new JLabel("MAP", SwingConstants.CENTER);
        mapPanel.setPreferredSize(new Dimension(350, 500));
        gui.add(mapPanel, BorderLayout.WEST);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));


        JComboBox<String> startStateBox = new JComboBox<>(allStates);
        JComboBox<String> startCityBox = new JComboBox<>(allCities);
        JComboBox<String> destStateBox = new JComboBox<>(allStates);
        JComboBox<String> destCityBox = new JComboBox<>(allCities);
        JComboBox<String> classBox = new JComboBox<>(new String[]{"economy", "business", "first", "private"});
        


        JButton findButton = new JButton("Find Route");

        // --------------------
        // JTextPane formatting
        // --------------------
        JTextPane results = new JTextPane();
        results.setEditable(false);

        // output area padding
        results.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scroll = new JScrollPane(results);
        scroll.setPreferredSize(new Dimension(350, 200));

        StyledDocument doc = results.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);

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
        controls.add(new JLabel("Class:"));
        controls.add(classBox);
        controls.add(Box.createVerticalStrut(20));
        controls.add(findButton);
        controls.add(Box.createVerticalStrut(10));
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
        // Find Route Button
        // --------------------
        findButton.addActionListener(e -> {
    String startCity = (String) startCityBox.getSelectedItem();
    String startState = (String) startStateBox.getSelectedItem();
    String destCity = (String) destCityBox.getSelectedItem();
    String destState = (String) destStateBox.getSelectedItem();
    String classType = (String) classBox.getSelectedItem(); // selected travel class

    // Clear previous output
    try { doc.remove(0, doc.getLength()); } catch (Exception ignored) {}

    // -------------------------
    // Calculate lowest price
    // -------------------------
    String fromCode = RouteFinder.stationsMap.entrySet().stream()
            .filter(entry -> entry.getValue().city.equalsIgnoreCase(startCity) &&
                             entry.getValue().state.equalsIgnoreCase(startState))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);

    String toCode = RouteFinder.stationsMap.entrySet().stream()
            .filter(entry -> entry.getValue().city.equalsIgnoreCase(destCity) &&
                             entry.getValue().state.equalsIgnoreCase(destState))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);

    if (fromCode != null && toCode != null) {
        double price = RouteFinder.calculateLowestPrice(fromCode, toCode, classType);
        appendStyled(doc, "Lowest possible price: $" + String.format("%.2f", price) + "\n\n", normal);
    }

    // -------------------------
    // Find matching routes
    // -------------------------
    List<ApiHandler.Route> matches =
            RouteFinder.findRoutesByCityAndState(allRoutes, startCity, startState, destCity, destState);

    if (matches.isEmpty()) {
        appendStyled(doc,
             "No direct trains from " + startCity + ", " + startState +
             " to " + destCity + ", " + destState + "\n",
             normal
        );
        return;
    }

    appendStyled(doc, "-------------------------------------------\n", normal);

    for (ApiHandler.Route route : matches) {

        appendStyled(doc, "Route: " + route.route + "\n", normal);

        Set<String> printedTrains = new HashSet<>();

        for (ApiHandler.Train train : route.trains) {
            String trainKey = train.number + "-" + train.heading;
            if (printedTrains.contains(trainKey)) continue;

            int startIndex = -1, destIndex = -1;
            List<ApiHandler.Station> stations = train.stations;

            // Find index of start + destination
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
                printedTrains.add(trainKey);

                appendStyled(doc, "  Train #" + train.number + " (" + train.heading + ")\n", normal);
                appendStyled(doc, "    Departure from " + startCity + "\n", normal);
                appendStyled(doc, "    Arrival at " + destCity + "\n", normal);

                // List stations, bold start and end stations
                for (int i = startIndex; i <= destIndex; i++) {
                    ApiHandler.Station s = stations.get(i);
                    String line = "      • " + s.station.state +
                                  " — " + s.station.city +
                                  " — " + s.station.name + "\n";

                    if (i == startIndex || i == destIndex)
                        appendStyled(doc, line, bold);
                    else
                        appendStyled(doc, line, normal);
                }

                appendStyled(doc, "-------------------------------------------\n", normal);

                // Compute travel time
                try {
                    java.time.ZonedDateTime dep = java.time.ZonedDateTime.parse(
                            stations.get(startIndex).departureActual != null
                                    ? stations.get(startIndex).departureActual
                                    : stations.get(startIndex).departureScheduled
                    );

                    java.time.ZonedDateTime arr = java.time.ZonedDateTime.parse(
                            stations.get(destIndex).arrivalActual != null
                                    ? stations.get(destIndex).arrivalActual
                                    : stations.get(destIndex).arrivalScheduled
                    );

                    java.time.Duration duration = java.time.Duration.between(dep, arr);
                    long hours = duration.toHours();
                    long minutes = duration.toMinutes() % 60;

                    appendStyled(doc, "EST: " + hours + " hours", normal);
                    if (minutes > 0) {
                        appendStyled(doc, " " + minutes + " mins", normal);
                    }
                    appendStyled(doc, "\n-------------------------------------------\n", normal);

                } catch (Exception ex) {
                    appendStyled(doc, "EST: N/A (can't compute)\n", normal);
                    appendStyled(doc, "-------------------------------------------\n", normal);
                }
            }
        }
    }
});


        gui.setVisible(true);
    }

    // ---------------------------------------------
    // Styled output helper
    // ---------------------------------------------
    private void appendStyled(StyledDocument doc, String text, AttributeSet style) {
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------- State / City Helpers --------------------

    private List<String> getAllStates(List<ApiHandler.Route> routes) {
        Set<String> set = new HashSet<>();
        for (ApiHandler.Route r : routes) {
            for (ApiHandler.Train t : r.trains) {
                for (ApiHandler.Station s : t.stations) {
                    if (s.station != null && s.station.state != null)
                        set.add(s.station.state);
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
                    if (s.station != null && s.station.city != null)
                        set.add(s.station.city);
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
                    if (s.station != null &&
                        state.equalsIgnoreCase(s.station.state))
                        set.add(s.station.city);
                }
            }
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }
     // -------------------- Pricing Helpers --------------------
    // Assume you have the station codes (3-letter) for start and destination


}
