import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginRegistrationFrame {
    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginRegistrationFrame() {
        initializeUI();
    }

    private void initializeUI() {
        // Use FlatLaf for modern look and feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

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
        
        frame = new JFrame("MoodSync Login/Register");
        frame.setFont(montserratBold);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // Adjusted width
        frame.setLocationRelativeTo(null); // Center the frame on screen
        frame.setLayout(new BorderLayout());
        frame.setResizable(false); // Fixed size

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.decode("#191B2C"));

        JLabel titleLabel = new JLabel("MOOD SYNC BOT", SwingConstants.CENTER);
        titleLabel.setFont(montserratBold.deriveFont(Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Load and display the GIF image
        try {
            File imgFile = new File("moodsynclogo.gif"); // Change to your image path
            if (imgFile.exists()) {
                ImageIcon imageIcon = new ImageIcon(imgFile.getAbsolutePath());
                JLabel imageLabel = new JLabel(imageIcon);
                JPanel imagePanel = new JPanel(new BorderLayout());
                imagePanel.setBackground(Color.decode("#191B2C"));
                imagePanel.add(imageLabel, BorderLayout.CENTER);
                mainPanel.add(imagePanel, BorderLayout.WEST);
        }
        }    catch (Exception ex) {
            ex.printStackTrace();
        }

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.decode("#191B2C"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);


        JLabel usernameLabel = new JLabel("Username:", SwingConstants.LEFT);
        usernameLabel.setFont(montserratReg.deriveFont(Font.BOLD, 14));
        usernameLabel.setForeground(Color.WHITE);
        usernameField = new RoundedTextField("Enter Username");
        addPlaceholderStyle(usernameField);
        centerPanel.add(usernameLabel, gbc);
        centerPanel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:", SwingConstants.LEFT);
        passwordLabel.setFont(montserratReg.deriveFont(Font.BOLD, 14));
        passwordLabel.setForeground(Color.WHITE);
        passwordField = new RoundedPasswordField("Enter Password");
        addPlaceholderStyle(passwordField);
        centerPanel.add(passwordLabel, gbc);
        centerPanel.add(passwordField, gbc);

        JCheckBox showPassword = new JCheckBox("Show Password");
        showPassword.setFont(montserratReg.deriveFont(Font.BOLD, 12));
        showPassword.setForeground(Color.WHITE);
        showPassword.setBackground(Color.decode("#191B2C"));
        showPassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showPassword.isSelected()) {
                    passwordField.setEchoChar((char) 0);
                } else {
                    passwordField.setEchoChar('•');
                }
            }
        });

        centerPanel.add(showPassword, gbc);

        JButton loginButton = new RoundedButton("Login");
        loginButton.setFont(montserratReg.deriveFont(Font.BOLD, 14));
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        JButton registerButton = new RoundedButton("Register");
        registerButton.setFont(montserratReg.deriveFont(Font.BOLD, 14));
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                register();
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.decode("#191B2C"));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        centerPanel.add(buttonPanel, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);

        // Add KeyListener for Enter key
        Action loginAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        };

        usernameField.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);
    }

    private void login() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (isInputInvalid(username, password)) {
            JOptionPane.showMessageDialog(frame, "Please enter both username and password", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (authenticateUser(username, new String(password))) {
            JOptionPane.showMessageDialog(frame, "Login Successful");
            frame.dispose();
            new MoodSyncApp(username);
        } else {
            JOptionPane.showMessageDialog(frame, "Invalid username or password", "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void register() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (isInputInvalid(username, password)) {
            JOptionPane.showMessageDialog(frame, "Please enter both username and password", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (isUsernameTaken(username)) {
            JOptionPane.showMessageDialog(frame, "Username is already taken", "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (registerUser(username, new String(password))) {
            JOptionPane.showMessageDialog(frame, "Registration Successful");
        } else {
            JOptionPane.showMessageDialog(frame, "Registration Failed. Please try again.", "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isInputInvalid(String username, char[] password) {
        return username.isEmpty() || password.length == 0 || username.equals("Enter Username") || new String(password).equals("Enter Password");
    }

    private boolean isUsernameTaken(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean authenticateUser(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerUser(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addPlaceholderStyle(JTextField textField) {
        textField.setForeground(Color.GRAY); // Placeholder text color
        textField.setFont(new Font("Arial", Font.ITALIC, 14)); // Italic font for placeholder
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals("Enter Username") || textField.getText().equals("Enter Password")) {
                    textField.setText("");
                    textField.setForeground(Color.WHITE); // Actual text color
                    textField.setFont(new Font("Arial", Font.PLAIN, 14)); // Normal font for actual text
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setForeground(Color.GRAY); // Placeholder text color
                    textField.setFont(new Font("Arial", Font.ITALIC, 14)); // Italic font for placeholder
                    if (textField instanceof JPasswordField) {
                        textField.setText("Enter Password");
                    } else {
                        textField.setText("Enter Username");
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginRegistrationFrame::new);
    }
}

class RoundedTextField extends JTextField {
    private Shape shape;

    public RoundedTextField(String placeholder) {
        super(placeholder);
        setOpaque(false);
        setForeground(Color.GRAY);
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        setFont(new Font("Arial", Font.ITALIC, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, Color.DARK_GRAY, getWidth(), getHeight(), Color.GRAY);
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.GRAY);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        g2.dispose();
    }

    @Override
    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        }
        return shape.contains(x, y);
    }
}

class RoundedPasswordField extends JPasswordField {
    private Shape shape;

    public RoundedPasswordField(String placeholder) {
        super(placeholder);
        setOpaque(false);
        setForeground(Color.GRAY);
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        setFont(new Font("Arial", Font.ITALIC, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, Color.DARK_GRAY, getWidth(), getHeight(), Color.GRAY);
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.GRAY);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        g2.dispose();
    }

    @Override
    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        }
        return shape.contains(x, y);
    }
}

class RoundedButton extends JButton {
    public RoundedButton(String text) {
        super(text);
        setUI(new BasicButtonUI());
        setContentAreaFilled(false);
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        setFont(new Font("Arial", Font.BOLD, 14));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
                setBackground(Color.decode("#31364D"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setForeground(Color.WHITE);
                setBackground(null);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getModel().isPressed()) {
            g.setColor(Color.decode("#262A3F"));
        } else {
            g.setColor(getBackground());
        }
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        g.setColor(Color.GRAY);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
    }
}


class DatabaseConnection {
    public static Connection getConnection() throws SQLException {
                return null;
    }
}
