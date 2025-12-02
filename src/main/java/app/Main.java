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
        // --------------------
        // Load routes from JSON
        // --------------------
        allRoutes = ApiHandler.loadRoutes();   // uses your existing loader

        // --------------------
        // Build city list
        // --------------------
        String[] allCities = getAllCities(allRoutes);

        // --------------------
        // GUI Setup
        // --------------------
        JFrame gui = new JFrame("Train Planner");
        gui.setSize(700, 450);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.setLayout(new BorderLayout());

        // LEFT map placeholder
        JLabel mapPanel = new JLabel("MAP", SwingConstants.CENTER);
        mapPanel.setPreferredSize(new Dimension(350, 450));
        gui.add(mapPanel, BorderLayout.WEST);

        // RIGHT controls panel
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JComboBox<String> startBox = new JComboBox<>(allCities);
        JComboBox<String> destBox = new JComboBox<>(allCities);

        JButton findButton = new JButton("Find Route");

        JTextArea results = new JTextArea(13, 25);
        results.setEditable(false);
        JScrollPane scroll = new JScrollPane(results);

        controls.add(new JLabel("Start:"));
        controls.add(startBox);
        controls.add(Box.createVerticalStrut(10));

        controls.add(new JLabel("Destination:"));
        controls.add(destBox);
        controls.add(Box.createVerticalStrut(20));

        controls.add(findButton);
        controls.add(Box.createVerticalStrut(20));

        controls.add(new JLabel("Results:"));
        controls.add(scroll);

        gui.add(controls, BorderLayout.EAST);

        // --------------------
        // BUTTON ACTION
        // --------------------
        findButton.addActionListener(e -> {
            String start = (String) startBox.getSelectedItem();
            String dest = (String) destBox.getSelectedItem();

            List<ApiHandler.Route> matches =
                    RouteFinder.findRoutesByCity(allRoutes, start, dest);

            if (matches.isEmpty()) {
                results.setText("No direct trains from " + start + " to " + dest);
                return;
            }

            StringBuilder out = new StringBuilder();

            for (ApiHandler.Route route : matches) {
                out.append("Route: ").append(route.route).append("\n");

                for (ApiHandler.Train t : route.trains) {
                    out.append("  Train #").append(t.number)
                       .append(" (").append(t.heading).append(")\n");

                    for (ApiHandler.Station s : t.stations) {
                        out.append("      • ")
                           .append(s.station.city)
                           .append(" — ")
                           .append(s.station.name)
                           .append("\n");
                    }
                }

                out.append("\n-----------------------\n");
            }

            results.setText(out.toString());
        });

        gui.setVisible(true);
    }

    // -------------------------------------------------------
    // Extract all unique city names from your nested structure
    // -------------------------------------------------------
    private String[] getAllCities(List<ApiHandler.Route> routes) {
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

        return set.toArray(new String[0]);
    }
}
