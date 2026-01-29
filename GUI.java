import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class GUI {

    static String[] Selection = new String[0];
    static JFrame frame;

    // GUI's own Node
    static Node node;

    // GUI STARTS HERE
    public static void start() {

        // Start the GUI's private Node in background
        node = new Node();
        node.location = "Anonymous";
        node.start();  // non-blocking because listener runs in its own thread

        SwingUtilities.invokeLater(GUI::show);
    }

    private static void show() {
        // ====== PASSWORD FIRST ======
        String password = JOptionPane.showInputDialog(
                null,
                "Enter Password to Continue:",
                "Authentication Required",
                JOptionPane.PLAIN_MESSAGE
        );

        if (password == null || !password.equals("letmein")) {
            JOptionPane.showMessageDialog(null,
                    "Incorrect Password. Exiting.",
                    "Access Denied",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        // ====== MAIN FRAME ======
        frame = new JFrame("Grid + Function Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1600, 700);
        frame.setLayout(new BorderLayout());

        // ====== GRID PANEL ======
        JPanel gridPanel = new JPanel(new GridLayout(4, 15, 10, 10));
        gridPanel.setBackground(Color.BLACK);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        RoundedButton[][] Grid = new RoundedButton[4][15];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 15; j++) {
                String btnText = "R" + (i + 1) + "C" + (j + 1);
                RoundedButton btn = new RoundedButton(btnText);

                btn.addActionListener(e -> {
                    Color red = new Color(200, 0, 0);
                    Color green = new Color(0, 180, 0);
                    boolean turningGreen = btn.getBackground().equals(red);

                    // Toggle color
                    btn.setBackground(turningGreen ? green : red);

                    // Update Selection[]
                    if (turningGreen) addToSelection(btn.getText());
                    else removeFromSelection(btn.getText());
                });

                gridPanel.add(btn);
                Grid[i][j] = btn;
            }
        }

        // ====== FUNCTION PANEL ======
        JPanel functionPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        functionPanel.setBackground(Color.DARK_GRAY);
        functionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] functions = {"Broadcast", "Send File", "Run Command in Terminal"};

        for (String f : functions) {
            JButton funcBtn = new JButton(f);
            funcBtn.setFont(new Font("Arial", Font.BOLD, 16));
            funcBtn.setBackground(new Color(80, 80, 80));
            funcBtn.setForeground(Color.WHITE);
            funcBtn.setFocusPainted(false);
            funcBtn.addActionListener(e -> handleFunction(f));
            functionPanel.add(funcBtn);
        }

        JScrollPane scrollPane = new JScrollPane(functionPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);
        scrollPane.setPreferredSize(new Dimension(100, 200));

        // ====== ADD TO FRAME ======
        frame.add(gridPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void handleFunction(String functionName) {
        switch (functionName) {
            case "Broadcast":
                Broadcast();
                break;
            case "Send File":
                sendFile();
                break;
            case "Run Command in Terminal":
                runTerminalCommand();
                break;
        }
    }

    private static void Broadcast() {
        if (Selection.length == 0) {
            JOptionPane.showMessageDialog(frame, "No computers selected!");
            return;
        }

        String msg = JOptionPane.showInputDialog(frame, "Enter broadcast message:", "Broadcast",
                JOptionPane.PLAIN_MESSAGE);
        if (msg == null || msg.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Message cannot be empty.");
            return;
        }

        for (String computer : Selection) {
            node.sendMessageToServer(computer, msg);
        }
    }

    private static void sendFile() {
        if (Selection.length == 0) {
            JOptionPane.showMessageDialog(frame, "No computers selected!");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();

        for (String computer : Selection) {
            try {
                node.sendFileToServer(computer, file);
                JOptionPane.showMessageDialog(frame, "File sent to " + computer);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Failed to send file to " + computer + ": " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void runTerminalCommand() {
        if (Selection.length == 0) {
            JOptionPane.showMessageDialog(frame, "No computers selected!");
            return;
        }

        String cmd = JOptionPane.showInputDialog(frame, "Enter terminal command to run:",
                "Run Command", JOptionPane.PLAIN_MESSAGE);
        if (cmd == null || cmd.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Command cannot be empty.");
            return;
        }

        for (String computer : Selection) {
            node.sendMessageToServer(computer, "CMD|" + cmd);
        }

        JOptionPane.showMessageDialog(frame, "Command sent to selected computers.");
    }

    private static void addToSelection(String text) {
        for (String s : Selection) if (s.equals(text)) return;
        String[] newArr = new String[Selection.length + 1];
        System.arraycopy(Selection, 0, newArr, 0, Selection.length);
        newArr[Selection.length] = text;
        Selection = newArr;
    }

    private static void removeFromSelection(String text) {
        int count = 0;
        for (String s : Selection) if (!s.equals(text)) count++;
        String[] newArr = new String[count];
        int idx = 0;
        for (String s : Selection) if (!s.equals(text)) newArr[idx++] = s;
        Selection = newArr;
    }
}
