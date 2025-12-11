package app;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.io.IOException;

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

        // ----------------------------
        // Populate station lookup data
        // ----------------------------
        List<ApiHandler.Station> flatStations = new ArrayList<>();
        for (ApiHandler.Route r : allRoutes)
            for (ApiHandler.Train t : r.trains)
                flatStations.addAll(t.stations);

        RouteFinder.populateStationsMap(allRoutes);

        // --------------------
        // Build state & city lists
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
        JFXPanel fxPanel = new JFXPanel();
            Platform.runLater(() -> {
                WebView webView = new WebView();
                fxPanel.setScene(new Scene(webView));
            });
        mapPanel.add(fxPanel);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));

        JComboBox<String> startStateBox = new JComboBox<>(allStates);
        JComboBox<String> startCityBox = new JComboBox<>(allCities);
        JComboBox<String> destStateBox = new JComboBox<>(allStates);
        JComboBox<String> destCityBox = new JComboBox<>(allCities);
        JComboBox<String> classBox = new JComboBox<>(new String[]{"economy", "business", "first", "private"});

        JButton findButton = new JButton("Find Route");

        JTextPane results = new JTextPane();
        results.setEditable(false);
        results.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scroll = new JScrollPane(results);
        scroll.setPreferredSize(new Dimension(350, 200));

        StyledDocument doc = results.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);

        // Add GUI controls
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
        // Filter cities by state (dynamic)
        // --------------------
        startStateBox.addActionListener(e -> {
            String st = (String) startStateBox.getSelectedItem();
            startCityBox.setModel(new DefaultComboBoxModel<>(getCitiesByState(st).toArray(new String[0])));
        });

        destStateBox.addActionListener(e -> {
            String st = (String) destStateBox.getSelectedItem();
            destCityBox.setModel(new DefaultComboBoxModel<>(getCitiesByState(st).toArray(new String[0])));
        });

        // -----------------------------------------
        // Find Route Click Action
        // -----------------------------------------
        findButton.addActionListener(e -> {
            String startCity = (String) startCityBox.getSelectedItem();
            String startState = (String) startStateBox.getSelectedItem();
            String destCity = (String) destCityBox.getSelectedItem();
            String destState = (String) destStateBox.getSelectedItem();
            String classType = (String) classBox.getSelectedItem();

            try { doc.remove(0, doc.getLength()); } catch (Exception ignored) {}

            // -------------------------
            // PRICE CALC LOOKUP
            // -------------------------
            

            // -------------------------
            // Find matching routes
            // -------------------------
            List<ApiHandler.Route> matches =
                    RouteFinder.findRoutesByCityAndState(allRoutes, startCity, startState, destCity, destState);

            if(startCity.equals(destCity) && startState.equals(destState)){
                 appendStyled(doc, "Can not route a train's destination to it's start" + "\n", normal);
                return;
            }
            if (matches.isEmpty()) {
                appendStyled(doc, "No direct trains from " + startCity + ", " + startState +
                                      " to " + destCity + ", " + destState + "\n", normal);
                return;
            }

            appendStyled(doc, "-------------------------------------------\n", normal);

            for (ApiHandler.Route r : matches) {
                appendStyled(doc, "Route: " + r.route + "\n", normal);

                Set<String> seenTrains = new HashSet<>();

                for (ApiHandler.Train t : r.trains) {
                    String key = t.number + "-" + t.heading;
                    if (seenTrains.contains(key)) continue;

                    int startIndex = -1, destIndex = -1;
                    List<ApiHandler.Station> stList = t.stations;

                    for (int i = 0; i < stList.size(); i++) {
                        ApiHandler.Station s = stList.get(i);
                        if (s.station != null) {
                            if (s.station.city.equalsIgnoreCase(startCity) &&
                                s.station.state.equalsIgnoreCase(startState)) startIndex = i;

                            if (s.station.city.equalsIgnoreCase(destCity) &&
                                s.station.state.equalsIgnoreCase(destState)) destIndex = i;
                        }
                    }

                    if (startIndex != -1 && destIndex != -1 && startIndex < destIndex) {
                        seenTrains.add(key);

                        appendStyled(doc, "  Train #" + t.number + " (" + t.heading + ")\n", normal);
                        appendStyled(doc, "    Departure from " + startCity + "\n", normal);
                        appendStyled(doc, "    Arrival at " + destCity + "\n", normal);

                        for (int i = startIndex; i <= destIndex; i++) {
                            ApiHandler.Station s = stList.get(i);
                            String line = "      • " + s.station.state +
                                          " — " + s.station.city +
                                          " — " + s.station.name + "\n";

                            if (i == startIndex || i == destIndex)
                                appendStyled(doc, line, bold);
                            else
                                appendStyled(doc, line, normal);
                        }

                        appendStyled(doc, "-------------------------------------------\n", normal);

                        // Compute travel time (unchanged)
                        try {
                            var dep = java.time.ZonedDateTime.parse(
                                    stList.get(startIndex).departureActual != null ?
                                    stList.get(startIndex).departureActual :
                                    stList.get(startIndex).departureScheduled);

                            var arr = java.time.ZonedDateTime.parse(
                                    stList.get(destIndex).arrivalActual != null ?
                                    stList.get(destIndex).arrivalActual :
                                    stList.get(destIndex).arrivalScheduled);

                            var dur = java.time.Duration.between(dep, arr);
                            long h = dur.toHours();
                            long m = dur.toMinutes() % 60;

                            appendStyled(doc, "EST: " + h + " hours" + (m > 0 ? " " + m + " mins" : "") + "\n", normal);
                            appendStyled(doc, "-------------------------------------------\n", normal);

                        } catch (Exception ex) {
                            appendStyled(doc, "EST: N/A\n-------------------------------------------\n", normal);
                        }
                        String fromCode = RouteFinder.stationsMap.entrySet().stream()
                    .filter(e2 -> e2.getValue().city.equalsIgnoreCase(startCity) &&
                                  e2.getValue().state.equalsIgnoreCase(startState))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);

            String toCode = RouteFinder.stationsMap.entrySet().stream()
                    .filter(e2 -> e2.getValue().city.equalsIgnoreCase(destCity) &&
                                  e2.getValue().state.equalsIgnoreCase(destState))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);

            if (fromCode != null && toCode != null) {
                double price = RouteFinder.calculateLowestPrice(fromCode, toCode, classType);
                appendStyled(doc, "Lowest possible price: $" + String.format("%.2f", price) + "", normal);
                appendStyled(doc, "\n-------------------------------------------\n", normal);
            }
                    }
                }
            }
        });

        gui.setVisible(true);
    }

    // Helper to print styled text
    private void appendStyled(StyledDocument doc, String text, AttributeSet style) {
        try { doc.insertString(doc.getLength(), text, style); }
        catch (Exception ignored) {}
    }

    // State / City helpers
    private List<String> getAllStates(List<ApiHandler.Route> routes) {
        Set<String> s = new HashSet<>();
        for (ApiHandler.Route r : routes)
            for (ApiHandler.Train t : r.trains)
                for (ApiHandler.Station st : t.stations)
                    if (st.station != null && st.station.state != null)
                        s.add(st.station.state);
        return new ArrayList<>(s);
    }

    private List<String> getAllCities(List<ApiHandler.Route> routes) {
        Set<String> s = new HashSet<>();
        for (ApiHandler.Route r : routes)
            for (ApiHandler.Train t : r.trains)
                for (ApiHandler.Station st : t.stations)
                    if (st.station != null && st.station.city != null)
                        s.add(st.station.city);
        return new ArrayList<>(s);
    }

    private List<String> getCitiesByState(String state) {
        Set<String> s = new HashSet<>();
        for (ApiHandler.Route r : allRoutes)
            for (ApiHandler.Train t : r.trains)
                for (ApiHandler.Station st : t.stations)
                    if (st.station != null &&
                        st.station.state.equalsIgnoreCase(state))
                        s.add(st.station.city);
        List<String> out = new ArrayList<>(s);
        Collections.sort(out);
        return out;
    }
}
