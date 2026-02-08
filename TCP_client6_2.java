package tcp_client6;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TCP_client6_2 extends JFrame {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final String BACKUP_DIR = "client_backups/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private PrintWriter backupWriter;
    private String backupFile;
    // GUI Components
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JButton recoverButton;
    private JLabel statusLabel; 
    // Client identifier
    private String clientName = "Client2"; // Different name 
    public TCP_client6_2() {
        setTitle("Secure TCP Chat Client - " + clientName);
        createGUI();
        setupBackupSystem();
    }  
    private void createGUI() {
        setTitle("Secure TCP Chat Client - " + clientName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLayout(new BorderLayout()); 
        // Use different colors for Client 2
        Color client2Color = new Color(230, 240, 255); 
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(client2Color);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER); 
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Disconnected - " + clientName);
        statusLabel.setForeground(Color.RED);
        statusPanel.add(statusLabel); 
        connectButton = new JButton("Connect");
        recoverButton = new JButton("Recover");
        statusPanel.add(connectButton);
        statusPanel.add(recoverButton);
        add(statusPanel, BorderLayout.NORTH);
        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setEnabled(false);
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.setBackground(new Color(100, 150, 255)); // Different color 
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH); 
        // Event listeners
        connectButton.addActionListener(e -> connectToServer());
        recoverButton.addActionListener(e -> recoverConversations());
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage()); 
        setVisible(true);
    }
    private void setupBackupSystem() {
        new File(BACKUP_DIR).mkdirs();
        backupFile = BACKUP_DIR + clientName + "_" + DATE_FORMAT.format(new Date()) + ".txt";
        try {
            backupWriter = new PrintWriter(new FileWriter(backupFile, true));
            logToBackup("=== " + clientName + " STARTED: " + new Date() + " ===");
            appendToChat("Backup file: " + backupFile);
        } catch (IOException e) {
            showError("Failed to create backup file: " + e.getMessage());
        }
    }
    private void logToBackup(String message) {
        if (backupWriter != null) {
            backupWriter.println(message);
            backupWriter.flush();
        }
    }
    private void connectToServer() {
        new Thread(() -> {
            try {
                appendToChat("Connecting to server...");
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);          
                // Send client name identification
                out.println("!name|" + clientName + "|" + hash(clientName));          
                // Enable UI
                SwingUtilities.invokeLater(() -> {
                    inputField.setEnabled(true);
                    sendButton.setEnabled(true);
                    connectButton.setEnabled(false);
                    statusLabel.setText("Connected as " + clientName);
                    statusLabel.setForeground(new Color(0, 150, 0)); // Green
                    appendToChat("Connected as: " + clientName);
                });         
                logToBackup(clientName + " connected to server at " + new Date());         
                // Start message receiver thread
                new Thread(this::receiveMessages).start();
            } catch (IOException e) {
                showError("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;
        inputField.setText("");     
        // Hash and send
        String hashed = hash(message);
         String toSend = message + "|" + hashed;     
        out.println(toSend);
        // Display locally with different color indicator
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String displayMsg = timestamp + " [You as " + clientName + "]: " + message;
        appendToChat(displayMsg);
        logToBackup(timestamp + " [Sent Hash]: " + hashed);    
        if (message.equalsIgnoreCase("!exit")) {
            disconnect();
        }
    }
    private void receiveMessages() {
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                final String response = serverResponse;
                SwingUtilities.invokeLater(() -> {
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());    
                    if (response.startsWith("ACK|")) {
                        // Message acknowledged and verified
                        String[] parts = response.split("\\|", 3);
                        if (parts.length >= 3) {
                            appendToChat(timestamp + " [✓]: Message delivered at " + parts[1]);
                        }
                    } else if (response.startsWith("ERROR|")) {
                        // Error from server
                        appendToChat(timestamp + " [✗ Error]: " + response.substring(6));
                        logToBackup("ERROR: " + response);
                    } else if (response.contains("|")) {
                        // Regular message with verification
                        String[] parts = response.split("\\|", 2);
                        String message = parts[0];
                        String receivedHash = parts[1];   
                        // Verify message
                        String calculatedHash = hash(message);
                        if (calculatedHash.equals(receivedHash)) {
                            // Color code different clients
                            if (message.contains("[Client1]:")) {
                                appendToChat(timestamp + " [Client1]: " + 
                                          message.substring(message.indexOf("]:") + 2) + " ✓");
                            } else if (message.contains("[Client2]:")) {
                                appendToChat(timestamp + " [Client2]: " + 
                                          message.substring(message.indexOf("]:") + 2) + " ✓");
                            } else {
                                appendToChat(timestamp + " [Other]: " + message + " ✓");
                            }
                            logToBackup(timestamp + " [Received Hash Verified]: " + receivedHash);
                        } else {
                            appendToChat(timestamp + " [Security Alert]: Message integrity check failed!");
                            logToBackup("SECURITY ALERT: Hash mismatch in received message");
                        }
                    } else {
                        if (response.contains("Welcome")) {
                            appendToChat("✓ " + response);
                        } else {
                            appendToChat(timestamp + " [Server]: " + response);
                        }
                        logToBackup(timestamp + " [Server]: " + response);
                    }
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                appendToChat("Disconnected from server.");
                statusLabel.setText("Disconnected - " + clientName);
                statusLabel.setForeground(Color.RED);
                inputField.setEnabled(false);
                sendButton.setEnabled(false);
                connectButton.setEnabled(true);
            });
        }
    }  
    private void recoverConversations() {
        File backupDir = new File(BACKUP_DIR);
        File[] backupFiles = backupDir.listFiles((dir, name) -> 
            name.startsWith("Client") && name.endsWith(".txt"));      
        if (backupFiles == null || backupFiles.length == 0) {
            showMessage("No backup files found.");
            return;
        }      
        // Create recovery dialog
        JDialog recoveryDialog = new JDialog(this, "Recover Conversations - " + clientName, true);
        recoveryDialog.setSize(500, 400);
        recoveryDialog.setLayout(new BorderLayout());      
        JTextArea recoveryArea = new JTextArea();
        recoveryArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(recoveryArea);      
        // Load all backup files
        StringBuilder recoveryText = new StringBuilder();
        recoveryText.append("=== " + clientName + " - RECOVERED CONVERSATIONS ===\n\n");      
        java.util.List<File> sortedFiles = Arrays.asList(backupFiles);
        sortedFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));      
        for (File file : sortedFiles) {
            recoveryText.append("File: ").append(file.getName()).append("\n");
            recoveryText.append("Last modified: ").append(new Date(file.lastModified())).append("\n");
            recoveryText.append("-".repeat(50)).append("\n");       
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    recoveryText.append(line).append("\n");
                }
            } catch (IOException e) {
                recoveryText.append("Error reading file: ").append(e.getMessage()).append("\n");
            }
            recoveryText.append("\n");
        }
        recoveryArea.setText(recoveryText.toString());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> recoveryDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        recoveryDialog.add(scrollPane, BorderLayout.CENTER);
        recoveryDialog.add(buttonPanel, BorderLayout.SOUTH);
        recoveryDialog.setLocationRelativeTo(this);
        recoveryDialog.setVisible(true);
    }
    private void disconnect() {
        try {
            if (out != null) {
                out.println("exit");
                out.close();
            }
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (backupWriter != null) backupWriter.close();
            
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Disconnected - " + clientName);
                statusLabel.setForeground(Color.RED);
                inputField.setEnabled(false);
                sendButton.setEnabled(false);
                connectButton.setEnabled(true);
                appendToChat("Disconnected from server.");
            });
            
        } catch (IOException e) {
            showError("Error disconnecting: " + e.getMessage());
        }
    }
    private void appendToChat(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    private void showError(String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, error, clientName + " - Error", 
                                         JOptionPane.ERROR_MESSAGE);
            appendToChat("[Error]: " + error);
        });
    }
    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, clientName + " - Information", 
                                         JOptionPane.INFORMATION_MESSAGE);
        });
    }
    // Static hash method 
    static String hash(String msg) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(msg.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "ERROR_HASH";
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TCP_client6_2();
        });
    }
}