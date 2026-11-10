package com.example.lab2;


import java.io.*;
import java.net.*;

public class GameClient {

    public interface Listener {
        void onConnected(int attempts);
        void onMessage(String type, String message, int remaining);
        void onDisconnected();
    }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private Listener listener;

    private volatile boolean connected = false;

    public GameClient(Listener listener) {
        this.listener = listener;
    }

    // Connect to server
    public void connect(String host, int port) {

        Thread connectionThread = new Thread(() -> {

            try {

                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);

                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                out = new PrintWriter(
                        socket.getOutputStream(), true);

                connected = true;

                String message;

                while ((message = in.readLine()) != null) {

                    String[] parts = message.split("\\|", 2);

                    String type = parts[0];

                    String value = parts.length > 1
                            ? parts[1] : "";

                    switch (type) {

                        case "WELCOME":

                            listener.onConnected(
                                    Integer.parseInt(value));
                            break;

                        case "HIGH":
                        case "LOW":

                            listener.onMessage(
                                    type,
                                    type.equals("HIGH")
                                            ? "Your guess is too high!"
                                            : "Your guess is too low!",
                                    Integer.parseInt(value));
                            break;

                        case "WIN":

                            listener.onMessage(
                                    type, value, 0);
                            break;

                        case "LOST":

                            listener.onMessage(
                                    type, value, 0);
                            break;

                        case "GAME_OVER":

                            listener.onMessage(
                                    type,
                                    "Another player won: " + value,
                                    0);
                            break;

                        case "ERROR":

                            listener.onMessage(
                                    type, value, -1);
                            break;
                    }
                }

            } catch (IOException e) {

                listener.onMessage(
                        "ERROR",
                        "Cannot connect to server: " + e.getMessage(),
                        -1);

            } finally {

                connected = false;
                close();
                listener.onDisconnected();
            }

        });

        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    // Send guess
    public void sendGuess(int guess) {

        if (connected && out != null) {
            out.println(guess);
        }
    }

    // Disconnect
    public void disconnect() {
        connected = false;
        close();
    }

    private void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected;
    }
}

