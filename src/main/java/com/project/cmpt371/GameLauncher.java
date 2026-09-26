package com.project.cmpt371;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GameLauncher serves as the entry point for the Team Box Conquest game.
 * It provides a GUI for users to either host a new game or join an existing one.
 * This launcher handles:
 * - Starting a new game server
 * - Connecting to an existing server
 * - Player name selection
 * - Team selection and availability checking
 * - Game client initialization
 */
public class GameLauncher extends Application {
    /** Maximum number of players allowed per team */
    private static final int MAX_PLAYERS_PER_TEAM = 3;

    /**
     * Initialize and display the main launcher interface.
     * Provides options to host a new game or join an existing one.
     *
     * @param primaryStage The primary stage provided by JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Game Launcher");
        primaryStage.getIcons().add(new Image(String.valueOf(getClass().getResource("/Images/icon.png"))));

        // Create main menu buttons
        Button hostButton = new Button("Host Game");
        Button joinButton = new Button("Join Game");

        // Set button IDs for CSS styling
        hostButton.setId("hostButton");
        joinButton.setId("joinButton");

        // Load title image
        Image titleImage = new Image(String.valueOf(getClass().getResource("/Images/GameLauncherTitle.png")));
        ImageView titleImageView = new ImageView(titleImage);

        // Layout setup for main menu
        VBox optionsBox = new VBox(20, hostButton, joinButton);
        VBox screenBox = new VBox(30, titleImageView, optionsBox);
        optionsBox.setAlignment(Pos.CENTER);
        screenBox.setAlignment(Pos.CENTER);

        // Create scene with styling
        Scene scene = new Scene(screenBox, 400, 300);
        scene.getStylesheets().add(getClass().getResource("/css/launcher-style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        // Add button event handlers
        hostButton.setOnAction(e -> showHostScreen(primaryStage));
        joinButton.setOnAction(e -> showJoinScreen(primaryStage));
    }

    /**
     * Displays the host screen showing IP and port information.
     * Starts the game server in a separate thread.
     *
     * @param primaryStage The primary stage to update
     */
    private void showHostScreen(Stage primaryStage) {
        // Start the game server in a separate thread
        new Thread(() -> {
            try {
                GameServer.main(new String[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        // Get local IP address to display for other players to connect
        final String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return; // Exit method if IP cannot be determined
        }

        // Create IP address display with copy button
        Label ipLabel = new Label("IP Address: " + ip);
        ipLabel.setId("ipLabel");
        Button ipCopyButton = new Button("Copy");
        ipCopyButton.setId("ipCopyButton");
        ipCopyButton.setOnAction(e -> {
            // Copy IP to clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(ip);
            clipboard.setContent(content);
            
            // Provide visual feedback
            ipCopyButton.setText("Copied!");
            ipCopyButton.setDisable(true);
            
            // Reset button after 2 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Show "Copied!" for 2 seconds
                    Platform.runLater(() -> {
                        ipCopyButton.setText("Copy");
                        ipCopyButton.setDisable(false);
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
        HBox ipBox = new HBox(10, ipLabel, ipCopyButton);
        ipBox.setAlignment(Pos.CENTER);

        // Create port display with copy button
        Label portLabel = new Label("Port: 12345");
        portLabel.setId("portLabel");
        Button portCopyButton = new Button("Copy");
        portCopyButton.setId("portCopyButton");
        portCopyButton.setOnAction(e -> {
            // Copy port to clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString("12345");
            clipboard.setContent(content);
            
            // Provide visual feedback
            portCopyButton.setText("Copied!");
            portCopyButton.setDisable(true);
            
            // Reset button after 2 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        portCopyButton.setText("Copy");
                        portCopyButton.setDisable(false);
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
        HBox portBox = new HBox(10, portLabel, portCopyButton);
        portBox.setAlignment(Pos.CENTER);

        // Combine IP and port information
        VBox infoBox = new VBox(15, ipBox, portBox);
        infoBox.setAlignment(Pos.CENTER);

        // Add start button to proceed to player setup
        Button startButton = new Button("Start");
        startButton.setId("startButton");
        startButton.setOnAction(e -> showHostPlayerSetup(primaryStage));

        // Create layout for host screen
        BorderPane hostPane = new BorderPane();
        hostPane.setCenter(infoBox);

        HBox bottomBox = new HBox(startButton);
        bottomBox.setAlignment(Pos.BOTTOM_RIGHT);
        bottomBox.setPadding(new Insets(15));
        hostPane.setBottom(bottomBox);

        // Update scene
        Scene hostScene = new Scene(hostPane, 400, 300);
        hostScene.getStylesheets().add(getClass().getResource("/css/launcher-style.css").toExternalForm());
        primaryStage.setScene(hostScene);
    }
}
