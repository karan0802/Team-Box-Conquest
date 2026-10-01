# Team Box Conquest

![Team Box Conquest Banner](src/main/resources/Images/GameLauncherTitle.png)

**A Real-Time Multiplayer Strategy Game**

## Overview
Team Box Conquest is a high-performance, real-time multiplayer strategy game utilizing a custom socket-based architecture. Built from the ground up without third-party networking frameworks, it features a client-server model where two teams compete to dominate a shared 10x10 grid. The project demonstrates advanced concepts in distributed systems, concurrency control, and application-layer protocol design.

## Key Features
- **Real-Time Multiplayer**: Seamless interaction for multiple simultaneous players.
- **Custom Networking Protocol**: A bespoke application-layer protocol built on raw TCP sockets for optimized communication.
- **Concurrency Control**: Robust locking mechanisms to manage shared state (game board squares) and prevent race conditions.
- **Team-Based Strategy**: Cooperative gameplay mechanics requiring team coordination to win.
- **Live Synchronization**: Instant state propagation ensuring all clients have a consistent view of the game world.

## Quick Start 

### Prerequisites
- **Java 17** or higher
- **Maven** 3.6+

### Run the Game
1. **Clone the repository**
   ```bash
   git clone https://github.com/manavbansal1/Team-Box-Conquest.git
   cd Team-Box-Conquest
   ```

2. **Build and Run**
   This project uses Maven for dependency management and JavaFX for the UI.
   ```bash
   mvn clean javafx:run
   ```

   *Note: Since this is a client-server game, you can run multiple instances in separate terminal windows to simulate multiple players.*

## Technical Highlights

### Custom Network Stack
Instead of relying on high-level libraries, this project implements a raw socket communication layer:
- **Direct Socket Programming**: Full control over byte-stream transmission and parsing.
- **Protocol Design**: Developed a custom message classification system (Connection, Game State, Action, Flow) to handle complex game logic efficiently.

### Concurrency & State Management
Managing shared resources in a real-time environment is critical. The server implements strict concurrency controls:
- **Fine-Grained Locking**: Each square on the board functions as a shared object with its own lock state.
- **Race Condition Prevention**: Atomic operations ensure that simultaneous claims are resolved deterministically.
- **State Consistency**: A centralized server acts as the source of truth, utilizing a "lock request" pattern to validate all client actions before broadcast.

## How to Play
1. **Connect**: Join the server and get automatically assigned to a team.
2. **Claim**: Left-click and hold an unclaimed square for 2 seconds to capture it.
3. **Defend**: Watch for enemy claims in real-time and race to secure strategic positions.
4. **Win**: Connect 10 squares in a row/column/diagonal or control the majority of the board when it's full.

## Tech Stack
- **Language**: Java 17
- **Networking**: java.net.Socket (Raw Sockets)
- **GUI**: JavaFX
- **Build Tool**: Maven


## License
Distributed under the MIT License. See `LICENSE` for more information.
