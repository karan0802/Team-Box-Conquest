package com.project.cmpt371;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * The GameServer class manages the server-side logic for the Team Box Conquest game.
 * It handles client connections, game state management, player team assignments,
 * and the core game mechanics including the shared grid state and win conditions.
 */
public class GameServer {
    /** Server port number */
    private static final int PORT = 12345;
    
    /** Size of the game grid (10x10) */
    private static final int GRID_SIZE = 10;
    
    /** Maximum number of players allowed per team */
    private static final int MAX_PLAYERS_PER_TEAM = 3;
    
    /** Maximum total number of players allowed (across all teams) */
    private static final int MAX_TOTAL_PLAYERS = 6;
    
    /** Map of client IDs to their handlers */
    private static Map<String, ClientHandler> clients = new HashMap<>();
    
    /** Current ownership state of each square on the board */
    private static String[][] boardState = new String[GRID_SIZE][GRID_SIZE];
    
    /** Maps each grid cell to teams currently holding it and their count */
    private static Map<String, Integer>[][] heldState = new HashMap[GRID_SIZE][GRID_SIZE];
    
    /** Thread pool for handling multiple client connections */
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    /** Scheduled executor service for managing claim timers */
    private static final ScheduledExecutorService timerService = Executors.newScheduledThreadPool(1);
    
    /** Map of grid coordinates to their claim timers */
    private static Map<String, ScheduledFuture<?>> claimTimers = new HashMap<>();
    
    /** Count of players on Team A */
    private static int teamACount = 0;
    
    /** Count of players on Team B */
    private static int teamBCount = 0;
    
    /** Counter for generating unique client IDs */
    private static int clientCounter = 0;
    
    /** List of player names on Team A */
    private static List<String> teamAPlayers = new ArrayList<>();
    
    /** List of player names on Team B */
    private static List<String> teamBPlayers = new ArrayList<>();
    
    /**
     * Main method that initializes and starts the game server.
     * Sets up the initial board state and listens for client connections.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        resetBoard();
        System.out.println("Game Server started on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Accept new client connection
                Socket clientSocket = serverSocket.accept();
                String clientId = "Client_" + clientCounter++;
                System.out.println("New client connected: " + clientSocket.getInetAddress() + ":" + 
                        clientSocket.getPort() + " as " + clientId);

                // Check if server is at capacity
                synchronized (clients) {
                    if (clients.size() >= MAX_TOTAL_PLAYERS) {
                        try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
                            out.writeUTF("SERVER_FULL");
                            clientSocket.close();
                        }
                        continue;
                    }
                }

                // Create and register a new client handler
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                executorService.submit(clientHandler);
                synchronized (clients) {
                    clients.put(clientId, clientHandler);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets the game board and player state to initial values.
     * Called at server start and after each game ends.
     */
    private static void resetBoard() {
        // Reset grid state
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                boardState[row][col] = "UNCLAIMED";
                heldState[row][col] = new HashMap<>();
            }
        }
        
        // Reset team data
        teamAPlayers.clear();
        teamBPlayers.clear();
        teamACount = 0;
        teamBCount = 0;
        
        // Reset client tracking
        clients.clear();
        clientCounter = 0;
        
        // Cancel any active timers
        claimTimers.clear();
    }

    /**
     * Broadcasts the current game board state to all connected clients.
     *
     * @throws IOException If there's an error sending the game state
     */
    private static void broadcastGameState() throws IOException {
        synchronized (clients) {
            for (ClientHandler clientHandler : clients.values()) {
                clientHandler.sendGameState(boardState);
            }
        }
    }

    /**
     * Processes a client's request to hold (start claiming) a grid square.
     * Implements the logic for tracking which players are holding each square
     * and manages the claim timer.
     *
     * @param client The client handler for the player making the request
     * @param row The row of the square
     * @param col The column of the square
     * @throws IOException If there's an error broadcasting updates
     */
    private static void handleHoldRequest(ClientHandler client, int row, int col) throws IOException {
        synchronized (boardState) {
            // Only allow interaction with unclaimed squares
            if ("UNCLAIMED".equals(boardState[row][col])) {
                String team = client.getTeam();
                Map<String, Integer> holdMap = heldState[row][col];
                
                // Increment the count for this team
                int newCount = holdMap.getOrDefault(team, 0) + 1;
                holdMap.put(team, newCount);

                // If this is the first player from this team to hold the square
                if (newCount == 1) {
                    broadcastHoldInfo(row, col, team);
                    
                    // Manage the claim timer based on team count
                    if (holdMap.size() == 1) {
                        // Only one team is holding - start claim timer
                        scheduleClaimTimer(row, col);
                    } else if (holdMap.size() > 1) {
                        // Multiple teams are holding (tug-of-war) - cancel timer
                        cancelClaimTimer(row, col);
                    }
                }
            }
        }
    }

    /**
     * Processes a client's request to release a grid square they were holding.
     * Handles the "tug-of-war" mechanics when multiple teams contest a square.
     *
     * @param client The client handler for the player making the request
     * @param row The row of the square
     * @param col The column of the square
     * @throws IOException If there's an error broadcasting updates
     */
    private static void handleReleaseRequest(ClientHandler client, int row, int col) throws IOException {
        synchronized (boardState) {
            String team = client.getTeam();
            Map<String, Integer> holdMap = heldState[row][col];
            
            if (holdMap.containsKey(team)) {
                int count = holdMap.get(team);
                if (count > 1) {
                    // Multiple players from this team are holding - decrement count
                    holdMap.put(team, count - 1);
                } else {
                    // Last player from this team is releasing - remove team
                    holdMap.remove(team);
                    broadcastReleaseInfo(row, col, team);
                    
                    // Handle contested square resolution (tug-of-war)
                    if (holdMap.size() == 1) {
                        // Only one team left - they win the square immediately
                        String winningTeam = holdMap.keySet().iterator().next();
                        boardState[row][col] = winningTeam;
                        heldState[row][col].clear();
                        cancelClaimTimer(row, col);
                        broadcastGameState();
                        checkWinCondition(null);
                        broadcastTeamScores();
                    } else if (holdMap.size() == 0) {
                        // No teams holding - cancel any timer
                        cancelClaimTimer(row, col);
                    } else {
                        // This shouldn't happen, but handle it anyway
                        cancelClaimTimer(row, col);
                    }
                }
            }
        }
    }
}
