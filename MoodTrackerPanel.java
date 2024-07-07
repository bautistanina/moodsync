import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodTrackerPanel extends JPanel {

    private Map<String, Integer> moodCounts;
    private JFreeChart chart;
    private DefaultCategoryDataset dataset;
    private Map<Integer, List<LocalDate>> userMoodEntries;
    private int userId;
    private StringBuilder conversationContext;
    private Map<String, String> moodMapping;

    public MoodTrackerPanel(int userId) {
        moodCounts = new HashMap<>();
        this.userId = userId;
        userMoodEntries = new HashMap<>();
        conversationContext = new StringBuilder();
        initializeMoodMapping();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Set margin to the frame
        setBackground(new Color(25, 27, 44)); // Hex #191B2C
        
        Font montserratReg; //Montserrat Regular Font
        try {
            montserratReg = Font.createFont(Font.TRUETYPE_FONT, new File("lib\\Montserrat\\static\\Montserrat-Regular.ttf")).deriveFont(30f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(montserratReg);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            montserratReg = new Font("Arial", Font.BOLD, 30); 
        }
        float newRSize = 40f; // Change this value to resize the font after loading
        montserratReg = montserratReg.deriveFont(newRSize);
        
        Font montserratBold; //Montserrat Bold Font
        try {
            montserratBold = Font.createFont(Font.TRUETYPE_FONT, new File("lib\\Montserrat\\static\\Montserrat-ExtraBold.ttf")).deriveFont(30f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(montserratBold);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            montserratBold = new Font("Arial", Font.BOLD, 30); 
        }
        float newBSize = 40f; // Change this value to resize the font after loading
        montserratBold = montserratBold.deriveFont(newBSize);

        JLabel label = new JLabel("<html><div style='margin-bottom: 15px; font-size: 28px; text-align: center;'>MOOD TRACKER</div></html>", JLabel.CENTER);
        label.setFont(montserratBold.deriveFont(Font.BOLD, 40));
        label.setForeground(Color.WHITE);
        add(label, BorderLayout.NORTH);

        dataset = new DefaultCategoryDataset();
        chart = ChartFactory.createBarChart(
            "Mood Distribution", "", "Count", dataset,
            PlotOrientation.VERTICAL, false, true, false);

        applyRainbowColors(chart);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200)); // Resize the chart
        add(chartPanel, BorderLayout.CENTER);

        final Font finalMontserratBold = montserratBold; // Make a final copy for use in the listener

JButton updateMoodButton = new JButton("<html><b>Update Tracker</b></html>");
updateMoodButton.setFont(finalMontserratBold.deriveFont(Font.BOLD, 20));
updateMoodButton.addActionListener(e -> updateMoodAnalysis());
updateMoodButton.setPreferredSize(new Dimension(300, 50));
updateMoodButton.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseEntered(MouseEvent e) {
        updateMoodButton.setFont(finalMontserratBold.deriveFont(Font.BOLD, 20));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        updateMoodButton.setFont(finalMontserratBold.deriveFont(Font.BOLD, 20));
    }
});

        
        JPanel buttonPanel = new JPanel(new GridBagLayout()); // Use GridBagLayout for finer control
        buttonPanel.setBackground(new Color(25, 27, 44)); // Ensure the panel background matches the button
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; // Column 0
        gbc.gridy = 0; // Row 0
        gbc.insets = new Insets(30, 100, 0, 50); // Top, left, bottom, right padding (right padding effectively moves the button to the left)

        buttonPanel.add(updateMoodButton, gbc);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void applyRainbowColors(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.LIGHT_GRAY); // Set the background color to light gray
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(Color.DARK_GRAY);
        plot.setOutlineStroke(new BasicStroke(2.0f));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setCategoryLabelPositionOffset(10);
        domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
        domainAxis.setTickLabelFont(new Font("Arial", Font.BOLD, 12));
        domainAxis.setTickLabelPaint(Color.BLACK);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(true);
        renderer.setMaximumBarWidth(0.1); // Set bar width

        // Set the bar color to #191B2C
        Color barColor = new Color(7, 157, 247); // Lighter shade of the original dark blue
        renderer.setSeriesPaint(0, barColor); // Applying color to the series

        // Set this color for all series if multiple series exist
        for (int i = 0; i < dataset.getRowCount(); i++) {
            renderer.setSeriesPaint(i, barColor);
        }
    }

    private void initializeMoodMapping() {
        moodMapping = new HashMap<>();
        moodMapping.put("happy", "happy");
        moodMapping.put("joy", "happy");
        moodMapping.put("content", "happy");
        moodMapping.put("sad", "sad");
        moodMapping.put("unhappy", "sad");
        moodMapping.put("down", "sad");
        moodMapping.put("angry", "angry");
        moodMapping.put("mad", "angry");
        moodMapping.put("furious", "angry");
        // Add more mappings as needed
    }

    public void receiveMood(String mood) {
        if (mood != null && !mood.isEmpty()) {
            String detectedMood = APIClient.detectMood(mood).toLowerCase(); // Use AI to detect the mood from the input and convert to lower case
            String primaryMood = moodMapping.getOrDefault(detectedMood, detectedMood); // Map to primary mood category
            conversationContext.append(mood).append(" "); // Append to conversation context
            moodCounts.put(primaryMood, moodCounts.getOrDefault(primaryMood, 0) + 1);
            updateGraph();
            updateMoodEntries(userId, LocalDate.now());
        }
    }

    private void updateMoodAnalysis() {
        if (!moodCounts.isEmpty()) {
            // Display a comforting quote for the aggregated mood context
            displayComfortingQuote(conversationContext.toString().trim());
        }
    }

    private void displayComfortingQuote(String context) {
        String response = APIClient.getMoodQuote(context);
        JOptionPane.showMessageDialog(this, response, "Mood Tracker", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateGraph() {
        dataset.clear();
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            dataset.addValue(entry.getValue(), "Moods", entry.getKey());
        }
    }

    private void updateMoodEntries(int userId, LocalDate date) {
        List<LocalDate> entries = userMoodEntries.getOrDefault(userId, new ArrayList<>());
        if (!entries.contains(date)) {
            entries.add(date);
        }
        userMoodEntries.put(userId, entries);
    }
}

