package com.example.lab2;


import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {

    static int secret;
    static boolean gameOver;
    static String winnerIP;

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    private final ArrayList<PlayerHandler> players =
            new ArrayList<>();

    private final Listener listener;

    public interface Listener {
        void onLog(String message);
        void onPlayersChanged(List<String> players);
        void onStatus(String status);
    }

    public GameServer(Listener listener) {
        this.listener = listener;
    }

    // Start server
    public synchronized void startServer(int port)
            throws IOException {

        if (running) return;

        secret = new Random().nextInt(101);
        gameOver = false;
        winnerIP = null;

        serverSocket = new ServerSocket(port);
        running = true;

        listener.onStatus("Running on port " + port);
        listener.onLog("Server started.");
        listener.onLog("Secret number generated.");

        Thread acceptThread = new Thread(this::acceptPlayers);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    // Accept players
    private void acceptPlayers() {

        while (running) {
            try {
                Socket socket = serverSocket.accept();

                if (isGameOver()) {
                    PrintWriter out = new PrintWriter(
                            socket.getOutputStream(), true);

                    out.println("GAME_OVER|" + winnerIP);
                    socket.close();
                    continue;
                }

                PlayerHandler player =
                        new PlayerHandler(socket, this);

                addPlayer(player);
                player.start();

            } catch (IOException e) {
                if (running) {
                    listener.onLog("Connection error: " + e.getMessage());
                }
            }
        }
    }

    // Add player
    public synchronized void addPlayer(PlayerHandler player) {
        players.add(player);
        updatePlayers();

        listener.onLog("Player connected: " + player.getPlayerIP());
    }

    // Remove player
    public synchronized void removePlayer(PlayerHandler player) {
        players.remove(player);
        updatePlayers();

        listener.onLog("Player disconnected: " + player.getPlayerIP());
    }

    // Check guess (synchronized)
    public static synchronized String checkGuess(
            int guess, String ip, int attempts) {

        if (gameOver) {
            return "GAME_OVER";
        }

        if (guess == secret) {
            gameOver = true;
            winnerIP = ip;
            return "WIN";
        }

        if (attempts == 5) {
            return "LOST";
        }

        if (guess > secret) {
            return "HIGH";
        }

        return "LOW";
    }

    // Check game state
    public static synchronized boolean isGameOver() {
        return gameOver;
    }

    // Notify all players after someone wins
    public void notifyWinner(PlayerHandler winner) {

        listener.onLog("Winner: " + winner.getPlayerIP());

        ArrayList<PlayerHandler> copy;

        synchronized (this) {
            copy = new ArrayList<>(players);
        }

        for (PlayerHandler player : copy) {
            if (player != winner) {
                player.sendGameOver(winnerIP);
            }
        }
    }

    // Update JavaFX player list
    private synchronized void updatePlayers() {

        ArrayList<String> names = new ArrayList<>();

        for (PlayerHandler player : players) {
            names.add(player.getPlayerIP());
        }

        listener.onPlayersChanged(names);
    }

    public void log(String message) {
        listener.onLog(message);
    }

    // Stop server
    public void stopServer() {

        running = false;

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        ArrayList<PlayerHandler> copy;

        synchronized (this) {
            copy = new ArrayList<>(players);
        }

        for (PlayerHandler player : copy) {
            player.closeConnection();
        }

        listener.onStatus("Stopped");
        listener.onLog("Server stopped.");
    }
}

