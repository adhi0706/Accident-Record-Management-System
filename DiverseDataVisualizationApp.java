import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class DiverseDataVisualizationApp extends JFrame {

    private JPanel panel;
    private JButton uploadButton;
    private JButton generateButton;
    private JComboBox<String> chartTypeComboBox;
    private JComboBox<String> categoryColumnComboBox;
    private JComboBox<String> numericColumnComboBox;
    private JComboBox<String> xAxisComboBox;
    private JComboBox<String> yAxisComboBox;
    private SimpleDateFormat[] dateFormats = {
        new SimpleDateFormat("yyyy-MM-dd"),
        new SimpleDateFormat("dd-MM-yyyy"),
        new SimpleDateFormat("MM/dd/yyyy"),
        new SimpleDateFormat("dd/MM/yyyy")
    };

    private List<String[]> data;
    private String[] headers;
    private JTable dataTable;
    private JScrollPane tableScrollPane;
    private List<Integer> numericColumns;
    private List<Integer> categoryColumns;
    private Integer dateColumnIndex;

    public DiverseDataVisualizationApp() {
        setTitle("Diverse Data Visualization");
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(240, 248, 255));
        JScrollPane scrollPane = new JScrollPane(panel);

        uploadButton = new JButton("Upload Dataset");
        uploadButton.setBackground(new Color(70, 130, 180));
        uploadButton.setForeground(Color.WHITE);
        uploadButton.setFocusPainted(false);

        generateButton = new JButton("Generate Visualization");
        generateButton.setBackground(new Color(34, 139, 34));
        generateButton.setForeground(Color.WHITE);
        generateButton.setFocusPainted(false);
        generateButton.setEnabled(false);

        chartTypeComboBox = new JComboBox<>(new String[]{"Bar Chart", "Pie Chart", "Line Chart", "Scatter Chart"});
        chartTypeComboBox.setBackground(Color.WHITE);
        
        categoryColumnComboBox = new JComboBox<>();
        categoryColumnComboBox.setBackground(Color.WHITE);
        
        numericColumnComboBox = new JComboBox<>();
        numericColumnComboBox.setBackground(Color.WHITE);
        
        xAxisComboBox = new JComboBox<>();
        xAxisComboBox.setBackground(Color.WHITE);
        
        yAxisComboBox = new JComboBox<>();
        yAxisComboBox.setBackground(Color.WHITE);

        uploadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        loadData(file);
                        JOptionPane.showMessageDialog(null, "Data loaded successfully!");
                        generateButton.setEnabled(true);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "Error loading file: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        });

        chartTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateColumnSelectors();
            }
        });

        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (data == null || data.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please upload a dataset first!");
                    return;
                }
                String selectedChartType = (String) chartTypeComboBox.getSelectedItem();
                try {
                    createVisualization(selectedChartType);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error generating chart: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(240, 248, 255));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        topPanel.add(uploadButton);
        topPanel.add(new JLabel("Chart Type:"));
        topPanel.add(chartTypeComboBox);
        topPanel.add(categoryColumnComboBox);
        topPanel.add(numericColumnComboBox);
        topPanel.add(xAxisComboBox);
        topPanel.add(yAxisComboBox);
        topPanel.add(generateButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadData(File file) throws IOException {
        panel.removeAll();
        data = new ArrayList<>();
        headers = null;
        numericColumns = new ArrayList<>();
        categoryColumns = new ArrayList<>();
        dateColumnIndex = null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (headers == null) {
                    headers = values;
                } else {
                    data.add(values);
                }
            }
        }

        if (headers == null || headers.length < 2 || data.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Invalid data format!");
            return;
        }

        analyzeColumns();

        String[][] tableData = new String[data.size()][headers.length];
        for (int i = 0; i < data.size(); i++) {
            tableData[i] = data.get(i);
        }

        dataTable = new JTable(tableData, headers);
        tableScrollPane = new JScrollPane(dataTable);
        tableScrollPane.setPreferredSize(new Dimension(1150, 200));
        tableScrollPane.setMaximumSize(new Dimension(1150, 200));
        
        panel.add(tableScrollPane);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        updateColumnSelectors();

        panel.revalidate();
        panel.repaint();
    }

    private void analyzeColumns() {
        for (int col = 0; col < headers.length; col++) {
            boolean isNumeric = true;
            boolean isDate = true;

            for (String[] row : data) {
                if (col >= row.length) continue;
                
                String value = row[col].trim();
                if (value.isEmpty()) continue;

                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    isNumeric = false;
                }

                if (isDate && !tryParseDate(value)) {
                    isDate = false;
                }
            }

            if (isDate && dateColumnIndex == null) {
                dateColumnIndex = col;
            } else if (isNumeric) {
                numericColumns.add(col);
            } else {
                categoryColumns.add(col);
            }
        }
    }

    private boolean tryParseDate(String value) {
        for (SimpleDateFormat format : dateFormats) {
            try {
                format.parse(value);
                return true;
            } catch (Exception e) {
                continue;
            }
        }
        return false;
    }

    private Date parseDate(String value) {
        for (SimpleDateFormat format : dateFormats) {
            try {
                return format.parse(value);
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    private void updateColumnSelectors() {
        String chartType = (String) chartTypeComboBox.getSelectedItem();
        
        categoryColumnComboBox.removeAllItems();
        numericColumnComboBox.removeAllItems();
        xAxisComboBox.removeAllItems();
        yAxisComboBox.removeAllItems();
        
        categoryColumnComboBox.setVisible(false);
        numericColumnComboBox.setVisible(false);
        xAxisComboBox.setVisible(false);
        yAxisComboBox.setVisible(false);

        if (chartType.equals("Bar Chart")) {
            categoryColumnComboBox.setVisible(true);
            numericColumnComboBox.setVisible(true);
            for (int col : categoryColumns) {
                categoryColumnComboBox.addItem(headers[col]);
            }
            for (int col : numericColumns) {
                numericColumnComboBox.addItem(headers[col]);
            }
        } else if (chartType.equals("Pie Chart")) {
            categoryColumnComboBox.setVisible(true);
            for (int col : categoryColumns) {
                categoryColumnComboBox.addItem(headers[col]);
            }
        } else if (chartType.equals("Line Chart")) {
            categoryColumnComboBox.setVisible(true);
            numericColumnComboBox.setVisible(true);
            for (int col : categoryColumns) {
                categoryColumnComboBox.addItem(headers[col]);
            }
            for (int col : numericColumns) {
                numericColumnComboBox.addItem(headers[col]);
            }
        } else if (chartType.equals("Scatter Chart")) {
            xAxisComboBox.setVisible(true);
            yAxisComboBox.setVisible(true);
            for (int col : numericColumns) {
                xAxisComboBox.addItem(headers[col]);
                yAxisComboBox.addItem(headers[col]);
            }
            if (numericColumns.size() > 1) {
                yAxisComboBox.setSelectedIndex(1);
            }
        }
    }

    private void createVisualization(String chartType) {
        if (headers == null || headers.length < 2 || data == null || data.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No data available!");
            return;
        }

        Component[] components = panel.getComponents();
        for (int i = components.length - 1; i >= 0; i--) {
            if (components[i] instanceof ChartPanel || components[i] instanceof JLabel) {
                panel.remove(components[i]);
            }
        }

        JLabel visualizationLabel = new JLabel("Data Visualization");
        visualizationLabel.setFont(new Font("Serif", Font.BOLD, 20));
        visualizationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(visualizationLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        switch (chartType) {
            case "Bar Chart":
                createBarChart();
                break;
            case "Pie Chart":
                createPieChart();
                break;
            case "Line Chart":
                createLineChart();
                break;
            case "Scatter Chart":
                createScatterChart();
                break;
        }

        panel.revalidate();
        panel.repaint();
    }

    private void createBarChart() {
        String categoryCol = (String) categoryColumnComboBox.getSelectedItem();
        String numericCol = (String) numericColumnComboBox.getSelectedItem();
        
        if (categoryCol == null || numericCol == null) {
            JOptionPane.showMessageDialog(null, "Please select valid columns!");
            return;
        }

        int catIndex = getColumnIndex(categoryCol);
        int numIndex = getColumnIndex(numericCol);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Double> aggregated = new HashMap<>();

        for (String[] row : data) {
            try {
                if (catIndex >= row.length || numIndex >= row.length) continue;
                String category = row[catIndex].trim();
                double value = Double.parseDouble(row[numIndex].trim());
                aggregated.put(category, aggregated.getOrDefault(category, 0.0) + value);
            } catch (Exception e) {
                continue;
            }
        }

        for (Map.Entry<String, Double> entry : aggregated.entrySet()) {
            dataset.addValue(entry.getValue(), numericCol, entry.getKey());
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                numericCol + " by " + categoryCol,
                categoryCol,
                numericCol,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(1150, 400));
        chartPanel.setMaximumSize(new Dimension(1150, 400));
        chartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(chartPanel);
    }

    private void createPieChart() {
        String categoryCol = (String) categoryColumnComboBox.getSelectedItem();
        
        if (categoryCol == null) {
            JOptionPane.showMessageDialog(null, "Please select a category column!");
            return;
        }

        int catIndex = getColumnIndex(categoryCol);
        DefaultPieDataset dataset = new DefaultPieDataset();
        Map<String, Integer> counts = new HashMap<>();

        for (String[] row : data) {
            try {
                if (catIndex >= row.length) continue;
                String category = row[catIndex].trim();
                if (category.isEmpty()) continue;
                counts.put(category, counts.getOrDefault(category, 0) + 1);
            } catch (Exception e) {
                continue;
            }
        }

        if (counts.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No data available for pie chart!");
            return;
        }

        // Limit to top 10 categories if there are too many
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(counts.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int limit = Math.min(10, sortedEntries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = sortedEntries.get(i);
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Distribution of " + categoryCol + " (Top " + limit + ")",
                dataset,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(1150, 400));
        chartPanel.setMaximumSize(new Dimension(1150, 400));
        chartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(chartPanel);
    }

    private void createLineChart() {
        String categoryCol = (String) categoryColumnComboBox.getSelectedItem();
        String numericCol = (String) numericColumnComboBox.getSelectedItem();
        
        if (categoryCol == null || numericCol == null) {
            JOptionPane.showMessageDialog(null, "Please select valid columns!");
            return;
        }

        int catIndex = getColumnIndex(categoryCol);
        int numIndex = getColumnIndex(numericCol);

        if (dateColumnIndex != null) {
            createTimeSeriesChart(catIndex, numIndex, numericCol);
        } else {
            createCategoryLineChart(catIndex, numIndex, categoryCol, numericCol);
        }
    }

    private void createTimeSeriesChart(int catIndex, int numIndex, String numericCol) {
        Map<String, TimeSeries> seriesMap = new HashMap<>();

        for (String[] row : data) {
            try {
                if (catIndex >= row.length || numIndex >= row.length || dateColumnIndex >= row.length) continue;
                
                String category = row[catIndex].trim();
                Date date = parseDate(row[dateColumnIndex].trim());
                double value = Double.parseDouble(row[numIndex].trim());
                
                if (date == null) continue;

                if (!seriesMap.containsKey(category)) {
                    seriesMap.put(category, new TimeSeries(category));
                }
                seriesMap.get(category).add(new Day(date), value);
            } catch (Exception e) {
                continue;
            }
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (TimeSeries series : seriesMap.values()) {
            dataset.addSeries(series);
        }

        JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
                numericCol + " Over Time",
                "Date",
                numericCol,
                dataset,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new Dimension(1150, 400));
        chartPanel.setMaximumSize(new Dimension(1150, 400));
        chartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(chartPanel);
    }

    private void createCategoryLineChart(int catIndex, int numIndex, String categoryCol, String numericCol) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Double> aggregated = new LinkedHashMap<>();

        for (String[] row : data) {
            try {
                if (catIndex >= row.length || numIndex >= row.length) continue;
                String category = row[catIndex].trim();
                double value = Double.parseDouble(row[numIndex].trim());
                aggregated.put(category, aggregated.getOrDefault(category, 0.0) + value);
            } catch (Exception e) {
                continue;
            }
        }

        for (Map.Entry<String, Double> entry : aggregated.entrySet()) {
            dataset.addValue(entry.getValue(), numericCol, entry.getKey());
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                numericCol + " Trend",
                categoryCol,
                numericCol,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new Dimension(1150, 400));
        chartPanel.setMaximumSize(new Dimension(1150, 400));
        chartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(chartPanel);
    }

    private void createScatterChart() {
        String xCol = (String) xAxisComboBox.getSelectedItem();
        String yCol = (String) yAxisComboBox.getSelectedItem();
        
        if (xCol == null || yCol == null) {
            JOptionPane.showMessageDialog(null, "Please select valid columns!");
            return;
        }

        int xIndex = getColumnIndex(xCol);
        int yIndex = getColumnIndex(yCol);

        XYSeries series = new XYSeries("Data Points");

        for (String[] row : data) {
            try {
                if (xIndex >= row.length || yIndex >= row.length) continue;
                double xValue = Double.parseDouble(row[xIndex].trim());
                double yValue = Double.parseDouble(row[yIndex].trim());
                series.add(xValue, yValue);
            } catch (Exception e) {
                continue;
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart scatterChart = ChartFactory.createScatterPlot(
                xCol + " vs " + yCol,
                xCol,
                yCol,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = (XYPlot) scatterChart.getPlot();
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setDotHeight(8);
        renderer.setDotWidth(8);
        plot.setRenderer(renderer);

        ChartPanel chartPanel = new ChartPanel(scatterChart);
        chartPanel.setPreferredSize(new Dimension(1150, 400));
        chartPanel.setMaximumSize(new Dimension(1150, 400));
        chartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(chartPanel);
    }

    private int getColumnIndex(String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new DiverseDataVisualizationApp().setVisible(true);
            }
        });
    }
}