import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class Main {

    private List<ApiHandler.Route> allRoutes;   // all routes loaded from API

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().createGUI());
    }

    private void createGUI() {
        // Load routes from your API JSON
        allRoutes = ApiHandler.loadRoutes();   // <-- use your real method!

        // Build station list from the routes
        String[] stations = ApiHandler.getAllCities(allRoutes);

        // -----------------------
        JFrame gui = new JFrame("Train Planner");
        gui.setSize(700, 450);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.setLayout(new BorderLayout());

        // LEFT SIDE (Map placeholder)
        JLabel mapPlaceholder = new JLabel("MAP", SwingConstants.CENTER);
        mapPlaceholder.setPreferredSize(new Dimension(350, 450));
        gui.add(mapPlaceholder, BorderLayout.WEST);

        // RIGHT SIDE (Controls)
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JComboBox<String> startBox = new JComboBox<>(stations);
        JComboBox<String> destBox = new JComboBox<>(stations);

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

        // -----------------------
        // BUTTON ACTION
        // -----------------------
        findButton.addActionListener(e -> {
            String start = (String) startBox.getSelectedItem();
            String dest = (String) destBox.getSelectedItem();

            List<ApiHandler.Route> matches =
                    RouteFinder.findRoutesByCity(allRoutes, start, dest);

            if (matches.isEmpty()) {
                results.setText("No direct trains found from " + start + " to " + dest);
                return;
            }

            StringBuilder out = new StringBuilder();

            for (ApiHandler.Route r : matches) {
                out.append("Route ID: ").append(r.id).append("\n");

                for (ApiHandler.Train t : r.trains) {
                    out.append("  Train: ").append(t.name).append("\n");

                    for (ApiHandler.Station s : t.stations) {
                        out.append("      • ")
                           .append(s.station.city)
                           .append(" (")
                           .append(s.station.code)
                           .append(")")
                           .append("\n");
                    }
                }

                out.append("\n---\n");
            }

            results.setText(out.toString());
        });

        gui.setVisible(true);
    }
}
