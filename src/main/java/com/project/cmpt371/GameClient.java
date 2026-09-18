package com.project.cmpt371;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The GameClient class represents the client-side application for the Team Box Conquest game.
 * It handles the connection to the server, displays the game board, and manages user interactions.
 * Players can claim squares on the grid by pressing and holding them, chat with other players,
 * and view team information and scores.
 */
public class GameClient extends Application {
    /** Default server IP address */
    public static String serverIP = "localhost";
    
    /** Default server port */
    public static int serverPort = 12345;
    
    /** Player's name, set from the launcher */
    public static String playerName = "";
    
    /** Player's selected team color, set from the launcher */
    public static String teamColor = "TEAM_A";

    /** Size of the game grid (10x10) */
    private static final int GRID_SIZE = 10;
    
    /** Socket for connection to the server */
    private Socket socket;
    
    /** Input stream for receiving messages from the server */
    private DataInputStream inputStream;
    
    /** Output stream for sending messages to the server */
    private DataOutputStream outputStream;
    
    /** Map of grid coordinates to Rectangle UI elements */
    private Map<String, Rectangle> gridSquares;
    
    /** Current ownership state of each square on the board */
    private String[][] boardState = new String[GRID_SIZE][GRID_SIZE];
    
    /** Tracks which teams are currently holding each square */
    private List<String>[][] heldState = new ArrayList[GRID_SIZE][GRID_SIZE];
    
    /** The team assigned to this client by the server */
    private String assignedTeam;
    
    /** Text element for displaying game status messages */
    private Text gameInfo;
    
    /** Text element for displaying the red team score */
    private Text redScoreText;
    
    /** Text element for displaying the blue team score */
    private Text blueScoreText;
    
    /** TextArea listing players on Team A (Red) */
    private TextArea teamAList;
    
    /** TextArea listing players on Team B (Blue) */
    private TextArea teamBList;
    
    /** TextArea for displaying chat messages */
    private TextArea chatArea;
    
    /** TextField for entering chat messages */
    private TextField chatInput;
    
    /** Flag to control the message listening thread */
    private boolean isRunning = true;
    
    /** The main application window */
    private Stage primaryStage;
    
    /** The grid container for the game board */
    private GridPane gridPane;

    /**
     * Initializes the client application, connects to the server, and sets up the UI.
     *
     * @param primaryStage The primary stage provided by the JavaFX framework
     * @throws Exception If connection to the server fails
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        // Connect to the server
        socket = new Socket(serverIP, serverPort);
        primaryStage.getIcons().add(new Image(String.valueOf(getClass().getResource("/Images/icon.png"))));
        System.out.println("Client " + playerName + " connected to " + serverIP + ":" + serverPort);
        
        // Set up data streams
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
        
        // Configure window size
        primaryStage.setWidth(1100);
        primaryStage.setHeight(1040);
        primaryStage.setFullScreen(true);

        // Initialize heldState with empty lists for each grid cell
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                heldState[row][col] = new ArrayList<>();
            }
        }

        // Set up the UI components
        setupUI();

        // Send player information to the server
        outputStream.writeUTF("PLAYER_INFO " + playerName + " " + teamColor);
        outputStream.flush();

        // Start a separate thread for listening to server messages
        new Thread(this::listenForMessages).start();
    }
}
