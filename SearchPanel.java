import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchPanel extends JPanel {
    private JTextField searchField;
    private JList<String> searchResultsList;
    private DefaultListModel<String> searchResultsListModel;
    private MoodSyncApp moodSyncApp;
    private Font montserratReg;
    private Font montserratBold;

    public SearchPanel(MoodSyncApp moodSyncApp) {
        this.moodSyncApp = moodSyncApp;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Load Montserrat fonts
        try {
            montserratReg = Font.createFont(Font.TRUETYPE_FONT, new File("lib\\Montserrat\\static\\Montserrat-Regular.ttf")).deriveFont(16f);
            montserratBold = Font.createFont(Font.TRUETYPE_FONT, new File("lib\\Montserrat\\static\\Montserrat-Bold.ttf")).deriveFont(16f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(montserratReg);
            ge.registerFont(montserratBold);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            montserratReg = new Font("Arial", Font.PLAIN, 16); // Fallback font
            montserratBold = new Font("Arial", Font.BOLD, 16); // Fallback font
        }

        searchField = new JTextField();
        searchField.setFont(montserratReg);
        JButton searchButton = new JButton("Search");
        searchButton.setFont(montserratBold);

        JPanel searchInputPanel = new JPanel(new BorderLayout());
        searchInputPanel.add(searchField, BorderLayout.CENTER);
        searchInputPanel.add(searchButton, BorderLayout.EAST);

        searchResultsListModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsListModel);
        searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultsList.setCellRenderer(new CustomSearchResultRenderer());

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchMessages();
            }
        });

        searchResultsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                highlightSelectedMessage();
            }
        });

        add(searchInputPanel, BorderLayout.NORTH);
        add(new JScrollPane(searchResultsList), BorderLayout.CENTER);
    }

    private void searchMessages() {
        String searchQuery = searchField.getText().trim();
        searchResultsListModel.clear();

        if (!searchQuery.isEmpty()) {
            List<String> results = moodSyncApp.searchMessages(searchQuery);
            for (String result : results) {
                searchResultsListModel.addElement(result);
            }
        }
    }

    private void highlightSelectedMessage() {
        String selectedResult = searchResultsList.getSelectedValue();
        if (selectedResult != null) {
            moodSyncApp.highlightMessageInChatbot(selectedResult, searchField.getText().trim());
        }
    }

    private class CustomSearchResultRenderer extends JPanel implements ListCellRenderer<String> {
        private JLabel titleLabel;
        private JLabel messageLabel;
    
        public CustomSearchResultRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 10, 10, 10));
            titleLabel = new JLabel();
            titleLabel.setFont(montserratBold.deriveFont(16f));
            titleLabel.setForeground(Color.BLACK);  // Set title text color to black
            messageLabel = new JLabel();
            messageLabel.setFont(montserratBold.deriveFont(14f)); // Set message font to bold
            messageLabel.setForeground(Color.WHITE);  // Set message text color to white
            messageLabel.setVerticalAlignment(SwingConstants.TOP);
            add(titleLabel, BorderLayout.NORTH);
            add(messageLabel, BorderLayout.CENTER);
        }
    
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            String[] parts = value.split("\n", 3);
            if (parts.length >= 2) {
                titleLabel.setText(parts[0]);
                String messageContent = parts[1].replace("Based Message:", "").trim() + "<br>" + parts[2].replace("Based Message:", "").trim();
                messageLabel.setText("<html>" + highlightSearchTerm(messageContent, searchField.getText().trim()) + "</html>");
                
            }
    
            boolean isBotMessage = value.contains("MoodSync Bot:"); // Adjust this check as necessary
    
            if (isSelected) {
                setBackground(new Color(169, 169, 169)); // Gray for selected
            } else {
                setBackground(Color.DARK_GRAY);
            }
    
            // Set the bubble color
            setBubbleBackground(isBotMessage);
    
            return this;
        }
    
        private String highlightSearchTerm(String text, String term) {
            Pattern pattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            StringBuffer sb = new StringBuffer();
    
            while (matcher.find()) {
                matcher.appendReplacement(sb, "<span style='background-color:gray;'>" + matcher.group() + "</span>");
            }
            matcher.appendTail(sb);
    
            return sb.toString();
        }
    
        private void setBubbleBackground(boolean isBotMessage) {
            setOpaque(false);
            this.isBotMessage = isBotMessage;
        }
    
        private boolean isBotMessage;
    
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int width = getWidth();
            int height = getHeight();
            Color color1 = isBotMessage ? new Color(200, 200, 200) : new Color(0, 153, 255);
            Color color2 = isBotMessage ? new Color(169, 169, 169) : new Color(0, 102, 204); // Adjust the gradient colors for bot messages
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, width - 1, height - 1, 30, 30);
            g2.setColor(Color.GRAY);
            g2.drawRoundRect(0, 0, width - 1, height - 1, 30, 30);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}    