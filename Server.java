import java.net.*;
import java.io.*;
import java.util.*;

class Server {
    private static final int PORT = 5678;       // chat/protocol
    private static final int FILE_PORT = 6789;  // file transfer
    private static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);

        // Console thread to stop server
        new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    String line = scanner.nextLine();
                    if (line.equalsIgnoreCase("/stop")) {
                        shutdownServer();
                        break;
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }, "server-console").start();

        // Chat server
        new Thread(Server::startChatServer).start();

        // File server
        new Thread(Server::startFileServer).start();
    }

    private static void startChatServer() {
        try (ServerSocket ss = new ServerSocket(PORT)) {
            serverSocket = ss;
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("Chat server stopped.");
        }
    }

    private static void startFileServer() {
        try (ServerSocket fileServer = new ServerSocket(FILE_PORT)) {
            System.out.println("File server started on port " + FILE_PORT);
            while (true) {
                Socket fs = fileServer.accept();
                new Thread(() -> handleFileSocket(fs)).start();
            }
        } catch (IOException e) { System.out.println("File server stopped."); }
    }

    private static void handleFileSocket(Socket fs) {
        try (DataInputStream dis = new DataInputStream(fs.getInputStream());
             DataOutputStream dos = new DataOutputStream(fs.getOutputStream())) {

            String header = dis.readUTF();
            String[] parts = header.split("\\|");
            if (parts.length < 2) return;

            if ("REQUEST".equals(parts[0])) {
                // Node requests a file
                String filename = parts[2];
                File tempFile = new File("tmp_" + filename);
                if (!tempFile.exists()) {
                    dos.writeUTF("ERROR|File not found");
                    return;
                }
                dos.writeUTF("OK|" + tempFile.length());
                try (FileInputStream fis = new FileInputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, read);
                    }
                }
                System.out.println("File served: " + filename + " -> " + parts[1]);

            } else if ("SEND".equals(parts[0])) {
                // Node sends a file to server
                String sender = parts[1];
                String target = parts[2];
                String filename = parts[3];
                long filesize = Long.parseLong(parts[4]);
                File tempFile = new File("tmp_" + filename);

                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    long remaining = filesize;
                    int read;
                    while (remaining > 0 && (read = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) > 0) {
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                }

                System.out.println("Received file: " + filename + " from " + sender + " for " + target);

                // Broadcast file protocol to target nodes
                broadcast(target + "|FILE|" + filename + "|" + filesize);
            }

        } catch (IOException e) { e.printStackTrace(); }
        finally { try { fs.close(); } catch (IOException ignored) {} }
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                try {
                    c.dos.writeUTF(message);
                    c.dos.flush();
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    private static void shutdownServer() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                try {
                    client.dos.writeUTF("SERVER_CLOSED");
                    client.socket.close();
                } catch (IOException ignored) {}
            }
        }
        try { serverSocket.close(); } catch (IOException ignored) {}
        System.exit(0);
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private DataOutputStream dos;
        private DataInputStream dis;
        private String username;

        public ClientHandler(Socket socket) {
            super("client-" + socket.getPort());
            this.socket = socket;
        }

        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                username = dis.readUTF();
                if (username == null) return;

                synchronized (clients) { clients.add(this); }
                System.out.println(username + " connected.");

                while (true) {
                    String msg = dis.readUTF();
                    if (msg == null) break;
                    System.out.println("Received from " + username + ": " + msg);
                    broadcast(msg);
                }

            } catch (IOException e) {
                System.out.println(username + " disconnected.");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
            }
        }
    }
}
