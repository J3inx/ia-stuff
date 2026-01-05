package app;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.*;
import javafx.scene.web.WebView;

public class Main {

    private List<ApiHandler.Route> allRoutes;
    private int xof = 0;

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
        gui.setSize(390, 500);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.setLayout(new BorderLayout());

        JLabel mapPanel = new JLabel("MAP", SwingConstants.CENTER);
        mapPanel.setPreferredSize(new Dimension(350, 500));
       // gui.add(mapPanel, BorderLayout.WEST);

        JFXPanel fxPanel = new JFXPanel();
        Platform.runLater(() -> {
            WebView webView = new WebView();
            fxPanel.setScene(new Scene(webView));
        });
       // mapPanel.add(fxPanel);

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
        // Filter cities by state
        // --------------------
        startStateBox.addActionListener(e -> {
            String st = (String) startStateBox.getSelectedItem();
            startCityBox.setModel(new DefaultComboBoxModel<>(
                    getCitiesByState(st).toArray(new String[0])));
        });

        destStateBox.addActionListener(e -> {
            String st = (String) destStateBox.getSelectedItem();
            destCityBox.setModel(new DefaultComboBoxModel<>(
                    getCitiesByState(st).toArray(new String[0])));
        });

        // -----------------------------------------
        // Find Route Click Action (PATCHED)
        // -----------------------------------------
        findButton.addActionListener(e -> {
            try { doc.remove(0, doc.getLength()); } catch (Exception ignored) {}
        
            if (xof != 0) {
                appendStyled(doc, "Routing system failed to initialize.\n", normal);
                return;
            }
        
            String startCity = (String) startCityBox.getSelectedItem();
            String startState = (String) startStateBox.getSelectedItem();
            String destCity = (String) destCityBox.getSelectedItem();
            String destState = (String) destStateBox.getSelectedItem();
            String classType = (String) classBox.getSelectedItem();
             System.out.println("user routed from: " + startCity + ", " + startState + " to: " + destCity+ ", " + destState);
            if (startCity.equals(destCity) && startState.equals(destState)) {
                appendStyled(doc, "Can not route a train's destination to its start\n", normal);
                return;
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
        
            if (fromCode == null || toCode == null) {
                appendStyled(doc, "No valid stations found.\n", normal);
                return;
            }
        
            // ---------------------------
            // Animated Unicode Loading
            // ---------------------------
            String[] spinnerFrames = new String[]{"⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"};
            final int[] frameIndex = {0};
        
            javax.swing.Timer spinnerTimer = new javax.swing.Timer(200, evt -> {
                try {
                    doc.remove(0, doc.getLength());
                } catch (Exception ignored) {}
                appendStyled(doc,spinnerFrames[frameIndex[0]] + " " + "Calculating route... " + spinnerFrames[frameIndex[0]] + "\n", normal);
                frameIndex[0] = (frameIndex[0] + 1) % spinnerFrames.length;
            });
            spinnerTimer.start();
        
            // ---------------------------
            // Run DFS in background thread
            // ---------------------------
            SwingWorker<RouteFinder.PathResult, Void> worker = new SwingWorker<>() {
                @Override
                protected RouteFinder.PathResult doInBackground() {
                    return RouteFinder.findShortestRoute(allRoutes, fromCode, toCode);
                }
            
                @Override
                protected void done() {
                    spinnerTimer.stop(); 
                    try {
                        doc.remove(0, doc.getLength());
                        RouteFinder.PathResult result = get();
            
                        if (result.bestPath.isEmpty()) {
                            appendStyled(doc, "No route found.\n", normal);
                            return;
                        }
            
                        appendStyled(doc, "-------------------------------------------\n", normal);
                        appendStyled(doc, "Optimal Route\n", bold);
            
                        ApiHandler.Train lastTrain = null;
                        int x = 0;
                        for (RouteFinder.Leg leg : result.bestPath) {
                            if(x==0){
                                appendStyled(doc,
                                    "\t🚆 Train #" + leg.train.number +
                                    " → " + startCity + ", " +
                                    startState + "\n",
                                    normal);
                                  x++;  
                            }
                            if (lastTrain != null && leg.train.number != lastTrain.number) {
                                appendStyled(doc,
                                        "🔁 Switch trains at " +
                                        leg.from.station.city + ", " +
                                        leg.from.station.state +
                                        " → Train #" + leg.train.number + "\n",
                                        bold);
                            }
            
                            appendStyled(doc,
                                    "\t🚆 Train #" + leg.train.number +
                                    " → " + leg.to.station.city + ", " +
                                    leg.to.station.state + "\n",
                                    normal);
            
                            lastTrain = leg.train;
                        }
            
                        long h = result.totalMinutes / 60;
                        long m = result.totalMinutes % 60;
                        appendStyled(doc,
                                "EST: " + h + " hours" +
                                (m > 0 ? " " + m + " mins" : "") + "\n",
                                normal);
            
                        double price = RouteFinder.calculateLowestPrice(fromCode, toCode, classType);
                        appendStyled(doc,
                                "Lowest possible price: $" +
                                String.format("%.2f", price) + "\n",
                                normal);
            
                        appendStyled(doc, "-------------------------------------------\n", normal);
            
                    } catch (Exception ex) {
                        appendStyled(doc, "Error calculating route: " + ex.getMessage() + "\n", normal);
                    }
                }
            };
            
        
            worker.execute();  
        });
        

        // --------------------
        // Build leg graph
        // --------------------
        try {
            System.out.println("Building leg graphs...");
            RouteFinder.buildLegGraph(allRoutes);
        } catch (Exception e) {
            System.err.println("Error building leg graph: " + e.getMessage());
            xof = 1;
        }

        if (xof == 0) {
            System.out.println("leg graph built successfully");
        }

        gui.setVisible(true);
    }

    private void appendStyled(StyledDocument doc, String text, AttributeSet style) {
        try { doc.insertString(doc.getLength(), text, style); }
        catch (Exception ignored) {}
    }

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
