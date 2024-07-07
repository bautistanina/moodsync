import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

//multithreading imports
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class MoodSyncApp {

    private JFrame frame;
    private JPanel chatPanel;
    private JTextField userInputField;
    private DefaultListModel<String> historyListModel;
    private JList<String> historyList;
    private JTextArea conversationContext;
    private String currentTitle;
    private ArrayList<String> chatHistory;
    private Map<String, ArrayList<String>> historyMap;
    private ImageIcon userIcon;
    private ImageIcon botIcon;
    private String username;
    private int userId;
    private int currentConversationId;
    private MoodTrackerPanel moodTrackerPanel;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private ChatHistoryDAO chatHistoryDAO = new ChatHistoryDAO();
    private Font montserratReg;
    private Font montserratBold;
    private JTabbedPane tabbedPane;  // Add this field

    private final String WELCOME_MESSAGE = "Hello! I'm MoodSync, your emotional assistant. How are you feeling today?";

    public MoodSyncApp(String username) {
        this.username = username;
        FlatDarkLaf.setup();
        chatHistory = new ArrayList<>();
        historyMap = new HashMap<>();
        historyListModel = new DefaultListModel<>();
        conversationContext = new JTextArea();
        userIcon = new ImageIcon("path/to/user_icon.jpg");
        botIcon = new ImageIcon("path/to/bot_icon.jpg");

        // Load fonts before UI components that might use them are initialized
        loadFonts();

        // Initialize the frame here
        frame = new JFrame("MoodSync App - Hi " + username + "!");

        // Call initializeUI after frame and font initialization
        initializeUI();

        // Retrieve the userId from the database based on the username
        this.userId = getUserIdByUsername(username);

        // Load chat history from the database for the user
        loadChatHistoryFromDatabase();
    }

    private void loadFonts() {
        try {
            montserratReg = Font.createFont(Font.TRUETYPE_FONT, new File("C:\\Users\\andre\\OneDrive\\Desktop\\MoodSyncApp\\lib\\Montserrat\\static\\Montserrat-Regular.ttf")).deriveFont(16f);
            montserratBold = Font.createFont(Font.TRUETYPE_FONT, new File("C:\\Users\\andre\\OneDrive\\Desktop\\MoodSyncApp\\lib\\Montserrat\\static\\Montserrat-Bold.ttf")).deriveFont(16f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(montserratReg);
            ge.registerFont(montserratBold);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            montserratReg = new Font("Arial", Font.PLAIN, 16); // Fallback font
            montserratBold = new Font("Arial", Font.BOLD, 16); // Fallback font
        }
    }

    private void initializeUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveCurrentConversation(currentTitle);
            }
        });
        userInputField = new JTextField();
        userInputField.setFont(montserratReg);
        JButton sendButton = new JButton("Send");
        sendButton.setFont(montserratReg);
        moodTrackerPanel = new MoodTrackerPanel(userId);
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(montserratReg.deriveFont(Font.BOLD, 14));
        tabbedPane.addTab("Mood Tracker", moodTrackerPanel);
        tabbedPane.addTab("Chatbot", createChatbotPanel(moodTrackerPanel));
        tabbedPane.addTab("Search", new SearchPanel(this)); // Add SearchPanel tab
        frame.add(tabbedPane, BorderLayout.CENTER);
        addLogoutButton();
        frame.setVisible(true);
        startNewConversation();
    }

    public List<String> searchMessages(String searchQuery) {
        List<String> results = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate searchDate = null;

        // Try to parse the search query as a date
        try {
            searchDate = LocalDate.parse(searchQuery, dateFormatter);
        } catch (Exception e) {
            // Not a date, proceed to search by message content
        }

        for (Map.Entry<String, ArrayList<String>> entry : historyMap.entrySet()) {
            String conversationTitle = entry.getKey();
            ArrayList<String> messages = entry.getValue();
            for (String message : messages) {
                if ((searchDate != null && message.contains(searchDate.toString())) ||
                    message.toLowerCase().contains(searchQuery.toLowerCase()) ||
                    conversationTitle.toLowerCase().contains(searchQuery.toLowerCase())) {

                    String formattedResult = "Title: " + conversationTitle + "\n" +
                                             "Message: " + message + "\n";
                    results.add(formattedResult);
                }
            }
        }
        return results;
    }
   public void highlightMessageInChatbot(String selectedResult, String searchTerm) {
    String[] parts = selectedResult.split("\n", 3);
    if (parts.length < 2) return;

    String selectedTitle = parts[0].replace("Title: ", "").trim();
    String messageToHighlight = parts[1].replace("Message: ", "").trim();

    historyList.setSelectedValue(selectedTitle, true); // Assuming historyList is your JList
    loadSelectedConversation();

    SwingUtilities.invokeLater(() -> {
        tabbedPane.setSelectedIndex(1); // Switch to the chatbot tab

        // Iterate through the chatPanel components to find and highlight the message
        for (Component component : chatPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel messagePanel = (JPanel) component;
                for (Component innerComponent : messagePanel.getComponents()) {
                    if (innerComponent instanceof JLabel) {
                        JLabel messageLabel = (JLabel) innerComponent;
                        if (messageLabel.getText().contains(messageToHighlight)) {
                            String highlightedText = highlightSearchTerm(messageLabel.getText(), searchTerm);
                            messageLabel.setText("<html>" + highlightedText + "</html>");

                            // Set background color to gray
                            messagePanel.setBackground(new Color(169, 169, 169)); // Gray background

                            // Focus on the highlighted text
                            messageLabel.requestFocusInWindow();

                            // Scroll to the highlighted message
                            scrollToMessage(messagePanel);

                            chatPanel.revalidate();
                            chatPanel.repaint();
                            return;
                        }
                    }
                }
            }
        }
    });
}

private void scrollToMessage(JPanel messagePanel) {
    SwingUtilities.invokeLater(() -> {
        JViewport viewport = (JViewport) chatPanel.getParent();
        Point point = messagePanel.getLocation();
        viewport.setViewPosition(point);
    });
}

private String highlightSearchTerm(String text, String term) {
    Pattern pattern = Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(text);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
        matcher.appendReplacement(sb, "<span style='background-color:gray;'>" + matcher.group() + "</span>");
    }
    matcher.appendTail(sb);

    return sb.toString();
}

    private void addLogoutButton() {

        Font montserratReg;
        try {
            montserratReg = Font.createFont(Font.TRUETYPE_FONT, new File("lib\\Montserrat\\static\\Montserrat-Regular.ttf")).deriveFont(30f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(montserratReg);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            montserratReg = new Font("Arial", Font.BOLD, 30); // Fallback font
        }
        float newRSize = 40f; // Change this value to resize the font after loading
        montserratReg = montserratReg.deriveFont(newRSize);
        
        Font montserratBold;
        try {
            montserratBold = Font.createFont(Font.TRUETYPE_FONT, new File("lib\\Montserrat\\static\\Montserrat-ExtraBold.ttf")).deriveFont(30f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(montserratBold);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            montserratBold = new Font("Arial", Font.BOLD, 30); // Fallback font
        }
        float newBSize = 40f; // Change this value to resize the font after loading
        montserratBold = montserratBold.deriveFont(newBSize);

        // Create a logout button with styling
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(montserratReg.deriveFont(Font.BOLD, 14));
        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCurrentConversation(currentTitle);  // Ensure this is called before closing
                frame.dispose();
                SwingUtilities.invokeLater(() -> new LoginRegistrationFrame());
            }
        });
        
    // Create a label for user greeting
    JLabel userGreeting = new JLabel("Welcome " + username + "!", SwingConstants.CENTER);
    userGreeting.setFont(montserratReg.deriveFont(Font.BOLD, 14));
    userGreeting.setForeground(Color.WHITE);  // Set text color to white
    userGreeting.setBorder(new EmptyBorder(10, 0, 10, 0)); // Margin top and bottom for the label

    // Configure layout and border for alignment
    JPanel logoutPanel = new JPanel();
    logoutPanel.setLayout(new BorderLayout());
    logoutPanel.add(userGreeting, BorderLayout.NORTH);
    logoutPanel.add(logoutButton, BorderLayout.SOUTH);
    logoutPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

    // Create a pop-up menu for the logout feature
    JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(logoutPanel);

    // Panel to hold the button that triggers the popup
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    buttonPanel.setOpaque(false);

    // Button to trigger the popup menu
    JButton dropUpButton = new JButton("    •••    ");
    dropUpButton.setFont(new Font("Arial", Font.BOLD, 14));
    dropUpButton.setMargin(new Insets(0, 0, 0, 0));
    dropUpButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (popupMenu.isVisible()) {
                popupMenu.setVisible(false);
            } else {
                // Show the popup menu at the specified position
                popupMenu.show(frame, 15, frame.getHeight() - popupMenu.getPreferredSize().height - 40);
            }
        }
    });

    buttonPanel.add(dropUpButton);

    // Adding to the main frame's south border but aligning left
    JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(buttonPanel, BorderLayout.WEST);
    frame.add(southPanel, BorderLayout.SOUTH);
}

public JPanel createChatbotPanel(MoodTrackerPanel moodTrackerPanel) { // Accept MoodTrackerPanel as parameter

    Font montserratReg;
        try {
            montserratReg = Font.createFont(Font.TRUETYPE_FONT, new File("lib\\Montserrat\\static\\Montserrat-Regular.ttf")).deriveFont(30f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(montserratReg);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            montserratReg = new Font("Arial", Font.BOLD, 30); // Fallback font
        }
        float newRSize = 40f; // Change this value to resize the font after loading
        montserratReg = montserratReg.deriveFont(newRSize);

    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBackground(Color.DARK_GRAY);

    chatPanel = new JPanel();
    chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
    chatPanel.setBackground(Color.DARK_GRAY);

    JScrollPane chatScrollPane = new JScrollPane(chatPanel);
    chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
    chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    mainPanel.add(chatScrollPane, BorderLayout.CENTER);

    JPanel inputPanel = new JPanel(new BorderLayout());
    inputPanel.setBackground(Color.DARK_GRAY);
    userInputField = new JTextField();
    userInputField.setFont(montserratReg.deriveFont(16f)); // Set to MontserratRegular
    userInputField.setForeground(Color.WHITE);
    JButton sendButton = new JButton("Send");
    sendButton.setFont(montserratReg.deriveFont(Font.BOLD, 16));
    sendButton.addActionListener(new SendButtonListener(frame, moodTrackerPanel, userInputField));


    inputPanel.add(userInputField, BorderLayout.CENTER);
    inputPanel.add(sendButton, BorderLayout.EAST);
    mainPanel.add(inputPanel, BorderLayout.SOUTH);

    historyList = new JList<>(historyListModel);
    historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    historyList.addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) {
            saveCurrentConversation(currentTitle);  // Pass the current title as the argument
            loadSelectedConversation();
        }
    });

    historyList.setCellRenderer(new CustomHistoryListRenderer());
    JScrollPane historyScrollPane = new JScrollPane(historyList);
    historyScrollPane.setBorder(BorderFactory.createTitledBorder("History"));
    historyScrollPane.setPreferredSize(new Dimension(250, 0));
    mainPanel.add(historyScrollPane, BorderLayout.WEST);

    JButton newChatButton = new JButton("New Chat");
    newChatButton.setFont(montserratReg.deriveFont(Font.BOLD, 16));
    newChatButton.addActionListener(e -> {
        saveCurrentConversation(currentTitle);  // Pass the current title as the argument
        startNewConversation();
    });

    JPanel newChatPanel = new JPanel(new BorderLayout());
    newChatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    newChatPanel.add(newChatButton, BorderLayout.CENTER);

    mainPanel.add(newChatPanel, BorderLayout.NORTH);

    addContextMenuToHistoryList();

    return mainPanel;
}


private void startNewConversation() {
    chatHistory.clear();
    chatPanel.removeAll();
    conversationContext.setText("");
    displayMessage("MoodSync Bot", WELCOME_MESSAGE, false);
    currentConversationId = (int) (System.currentTimeMillis() / 1000); // Generate a new conversation ID
    currentTitle = null; // Reset currentTitle to ensure it's set based on user input
}

    
    

private void loadSelectedConversation() {
    if (!historyList.isSelectionEmpty()) {
        String selectedTitle = historyList.getSelectedValue();
        ArrayList<String> selectedHistory = historyMap.get(selectedTitle);
        chatPanel.removeAll();
        chatHistory.clear();
        conversationContext.setText("");

        // Display the welcome message first
        displayMessage("MoodSync Bot", WELCOME_MESSAGE, false);
        conversationContext.append("MoodSync Bot: " + WELCOME_MESSAGE + "\n");

        if (selectedHistory != null) {
            for (String messageWithTimestamp : selectedHistory) {
                boolean isUser = messageWithTimestamp.startsWith("You: ");
                String displayedMessage = messageWithTimestamp;
                if (isUser) {
                    displayedMessage = messageWithTimestamp.substring(4).trim();
                } else if (messageWithTimestamp.startsWith("MoodSync Bot: ")) {
                    displayedMessage = messageWithTimestamp.substring(13).trim();
                } else {
                    displayedMessage = messageWithTimestamp;
                }
                displayMessage(isUser ? "You" : "MoodSync Bot", displayedMessage, isUser);
                conversationContext.append(messageWithTimestamp + "\n");
            }
        }
        chatPanel.revalidate();
        chatPanel.repaint();

        // Auto-focus to the latest chat message
        JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
}




public class SendButtonListener implements ActionListener {
    private boolean banned = false;
    private long banEndTime = 0;
    private Timer timer;
    private JFrame frame;
    private MoodTrackerPanel moodTrackerPanel;
    private JTextField userInputField;

    public SendButtonListener(JFrame frame, MoodTrackerPanel moodTrackerPanel, JTextField userInputField) {
    this.frame = frame;
    this.moodTrackerPanel = moodTrackerPanel;
    this.userInputField = userInputField;

    // Add ActionListener for Enter key press
    this.userInputField.addActionListener(e -> {
        actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Send"));
    });
}


    @Override
    public void actionPerformed(ActionEvent e) {
        if (banned && System.currentTimeMillis() < banEndTime) {
            JOptionPane.showMessageDialog(frame, getBannedMessage());
            return;
        }

        String userMessage = userInputField.getText().trim();
        if (!userMessage.isEmpty()) {
            executorService.submit(() -> {
                boolean containsBadWord = checkForBadWords(userMessage);
                if (containsBadWord) {
                    SwingUtilities.invokeLater(() -> {
                        displayMessage("MoodSync Bot", "Sorry, you used a banned word!", false);
                        banUser();
                    });
                    return;
                }

                if (currentTitle == null) {
                    CompletableFuture.supplyAsync(() ->
                        APIClient.generateTitleFromMessages(userMessage)
                    ).thenAccept(title -> {
                        currentTitle = title;
                        SwingUtilities.invokeLater(() -> {
                            historyListModel.addElement(currentTitle);
                            saveCurrentConversation(currentTitle);
                        });
                    });
                }

                SwingUtilities.invokeLater(() -> {
                    displayMessage("You", userMessage, true);
                    chatHistory.add("You: " + userMessage + " (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")");
                    userInputField.setText("");
                });

                String conversationContextText = conversationContext.getText() + " " + userMessage;
                String botResponse = APIClient.getChatbotResponse(conversationContextText);

                SwingUtilities.invokeLater(() -> {
                    displayMessage("MoodSync Bot", botResponse, false);
                    chatHistory.add("MoodSync Bot: " + botResponse + " (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")");
                    conversationContext.append("You: " + userMessage + "\nMoodSync Bot: " + botResponse + "\n");

                    JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                });

                saveChatMessageToDatabase(userId, "You: " + userMessage, currentConversationId);
                saveChatMessageToDatabase(userId, "MoodSync Bot: " + botResponse, currentConversationId);

                moodTrackerPanel.receiveMood(userMessage);
            });
        }
    }

        private void banUser() {
            banned = true;
            banEndTime = System.currentTimeMillis() + 30 * 1000; // 30 seconds ban
    
            JOptionPane.showMessageDialog(frame, getBannedMessage());
    
            timer = new Timer(1000, new ActionListener() {
                double remainingSeconds = 26.5;
    
                @Override
                public void actionPerformed(ActionEvent e) {
                    remainingSeconds--;
                    if (remainingSeconds > 0) {
                        JOptionPane.showMessageDialog(frame, getBannedMessage());
                    } else {
                        ((Timer) e.getSource()).stop();
                        banned = false;
                    }
                }
            });
            timer.start();
        }
    
        private String getBannedMessage() {
            int remainingSeconds = (int) Math.max(0, (banEndTime - System.currentTimeMillis()) / 1000);
            return "<html><div style='width: 200px;'><font color='red'><b><big>You are banned for "
                    + remainingSeconds + " seconds.</big></b></font></div></html>";
        }
    
        private boolean checkForBadWords(String message) {
            String[] badWords = {
                "arse", "arsehead", "arsehole", "asshole", "bastard", "bitch", "bloody", "bollocks", "brotherfucker",
                "bugger", "bullshit", "child-fucker", "cock", "cocksucker", "crap", "cunt", "dammit", "damn", "dick", "dick-head",
                "dickhead", "dumb ass", "dumb-ass", "dumbass", "dyke", "father-fucker", "fatherfucker", "frigger", "fuck", "fucker",
                "fucking", "god dammit", "God damn", "goddammit", "goddamn", "goddamned", "goddamnit", "godsdamn", "holy shit",
                "horseshit", "in shit", "jack-ass", "jackarse", "jackass", "Jesus Christ", "Jesus fuck", "Jesus H. Christ",
                "Jesus Harold Christ", "Jesus, Mary and Joseph", "Jesus wept", "kike", "mother fucker", "mother-fucker",
                "motherfucker", "nigga", "nigra", "pigfucker", "piss", "prick", "pussy", "shit", "shite", "sibling fucker",
                "sisterfuck", "sisterfucker", "slut", "son of a whore", "son of a bitch", "spastic", "twat", "wanker", "putangina mo",
                "puta", "gago", "tanga", "bobo"
            };
        
            for (String badWord : badWords) {
                if (message.matches("(?i).*\\b" + Pattern.quote(badWord) + "\\b.*")) {
                    return true;
                }
            }
            return false;
        }
    
        private void saveChatMessageToDatabase(int userId, String message, int conversationId) {
            ChatHistoryDAO chatHistoryDAO = new ChatHistoryDAO();
            try {
                LocalDateTime now = LocalDateTime.now();
                String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                chatHistoryDAO.saveChatMessage(userId, message, conversationId, timestamp);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        
    }
    public void startApplication() {
        loadChatHistoryAsync();  
    }
    
    private void loadChatHistoryAsync() {
        new SwingWorker<ArrayList<String>, Void>() {
            @Override
            protected ArrayList<String> doInBackground() throws Exception {
                
                return chatHistoryDAO.getChatHistory(userId, currentConversationId);
            }
    
            @Override
            protected void done() {
                try {
                    ArrayList<String> loadedChatHistory = get(); // This will automatically be ArrayList<String>
                    SwingUtilities.invokeLater(() -> {
                        if (loadedChatHistory != null) {
                            loadedChatHistory.forEach(message -> {
                                displayMessage("Past Conversation", message, false);
                            });
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
    
    
        

    private void displayMessage(String sender, String message, boolean isUser) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
    
        String timestamp;
        if (message.contains("(") && message.endsWith(")")) {
            int index = message.lastIndexOf('(');
            timestamp = message.substring(index + 1, message.length() - 1);
            message = message.substring(0, index).trim();
        } else {
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    
        JLabel messageLabel = new JLabel("<html><div style='width: auto; max-width: 250px;'><b style='color:darkblue;'>"
                + sender + "</b><br><small>" + timestamp + "</small><br>"
                + message.replaceAll("(\r\n|\n)", "<br>") + "</div></html>");
        messageLabel.setFont(montserratBold.deriveFont(14f)); // Set to MontserratBold for sender name
        messageLabel.setForeground(Color.WHITE);
    
        JPanel bubblePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int width = getWidth();
                int height = getHeight();
                Color color1 = isUser ? new Color(0, 153, 255) : new Color(204, 204, 204);
                Color color2 = Color.DARK_GRAY;
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, width - 1, height - 1, 30, 30);
                g2.dispose();
            }
    
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width = Math.min(dim.width, 250);
                return dim;
            }
        };
        bubblePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        bubblePanel.add(messageLabel, BorderLayout.CENTER);
    
        JPanel alignedMessagePanel = new JPanel(new BorderLayout());
        alignedMessagePanel.setOpaque(false);
    
        JPanel iconAndBubblePanel = new JPanel(new BorderLayout());
    
        JLabel userPicture = new JLabel("", JLabel.CENTER);
        userPicture.setIcon(new ImageIcon(getScaledRoundImage(isUser ? userIcon.getImage() : botIcon.getImage(), 40)));
    
        if (isUser) {
            iconAndBubblePanel.add(userPicture, BorderLayout.WEST);
            iconAndBubblePanel.add(bubblePanel, BorderLayout.CENTER);
            alignedMessagePanel.add(iconAndBubblePanel, BorderLayout.WEST);
        } else {
            iconAndBubblePanel.add(bubblePanel, BorderLayout.CENTER);
            iconAndBubblePanel.add(userPicture, BorderLayout.EAST);
            alignedMessagePanel.add(iconAndBubblePanel, BorderLayout.EAST);
        }
    
        chatPanel.add(alignedMessagePanel);
        chatPanel.add(Box.createVerticalStrut(5));
        chatPanel.revalidate();
        chatPanel.repaint();
    
        JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
    
    
    
    private Image getScaledRoundImage(Image srcImg, int size) {
        BufferedImage resizedImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new RoundRectangle2D.Double(0, 0, size, size, size, size));
        g2.drawImage(srcImg, 0, 0, size, size, null);
        g2.dispose();
        return resizedImg;
    }

    private void saveCurrentConversation(String title) {
        if (!chatHistory.isEmpty() && title != null && !title.isEmpty()) {
            if (currentTitle == null) {
                currentTitle = title;
            }
            historyMap.put(currentTitle, new ArrayList<>(chatHistory));
            if (!historyListModel.contains(currentTitle)) {
                historyListModel.addElement(currentTitle);
            }
    
            // Save title to database
            ChatHistoryDAO chatHistoryDAO = new ChatHistoryDAO();
            try {
                chatHistoryDAO.saveGeneratedTitle(currentConversationId, currentTitle);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    
    

    public String generateTitleFromMessage(String message) {
        StringTokenizer tokenizer = new StringTokenizer(message, " ");
        StringBuilder titleBuilder = new StringBuilder();
        int wordCount = 0;
    
        while (tokenizer.hasMoreTokens() && wordCount < 5) { // Ensure you do not exceed 5 words
            titleBuilder.append(tokenizer.nextToken()).append(" ");
            wordCount++;
        }
    
        return titleBuilder.toString().trim();
    }
    
    public class CustomHistoryListRenderer extends DefaultListCellRenderer {
        @Override
       
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(10, 10, 10, 10));
            label.setFont(montserratBold.deriveFont(14f)); // Set to MontserratBold
            label.setForeground(Color.WHITE);
            label.setBackground(isSelected ? Color.GRAY : Color.DARK_GRAY);
            return label;
        }
    }
    

    private void addContextMenuToHistoryList() {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem deleteItem = new JMenuItem("Delete");

        renameItem.addActionListener(e -> renameSelectedConversation());
        deleteItem.addActionListener(e -> deleteSelectedConversation());

        contextMenu.add(renameItem);
        contextMenu.add(deleteItem);

        historyList.setComponentPopupMenu(contextMenu);
    }

    private void renameSelectedConversation() {
        String selectedTitle = historyList.getSelectedValue();
        if (selectedTitle != null) {
            String newTitle = JOptionPane.showInputDialog(frame, "Enter new title:", selectedTitle);
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                ArrayList<String> conversation = historyMap.remove(selectedTitle);
                historyMap.put(newTitle, conversation);
                historyListModel.setElementAt(newTitle, historyList.getSelectedIndex());
            }
        }
    }

    private void deleteSelectedConversation() {
        String selectedTitle = historyList.getSelectedValue();
        if (selectedTitle != null) {
            int response = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this conversation?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                historyMap.remove(selectedTitle);
                historyListModel.removeElement(selectedTitle);
            }
        }
    }

    private int getUserIdByUsername(String username) {
        UserDAO userDAO = new UserDAO();
        return userDAO.getUserIdByUsername(username);
    }

    private void loadChatHistoryFromDatabase() {
        ChatHistoryDAO chatHistoryDAO = new ChatHistoryDAO();
        try {
            List<Integer> conversationIds = chatHistoryDAO.getUserConversationIds(userId);
            for (int conversationId : conversationIds) {
                ArrayList<String> loadedChatHistory = chatHistoryDAO.getChatHistory(userId, conversationId);
                String title = chatHistoryDAO.getGeneratedTitle(conversationId);
                if (loadedChatHistory != null && !loadedChatHistory.isEmpty()) {
                    String finalTitle = title != null ? title : generateTitleFromMessage(loadedChatHistory.get(0));
                    historyListModel.addElement(finalTitle);
                    historyMap.put(finalTitle, new ArrayList<>(loadedChatHistory));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginRegistrationFrame());
    }
}
