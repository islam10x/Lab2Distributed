package com.example.lab2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ClientApp extends Application {

    private GameClient client;

    private TextField ipField = new TextField("127.0.0.1");
    private TextField portField = new TextField("5000");
    private TextField guessField = new TextField();

    private Button connectButton = new Button("Connect");
    private Button guessButton = new Button("SUBMIT GUESS");

    private Label status = new Label("NOT CONNECTED");
    private Label feedback = new Label("Connect to the server to start playing.");
    private Label attemptsLabel = new Label("5");

    private int remainingAttempts = 5;
    private boolean gameEnded = false;

    @Override
    public void start(Stage stage) {

        stage.setTitle("Secret Number - Player");

        // Header
        Label title = new Label("SECRET NUMBER");
        title.getStyleClass().add("title");

        Label subtitle = new Label("MULTIPLAYER GUESSING GAME");
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(5, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // Connection section
        Label connectionTitle = new Label("SERVER CONNECTION");
        connectionTitle.getStyleClass().add("section-title");

        ipField.setPromptText("Server IP");
        portField.setPromptText("Port");

        ipField.getStyleClass().add("input");
        portField.getStyleClass().add("input");

        ipField.setPrefWidth(190);
        portField.setPrefWidth(80);

        connectButton.getStyleClass().add("primary-button");

        HBox connectionFields = new HBox(10,
                ipField,
                portField,
                connectButton
        );

        connectionFields.setAlignment(Pos.CENTER_LEFT);

        VBox connectionCard = new VBox(12,
                connectionTitle,
                connectionFields
        );

        connectionCard.getStyleClass().add("card");

        // Game status
        Label statusTitle = new Label("CONNECTION STATUS");
        statusTitle.getStyleClass().add("section-title");

        status.getStyleClass().add("status-stopped");

        HBox statusBox = new HBox(10, statusTitle, status);
        statusBox.setAlignment(Pos.CENTER);

        // Guessing area
        Label gameTitle = new Label("Guess the secret number!");
        gameTitle.getStyleClass().add("game-title");

        Label range = new Label("Choose a number between 0 and 100");
        range.getStyleClass().add("subtitle");

        guessField.setPromptText("Enter your guess...");
        guessField.setAlignment(Pos.CENTER);
        guessField.getStyleClass().add("guess-input");

        guessButton.getStyleClass().add("primary-button");
        guessButton.setMaxWidth(Double.MAX_VALUE);
        guessButton.setDisable(true);
        guessField.setDisable(true);

        VBox gameCard = new VBox(18,
                gameTitle,
                range,
                guessField,
                guessButton
        );

        gameCard.setAlignment(Pos.CENTER);
        gameCard.getStyleClass().add("game-card");

        // Feedback
        feedback.setWrapText(true);
        feedback.setAlignment(Pos.CENTER);
        feedback.getStyleClass().add("feedback");

        VBox feedbackCard = new VBox(10,
                new Label("GAME FEEDBACK"),
                feedback
        );

        feedbackCard.setAlignment(Pos.CENTER);
        feedbackCard.getStyleClass().add("card");

        // Attempts counter
        Label attemptsTitle = new Label("ATTEMPTS REMAINING");
        attemptsTitle.getStyleClass().add("section-title");

        attemptsLabel.getStyleClass().add("attempts-number");

        VBox attemptsCard = new VBox(8,
                attemptsTitle,
                attemptsLabel
        );

        attemptsCard.setAlignment(Pos.CENTER);
        attemptsCard.getStyleClass().add("card");

        // Main layout
        VBox root = new VBox(18,
                header,
                connectionCard,
                statusBox,
                gameCard,
                feedbackCard,
                attemptsCard
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));
        root.getStyleClass().add("root");

        // Actions
        connectButton.setOnAction(e -> connect());
        guessButton.setOnAction(e -> makeGuess());
        guessField.setOnAction(e -> makeGuess());

        Scene scene = new Scene(root, 550, 700);

        scene.getStylesheets().add(
                getClass().getResource("/com/example/lab2/style.css")
                        .toExternalForm()
        );

        stage.setScene(scene);
        stage.setMinWidth(500);
        stage.setMinHeight(650);

        stage.setOnCloseRequest(e -> {
            if (client != null) {
                client.disconnect();
            }
        });

        stage.show();
    }

    // Connect to server
    private void connect() {

        String host = ipField.getText().trim();

        if (host.isEmpty()) {
            feedback.setText("Please enter a server IP.");
            return;
        }

        try {

            int port = Integer.parseInt(portField.getText().trim());

            if (port < 1 || port > 65535) {
                feedback.setText("Port must be between 1 and 65535.");
                return;
            }

            connectButton.setDisable(true);
            ipField.setDisable(true);
            portField.setDisable(true);

            status.setText("CONNECTING...");
            status.getStyleClass().removeAll(
                    "status-running",
                    "status-stopped"
            );

            feedback.setText("Waiting for server...");
            gameEnded = false;

            client = new GameClient(new GameClient.Listener() {

                @Override
                public void onConnected(int attempts) {

                    Platform.runLater(() -> {

                        remainingAttempts = attempts;

                        status.setText("CONNECTED");
                        status.getStyleClass().removeAll("status-stopped");
                        status.getStyleClass().add("status-running");

                        feedback.setText("Good luck! Make your first guess.");

                        attemptsLabel.setText(String.valueOf(attempts));

                        guessButton.setDisable(false);
                        guessField.setDisable(false);

                        guessField.requestFocus();
                    });
                }

                @Override
                public void onMessage(
                        String type, String message, int remaining) {

                    Platform.runLater(() -> {

                        feedback.setText(message);

                        switch (type) {

                            case "HIGH":
                                remainingAttempts = remaining;
                                feedback.setText("TOO HIGH! Try a lower number.");
                                attemptsLabel.setText(String.valueOf(remaining));
                                enableGuessing();
                                break;

                            case "LOW":
                                remainingAttempts = remaining;
                                feedback.setText("TOO LOW! Try a higher number.");
                                attemptsLabel.setText(String.valueOf(remaining));
                                enableGuessing();
                                break;

                            case "WIN":
                                gameEnded = true;
                                feedback.setText("YOU WON! " + message);
                                finishGame();
                                break;

                            case "LOST":
                                gameEnded = true;
                                feedback.setText("YOU LOST! " + message);
                                finishGame();
                                break;

                            case "GAME_OVER":
                                gameEnded = true;
                                feedback.setText("GAME OVER! " + message);
                                finishGame();
                                break;

                            case "ERROR":
                                feedback.setText(message);

                                if (!gameEnded && client.isConnected()) {
                                    enableGuessing();
                                }
                                break;
                        }
                    });
                }

                @Override
                public void onDisconnected() {

                    Platform.runLater(() -> {

                        status.setText("DISCONNECTED");

                        status.getStyleClass().removeAll("status-running");
                        status.getStyleClass().add("status-stopped");

                        connectButton.setDisable(false);
                        ipField.setDisable(false);
                        portField.setDisable(false);

                        guessButton.setDisable(true);
                        guessField.setDisable(true);
                    });
                }
            });

            client.connect(host, port);

        } catch (NumberFormatException e) {

            feedback.setText("Invalid port number.");

            connectButton.setDisable(false);
            ipField.setDisable(false);
            portField.setDisable(false);
        }
    }

    // Enable guessing controls
    private void enableGuessing() {
        guessButton.setDisable(false);
        guessField.setDisable(false);
        guessField.requestFocus();
    }

    // Send guess
    private void makeGuess() {

        if (client == null || !client.isConnected() || gameEnded) {
            return;
        }

        try {

            int guess = Integer.parseInt(guessField.getText().trim());

            if (guess < 0 || guess > 100) {
                feedback.setText("Enter a number between 0 and 100.");
                return;
            }

            client.sendGuess(guess);

            guessField.clear();
            guessButton.setDisable(true);
            guessField.setDisable(true);

            feedback.setText("Guess sent! Waiting for response...");

        } catch (NumberFormatException e) {

            feedback.setText("Please enter a valid number.");
        }
    }

    // Finish game
    private void finishGame() {

        guessButton.setDisable(true);
        guessField.setDisable(true);

        status.setText("GAME FINISHED");

        if (client != null) {
            client.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}