package com.example.lab2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ServerApp extends Application {

    private GameServer server;

    private Label status = new Label("STOPPED");
    private Label playerCount = new Label("0");
    private TextField portField = new TextField("5000");

    private ListView<String> playersList = new ListView<>();
    private TextArea logs = new TextArea();

    private Button startButton = new Button("Start Server");
    private Button stopButton = new Button("Stop Server");

    @Override
    public void start(Stage stage) {

        stage.setTitle("Secret Number - Server");

        // Header
        Label title = new Label("SECRET NUMBER");
        title.getStyleClass().add("title");

        Label subtitle = new Label("SERVER CONTROL PANEL");
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(5, title, subtitle);

        // Server status card
        Label statusTitle = new Label("SERVER STATUS");
        statusTitle.getStyleClass().add("card-title");

        status.getStyleClass().add("status-stopped");

        Label countTitle = new Label("CONNECTED PLAYERS");
        countTitle.getStyleClass().add("card-title");

        playerCount.getStyleClass().add("number");

        VBox statusCard = new VBox(12,
                statusTitle,
                status,
                new Separator(),
                countTitle,
                playerCount
        );

        statusCard.getStyleClass().add("card");

        // Server configuration
        Label portLabel = new Label("Port");

        portField.getStyleClass().add("input");
        portField.setPrefWidth(100);

        HBox portBox = new HBox(10, portLabel, portField);
        portBox.setAlignment(Pos.CENTER_LEFT);

        startButton.getStyleClass().add("primary-button");
        stopButton.getStyleClass().add("danger-button");

        stopButton.setDisable(true);

        HBox buttons = new HBox(10, startButton, stopButton);

        VBox controlCard = new VBox(15,
                new Label("SERVER CONFIGURATION"),
                portBox,
                buttons
        );

        controlCard.getStyleClass().add("card");

        // Connected players
        playersList.setPlaceholder(
                new Label("Waiting for players...")
        );

        playersList.setPrefHeight(140);

        VBox playersCard = new VBox(12,
                new Label("CONNECTED PLAYERS"),
                playersList
        );

        playersCard.getStyleClass().add("card");

        // Activity logs
        logs.setEditable(false);
        logs.setWrapText(true);
        logs.setPrefRowCount(7);

        VBox logsCard = new VBox(12,
                new Label("ACTIVITY LOG"),
                logs
        );

        logsCard.getStyleClass().add("card");

        // Main layout
        VBox root = new VBox(18,
                header,
                statusCard,
                controlCard,
                playersCard,
                logsCard
        );

        root.setPadding(new Insets(25));
        root.getStyleClass().add("root");

        // Start server
        startButton.setOnAction(e -> {

            try {

                int port = Integer.parseInt(portField.getText());

                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("Invalid port number.");
                }

                server = new GameServer(new GameServer.Listener() {

                    @Override
                    public void onLog(String message) {
                        Platform.runLater(() ->
                                logs.appendText(message + "\n"));
                    }

                    @Override
                    public void onPlayersChanged(
                            java.util.List<String> players) {

                        Platform.runLater(() -> {
                            playersList.getItems().setAll(players);
                            playerCount.setText(
                                    String.valueOf(players.size())
                            );
                        });
                    }

                    @Override
                    public void onStatus(String message) {
                        Platform.runLater(() -> {
                            status.setText(message.toUpperCase());
                            status.getStyleClass().removeAll(
                                    "status-running",
                                    "status-stopped"
                            );

                            status.getStyleClass().add(
                                    message.equalsIgnoreCase("Running")
                                            ? "status-running"
                                            : "status-stopped"
                            );
                        });
                    }
                });

                server.startServer(port);

                startButton.setDisable(true);
                stopButton.setDisable(false);
                portField.setDisable(true);

            } catch (Exception ex) {

                new Alert(Alert.AlertType.ERROR,
                        "Could not start server: " + ex.getMessage())
                        .showAndWait();
            }
        });

        // Stop server
        stopButton.setOnAction(e -> stopServer());

        // Window closing
        stage.setOnCloseRequest(e -> {
            if (server != null) {
                server.stopServer();
            }
        });

        Scene scene = new Scene(root, 650, 850);

        scene.getStylesheets().add(
                getClass().getResource("/com/example/lab2/style.css")
                        .toExternalForm()
        );

        stage.setScene(scene);
        stage.setMinWidth(550);
        stage.setMinHeight(700);
        stage.show();
    }

    private void stopServer() {

        if (server != null) {
            server.stopServer();
            server = null;
        }

        startButton.setDisable(false);
        stopButton.setDisable(true);
        portField.setDisable(false);

        playersList.getItems().clear();
        playerCount.setText("0");

        status.setText("STOPPED");
        status.getStyleClass().removeAll(
                "status-running",
                "status-stopped"
        );
        status.getStyleClass().add("status-stopped");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stopServer();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}