# Between Us

## About / Synopsis

* A Java & Libgdx adaptation of Among Us/Mafia
* Project status: working/prototype
* Group 66

## Table of contents

> * [Between Us](#title--repository-name)
>   * [About / Synopsis](#about--synopsis)
>   * [Table of contents](#table-of-contents)
>   * [Playing Over the Internet](#playing-over-the-internet)
>   * [Installation](#installation)
>   * [Usage](#usage)
>     * [Screenshots](#screenshots)
>     * [Features](#features)
>   * [Code](#code)
>     * [Content](#content)
>     * [Build](#build)
>   * [Resources (Documentation and other links)](#resources-documentation-and-other-links)
>   * [Contributing / Reporting issues](#contributing--reporting-issues)

## Playing Over the Internet (No Port Forwarding Required!)

This version includes a **relay server** that allows you and your friends to play together over the internet without any network configuration or port forwarding.

### Quick Start - Playing with Friends

1. **Download the game** - Get the latest `BetweenUs.jar` from releases
2. **Run the game** - Double-click the JAR file (requires Java 8+)
3. **That's it!** - The game connects to the public relay server automatically

### Hosting Your Own Relay Server

If you want to host your own relay server (free options available):

#### Option 1: Deploy to Render.com (Free)

1. Create a free account at [render.com](https://render.com)
2. Click "New" → "Web Service"
3. Connect your GitHub repo or use this one
4. Set the following:
   - **Root Directory**: `relay-server`
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
5. Deploy! You'll get a URL like `https://your-app.onrender.com`
6. In the game, go to Settings and set the Relay URL to: `wss://your-app.onrender.com`

#### Option 2: Deploy to Railway.app (Free tier)

1. Create account at [railway.app](https://railway.app)
2. New Project → Deploy from GitHub
3. Select the `relay-server` folder
4. Railway auto-detects Node.js and deploys

#### Option 3: Run Locally (for LAN play)

```bash
cd relay-server
npm install
npm start
```

Then in game settings, use: `ws://YOUR_LOCAL_IP:3000`

### Configuration

The game creates a `game.properties` file with these settings:

```properties
# Use relay server for internet play (recommended)
use.relay=true
relay.url=wss://betweenus-relay.onrender.com

# Direct connection settings (if not using relay)
server.host=localhost
server.port=7077
```

### How It Works

**Relay Mode (Default):**
```
Player A ──┐
Player B ──┼── WebSocket ──> Relay Server <── WebSocket ── Game Server
Player C ──┘
```
All traffic goes through the relay server, eliminating the need for port forwarding.

**Direct Mode (Advanced):**
```
Player A ──┐
Player B ──┼── UDP:7077 ──> Game Server (port forwarded)
Player C ──┘
```
Requires the host to configure port forwarding.

## Installation (Legacy - Direct Mode)

* The Server (Server.java) must be hosted locally by one of the players and the port 7077 must be forwarded on the Server's machine.
* The player hosting the Server must provide their IPv4 address to the Client players.
* **NEW:** Use the relay server instead for easy internet play without port forwarding!

## Usage

### Screenshots

Title Screen

![1](https://github.com/meetdigrajkar/BetweenUs/blob/master/screenshots/main_screen.PNG)

Join Room Screen

![1](https://github.com/meetdigrajkar/BetweenUs/blob/master/screenshots/join_screen.PNG)

Create Room Screen

![1](https://github.com/meetdigrajkar/BetweenUs/blob/master/screenshots/create_room_screen.PNG)

Lobby Screen

![1](https://github.com/meetdigrajkar/BetweenUs/blob/master/screenshots/lobby_screen.PNG)

Game Screen

![1](https://github.com/meetdigrajkar/BetweenUs/blob/master/screenshots/game_screen.PNG)

### Features

* Crew Members
* Imposters
* Four Tasks: Admin, Communications, Electrical and Reactor
* Polus Map
* Multiplayer across the Internet (Client-Server architecture)
* **NEW:** Relay server for easy internet play without port forwarding

## Code

### Content

* Developed in Java using Libgdx and Gradle

### Requirements

* Java 8 or higher
* Gradle
* LibGdx

### Build

```bash
# Build the game
./gradlew build

# Create distributable JAR
./gradlew desktop:dist

# The JAR will be at: desktop/build/libs/BetweenUs.jar

# Run in development
./gradlew desktop:run
```

### Project Structure

```
BetweenUs/
├── core/                    # Shared game logic
│   └── src/com/
│       ├── mmog/           # Game client code
│       │   ├── Client.java       # Network client
│       │   ├── GameConfig.java   # Configuration
│       │   ├── RelayClient.java  # WebSocket relay client
│       │   ├── screens/          # UI screens
│       │   └── players/          # Player classes
│       └── server/         # Game server code
│           ├── Server.java       # UDP game server
│           └── Room.java         # Game room logic
├── desktop/                # Desktop launcher
├── relay-server/           # Node.js relay server
│   ├── server.js          # WebSocket relay
│   └── package.json
└── game.properties         # Runtime configuration
```

### Running (Development)

1. Clone repository from this github page.
2. Download the latest version and setup AdoptJDK in your favourite IDE from https://adoptopenjdk.net/
3. Open the project in your favourite IDE and import the project as a Gradle Project
4. **NEW:** The game now uses configurable settings - go to Settings in-game to configure
5. Run DesktopLauncher.java as a Java Application
6. One player creates a Room
7. Other players join via the "Join Room" Screen (Click Refresh to fetch rooms)
8. Once everyone is in the game, the host will press "Enter" to start the game.
9. Enjoy!

## Troubleshooting

**Can't connect to relay server?**
- Check your internet connection
- Try a different relay URL in Settings
- Free Render.com servers may take 30 seconds to "wake up" on first connection

**Game is laggy?**
- Deploy your own relay server closer to your location
- Use direct connection mode if all players are on the same network

**Players can't see each other?**
- Make sure everyone is using the same relay server URL
- Ensure the game server is running (one player must host)

## Resources (Documentation and other links)

* Great thanks to the sprite sheets available at https://www.spriters-resource.com/pc_computer/amongus/

## Contributing / Developers

* Meet Digrajkar
* Abdillihai Nur
* Tareq Hanafi
* Alec D'Alessandro
