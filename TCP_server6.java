package tcp_server6;

import java.io.*;
import java.net.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class TCP_server6 {
    
    private static final int PORT = 5000;
    private static final String BACKUP_DIR = "backups/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static PrintWriter backupWriter;
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    private static List<ClientHandler> activeClients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        System.out.println("Secure Chat Server Starting...");
        System.out.println("Listening on port: " + PORT);
        // Create backup directory
        new File(BACKUP_DIR).mkdirs();
        // Create timestamped backup file
        String backupFile = BACKUP_DIR + "chat_" + DATE_FORMAT.format(new Date()) + ".txt";
        backupWriter = new PrintWriter(new FileWriter(backupFile, true));
        logToBackup("=== SERVER STARTED: " + new Date() + " ===");
        System.out.println("Backup file: " + backupFile);
        // Load recovery if available
        if (args.length > 0 && args[0].equals("-recover")) {
            recoverPreviousConversations();
        }
        ServerSocket serverSocket = new ServerSocket(PORT);
        startBackupRotation();
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                activeClients.add(clientHandler);
                threadPool.execute(clientHandler);
                System.out.println("New client connected. Active clients: " + activeClients.size());
            } catch (IOException e) {
                System.err.println("Error accepting client: " + e.getMessage());
            }
        }
    }
    private static void logToBackup(String message) {
        synchronized (backupWriter) {
            backupWriter.println(message);
            backupWriter.flush();
        }
    }
    private static void startBackupRotation() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String newBackupFile = BACKUP_DIR + "chat_" + DATE_FORMAT.format(new Date()) + ".txt";
                    synchronized (backupWriter) {
                        backupWriter.close();
                        backupWriter = new PrintWriter(new FileWriter(newBackupFile, true));
                        logToBackup("=== NEW DAILY BACKUP STARTED: " + new Date() + " ===");
                        System.out.println("Rotated to new backup file: " + newBackupFile);
                    }
                } catch (IOException e) {
                    System.err.println("Error rotating backup: " + e.getMessage());
                }
            }
        }, getNextMidnight(), 24 * 60 * 60 * 1000); // Daily at midnight
    }
    private static long getNextMidnight() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() - System.currentTimeMillis();
    }
    private static void recoverPreviousConversations() {
        File backupDir = new File(BACKUP_DIR);
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("chat_") && name.endsWith(".txt"));
        if (backupFiles != null && backupFiles.length > 0) {
            System.out.println("=== RECOVERY MODE ===");
            System.out.println("Found " + backupFiles.length + " backup file(s):");
            for (File file : backupFiles) {
                System.out.println("\nRecovering from: " + file.getName());
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("  " + line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading backup: " + e.getMessage());
                }
            }
            System.out.println("=== END RECOVERY ===");
        }
    } 
    private static void broadcastMessage(String message, ClientHandler sender) {
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                if (client != sender && client.isConnected()) {
                    client.sendMessage(message);
                }
            }
        }
    } 
    // Inner class for handling individual clients
    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientId;
        private boolean connected = true;   
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientId = "Client_" + socket.getPort(); // Default ID
        }
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String firstInput = in.readLine();
                if (firstInput != null && firstInput.startsWith("!name|")) {
                    String[] nameParts = firstInput.split("\\|", 3);
                    if (nameParts.length == 3) {
                        // Verify name hash
                        if (hash(nameParts[1]).equals(nameParts[2])) {
                            clientId = nameParts[1];
                            System.out.println("Client identified as: " + clientId);
                        }
                    }
                    out.println("Welcome to Secure Chat Server! Your ID: " + clientId);
                } else {
                    out.println("Welcome to Secure Chat Server! Your ID: " + clientId);
                    // Handle the first message I already read
                    if (firstInput != null) {
                        processMessage(firstInput);
                    }
                }
                String inputLine;
                while ((inputLine = in.readLine()) != null && connected) {
                    processMessage(inputLine);
                } 
            } catch (IOException e) {
                System.err.println("Error with client " + clientId + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        } 
        // Helper method to process messages
        private void processMessage(String inputLine) throws IOException {
            if (inputLine.equalsIgnoreCase("!exit")) {
                connected = false;
                return;
            }
            // Verify message format: message|hash
            String[] parts = inputLine.split("\\|", 2);
            if (parts.length == 2) {
                String message = parts[0];
                String receivedHash = parts[1];
                // Verify integrity
                String calculatedHash = hash(message);
                if (calculatedHash.equals(receivedHash)) {
                    // Message is valid
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    String logMessage = timestamp + " [" + clientId + "]: " + message + " ✓";
                    System.out.println(logMessage);
                    logToBackup(logMessage);
                    // Echo back with verification
                    out.println("ACK|" + timestamp + "|Message received and verified");
                    // Broadcast to other clients
                    broadcastMessage(message + "|" + receivedHash, this);
                } else {
                    // Hash mismatch - possible tampering
                    String errorMsg = "ERROR|Hash verification failed - possible tampering";
                    System.err.println("Integrity check failed from " + clientId);
                    logToBackup("SECURITY ALERT: Hash mismatch from " + clientId);
                    out.println(errorMsg);
                }
            } else {
                // Invalid format
                out.println("ERROR|Invalid message format");
            }
        } 
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
        public boolean isConnected() {
            return connected;
        }
        private void disconnect() {
            connected = false;
            activeClients.remove(this);
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client connection: " + e.getMessage());
            }
            System.out.println(clientId + " disconnected. Active clients: " + activeClients.size());
            logToBackup(clientId + " disconnected at " + new Date());
        }
    }
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
            System.err.println("SHA-256 algorithm not available");
            return "";
        }
    }
}