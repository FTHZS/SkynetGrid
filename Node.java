import java.io.*;
import java.net.*;
import javax.swing.*;

class Node {
    private static final String SERVER_IP = "192.168.1.58";
    private static final int SERVER_PORT = 5678; // chat/protocol
    private static final int FILE_PORT = 6789;   // file transfer
    public static String location = "R2C3";

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    public void start() {
        boolean connected = connectToServer();

        if (!connected) {
            System.out.println("Server not found. Starting server...");
            new Thread(() -> {
                try { Server.main(new String[]{}); } 
                catch (Exception ex) { ex.printStackTrace(); }
            }).start();

            while (!connected) {
                try { Thread.sleep(500); connected = connectToServer(); }
                catch (InterruptedException ignored) {}
            }
        }

        //System.out.println("Connected to server as " + location);
        startListening();
    }

    private boolean connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF(location);
            dos.flush();
            return true;
        } catch (IOException e) { return false; }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (true) {
    
                    String header = dis.readUTF();
                    if (header.equals("SERVER_CLOSED")) {
                        handleDisconnect();
                        break;
                    } else if (header.equals("REQUEST_NODE_LOCATION")) {
                        dos.writeUTF("NODE_LOCATION|" + location);
                        dos.flush();
                        continue;
                    }
                    
                        
                    String[] parts = header.split("\\|");
                    if (parts.length < 2) continue;
    
                    String target = parts[0];
                    String type = parts[1];
    
                    if (!target.equals(location))
                        continue;
    
                    // ---------------------------
                    //   MESSAGE HANDLING
                    // ---------------------------
                    if ("MSG".equals(type)) {
    
                        // COMMAND FORMAT:
                        // MSG | sender | CMD | command...
                        if (parts.length >= 4 && parts[2].equals("CMD")) {
    
                            StringBuilder cmd = new StringBuilder();
                            for (int i = 3; i < parts.length; i++) {
                                cmd.append(parts[i]);
                                if (i < parts.length - 1) cmd.append(" ");
                            }    
                            runLocalTerminalCommand(cmd.toString());
                            continue;
                        }
    
                        // NORMAL MESSAGE
                        if (parts.length >= 3) {
                            String message = parts[2];
                            SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(null, message, "Message", JOptionPane.INFORMATION_MESSAGE)
                            );
                        }
    
                        continue;
                    }
    
                    // ---------------------------
                    //   FILE HANDLING
                    // ---------------------------
                    if ("FILE".equals(type) && parts.length >= 4) {
                        String filename = parts[2];
                        long filesize = Long.parseLong(parts[3]);
                        new Thread(() -> receiveFileFromServer(filename, filesize)).start();
                    }
    
                }
    
            } catch (IOException e) {
                handleDisconnect();
            }
    
        }).start();
    }

    
    private void runLocalTerminalCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            // Optional: read output for logging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // prints command output in Node's console
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void receiveFileFromServer(String filename, long filesize) {
        final File[] saveFile = new File[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(filename));
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
                    saveFile[0] = chooser.getSelectedFile();
            });
        } catch (Exception e) { e.printStackTrace(); return; }

        if (saveFile[0] == null) return;

        try (Socket fs = new Socket(SERVER_IP, FILE_PORT);
             DataOutputStream fileOut = new DataOutputStream(fs.getOutputStream());
             DataInputStream fileIn = new DataInputStream(fs.getInputStream());
             FileOutputStream fos = new FileOutputStream(saveFile[0])) {

            fileOut.writeUTF("REQUEST|" + location + "|" + filename);
            fileOut.flush();

            String response = fileIn.readUTF();
            if (response.startsWith("ERROR")) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "File not found on server",
                            "Error", JOptionPane.ERROR_MESSAGE)
                );
                return;
            }

            long remaining = Long.parseLong(response.split("\\|")[1]);
            byte[] buffer = new byte[4096];
            int read;
            while (remaining > 0 && (read = fileIn.read(buffer, 0, (int)Math.min(buffer.length, remaining))) > 0) {
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.flush();

            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, "File saved to: " + saveFile[0].getAbsolutePath())
            );

        } catch (IOException e) { e.printStackTrace(); }
    }

    public void sendMessageToServer(String targetComputer, String message) {
        try { dos.writeUTF(targetComputer + "|MSG|" + message); dos.flush(); } 
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendFileToServer(String targetComputer, File file) {
        new Thread(() -> {
            try (Socket fs = new Socket(SERVER_IP, FILE_PORT);
                 DataOutputStream fileOut = new DataOutputStream(fs.getOutputStream());
                 FileInputStream fis = new FileInputStream(file)) {

                // SEND|sender|target|filename|filesize
                fileOut.writeUTF("SEND|" + location + "|" + targetComputer + "|" + file.getName() + "|" + file.length());
                fileOut.flush();

                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) > 0) {
                    fileOut.write(buffer, 0, read);
                }
                fileOut.flush();
                System.out.println("File sent: " + file.getName() + " -> " + targetComputer);

            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void handleDisconnect() {
        System.out.println("Disconnected from server.");
        closeClient();
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override public void run() { System.exit(0); }
        }, 2000);
    }

    private void closeClient() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (dis != null) dis.close();
            if (dos != null) dos.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public static void enableNodeAutostart(String serviceName, String className) {
        try {
            String home = System.getProperty("user.home");
            String appDir = new File(".").getCanonicalPath();
    
            // systemd user service location
            File serviceFile = new File(home + "/.config/systemd/user/" + serviceName + ".service");
            serviceFile.getParentFile().mkdirs();
    
            // Build Exec command
            String execLine = "/usr/bin/java -cp \"" + appDir + "\" " + className;
    
            String content =
                    "[Unit]\n" +
                    "Description=Auto-start SkynetGrid program\n" +
                    "After=network-online.target\n\n" +
    
                    "[Service]\n" +
                    "Type=simple\n" +
                    "ExecStart=" + execLine + "\n" +
                    "WorkingDirectory=" + appDir + "\n" +
                    "Restart=always\n" +
                    "RestartSec=5\n\n" +
    
                    "[Install]\n" +
                    "WantedBy=default.target\n";
    
            // Write service file
            try (FileWriter fw = new FileWriter(serviceFile)) {
                fw.write(content);
            }
    
            // Reload systemd user configuration
            new ProcessBuilder("systemctl", "--user", "daemon-reload")
                    .start().waitFor();
    
            // Enable service
            new ProcessBuilder("systemctl", "--user", "enable", serviceName + ".service")
                    .start().waitFor();
    
            // Start service now
            new ProcessBuilder("systemctl", "--user", "start", serviceName + ".service")
                    .start().waitFor();
    
            System.out.println("Node autostart enabled using systemd:");
            System.out.println(serviceFile.getAbsolutePath());
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        enableNodeAutostart("SkynetGrid", "Node");
        new Node().start();
    }
}
