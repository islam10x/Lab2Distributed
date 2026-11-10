package com.example.lab2;


import java.io.*;
import java.net.*;

public class PlayerHandler extends Thread {

    private Socket socket;
    private GameServer server;

    BufferedReader in;
    PrintWriter out;

    private int attempts = 0;

    public PlayerHandler(Socket socket, GameServer server)
            throws IOException {

        this.socket = socket;
        this.server = server;

        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        out = new PrintWriter(
                socket.getOutputStream(), true);
    }

    public String getPlayerIP() {
        return socket.getInetAddress().getHostAddress()
                + ":" + socket.getPort();
    }

    @Override
    public void run() {

        String ip = getPlayerIP();

        try {

            out.println("WELCOME|5");

            String message;

            while ((message = in.readLine()) != null) {

                int guess;

                try {
                    guess = Integer.parseInt(message);
                } catch (NumberFormatException e) {
                    out.println("ERROR|Enter a valid number.");
                    continue;
                }

                if (guess < 0 || guess > 100) {
                    out.println("ERROR|Enter a number between 0 and 100.");
                    continue;
                }

                attempts++;

                String result = GameServer.checkGuess(
                        guess, ip, attempts);

                server.log(ip + " guessed " + guess);

                switch (result) {

                    case "WIN":

                        out.println("WIN|You guessed correctly!");

                        server.notifyWinner(this);

                        return;

                    case "HIGH":

                        out.println("HIGH|" + (5 - attempts));
                        break;

                    case "LOW":

                        out.println("LOW|" + (5 - attempts));
                        break;

                    case "LOST":

                        out.println("LOST|You used all 5 attempts.");
                        return;

                    case "GAME_OVER":

                        out.println("GAME_OVER|" + GameServer.winnerIP);
                        return;
                }
            }

        } catch (IOException e) {

            server.log("Connection lost: " + ip);

        } finally {

            server.removePlayer(this);
            closeConnection();
        }
    }

    public void sendGameOver(String winnerIP) {
        out.println("GAME_OVER|" + winnerIP);
        closeConnection();
    }

    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}

