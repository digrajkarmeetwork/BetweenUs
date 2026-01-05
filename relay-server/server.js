/**
 * BetweenUs WebSocket Relay Server
 *
 * This server acts as a relay between game clients and game servers.
 * It eliminates the need for port forwarding by routing all traffic
 * through this central server.
 *
 * Clients connect and can either:
 * - HOST a game server (becomes the authoritative server for a room)
 * - JOIN a game (connects as a player)
 *
 * Deploy to: Render, Railway, Fly.io, or any Node.js hosting
 */

const WebSocket = require('ws');

const PORT = process.env.PORT || 3000;

// Store active game rooms
// roomId -> { host: WebSocket, clients: Map<clientId, WebSocket> }
const rooms = new Map();

// Store all connections for cleanup
const connections = new Map();

// Generate unique IDs
let connectionCounter = 0;
function generateId() {
    return `conn_${Date.now()}_${++connectionCounter}`;
}

const wss = new WebSocket.Server({ port: PORT });

console.log(`BetweenUs Relay Server running on port ${PORT}`);

wss.on('connection', (ws) => {
    const connectionId = generateId();
    connections.set(connectionId, {
        ws: ws,
        type: null, // 'host' or 'client'
        roomId: null,
        playerName: null
    });

    console.log(`New connection: ${connectionId}`);

    ws.on('message', (data) => {
        try {
            const message = data.toString();
            handleMessage(connectionId, ws, message);
        } catch (e) {
            console.error('Error handling message:', e);
        }
    });

    ws.on('close', () => {
        handleDisconnect(connectionId);
    });

    ws.on('error', (err) => {
        console.error(`Connection ${connectionId} error:`, err.message);
        handleDisconnect(connectionId);
    });

    // Send welcome message with connection ID
    ws.send(JSON.stringify({
        type: 'welcome',
        connectionId: connectionId
    }));
});

function handleMessage(connectionId, ws, message) {
    // Try to parse as JSON first (relay protocol)
    try {
        const json = JSON.parse(message);
        handleRelayProtocol(connectionId, ws, json);
        return;
    } catch (e) {
        // Not JSON, treat as raw game data
    }

    // Forward raw game data based on connection type
    const conn = connections.get(connectionId);
    if (!conn || !conn.roomId) return;

    const room = rooms.get(conn.roomId);
    if (!room) return;

    if (conn.type === 'host') {
        // Host is sending to a specific client or broadcast
        // Format: clientId:message or just message for broadcast
        if (message.includes('|TO|')) {
            const [targetId, actualMessage] = message.split('|TO|');
            const targetConn = connections.get(targetId);
            if (targetConn && targetConn.ws.readyState === WebSocket.OPEN) {
                targetConn.ws.send(actualMessage);
            }
        } else {
            // Broadcast to all clients in room
            room.clients.forEach((clientWs, clientId) => {
                if (clientWs.readyState === WebSocket.OPEN) {
                    clientWs.send(message);
                }
            });
        }
    } else if (conn.type === 'client') {
        // Client sending to host - prepend their connection ID
        if (room.host && room.host.readyState === WebSocket.OPEN) {
            room.host.send(`${connectionId}|FROM|${message}`);
        }
    }
}

function handleRelayProtocol(connectionId, ws, json) {
    const conn = connections.get(connectionId);
    if (!conn) return;

    switch (json.type) {
        case 'host_room':
            // Game server wants to host a room
            hostRoom(connectionId, ws, json.roomId);
            break;

        case 'join_room':
            // Client wants to join a room
            joinRoom(connectionId, ws, json.roomId, json.playerName);
            break;

        case 'list_rooms':
            // Client wants list of available rooms
            listRooms(ws);
            break;

        case 'close_room':
            // Host wants to close their room
            closeRoom(connectionId);
            break;

        case 'game_data':
            // Forward game data (wrapped in JSON for specific targeting)
            forwardGameData(connectionId, json);
            break;

        case 'ping':
            ws.send(JSON.stringify({ type: 'pong', timestamp: Date.now() }));
            break;

        default:
            console.log(`Unknown message type: ${json.type}`);
    }
}

function hostRoom(connectionId, ws, roomId) {
    const conn = connections.get(connectionId);

    // Check if room already exists
    if (rooms.has(roomId)) {
        ws.send(JSON.stringify({
            type: 'error',
            message: 'Room already exists'
        }));
        return;
    }

    // Create new room
    rooms.set(roomId, {
        host: ws,
        hostId: connectionId,
        clients: new Map(),
        createdAt: Date.now()
    });

    conn.type = 'host';
    conn.roomId = roomId;

    console.log(`Room created: ${roomId} by ${connectionId}`);

    ws.send(JSON.stringify({
        type: 'room_created',
        roomId: roomId
    }));
}

function joinRoom(connectionId, ws, roomId, playerName) {
    const conn = connections.get(connectionId);
    const room = rooms.get(roomId);

    if (!room) {
        ws.send(JSON.stringify({
            type: 'error',
            message: 'Room not found'
        }));
        return;
    }

    // Add client to room
    room.clients.set(connectionId, ws);
    conn.type = 'client';
    conn.roomId = roomId;
    conn.playerName = playerName;

    console.log(`Client ${connectionId} (${playerName}) joined room ${roomId}`);

    // Notify client
    ws.send(JSON.stringify({
        type: 'joined_room',
        roomId: roomId
    }));

    // Notify host of new client
    if (room.host && room.host.readyState === WebSocket.OPEN) {
        room.host.send(JSON.stringify({
            type: 'client_joined',
            clientId: connectionId,
            playerName: playerName
        }));
    }
}

function listRooms(ws) {
    const roomList = [];
    rooms.forEach((room, roomId) => {
        roomList.push({
            roomId: roomId,
            playerCount: room.clients.size + 1, // +1 for host
            createdAt: room.createdAt
        });
    });

    ws.send(JSON.stringify({
        type: 'room_list',
        rooms: roomList
    }));
}

function closeRoom(connectionId) {
    const conn = connections.get(connectionId);
    if (!conn || conn.type !== 'host') return;

    const room = rooms.get(conn.roomId);
    if (!room) return;

    // Notify all clients
    room.clients.forEach((clientWs, clientId) => {
        if (clientWs.readyState === WebSocket.OPEN) {
            clientWs.send(JSON.stringify({
                type: 'room_closed'
            }));
        }
        const clientConn = connections.get(clientId);
        if (clientConn) {
            clientConn.roomId = null;
            clientConn.type = null;
        }
    });

    console.log(`Room closed: ${conn.roomId}`);
    rooms.delete(conn.roomId);
    conn.roomId = null;
    conn.type = null;
}

function forwardGameData(connectionId, json) {
    const conn = connections.get(connectionId);
    if (!conn || !conn.roomId) return;

    const room = rooms.get(conn.roomId);
    if (!room) return;

    const gameData = json.data;

    if (conn.type === 'host') {
        // Host sending to specific client or all
        if (json.targetId) {
            const targetConn = connections.get(json.targetId);
            if (targetConn && targetConn.ws.readyState === WebSocket.OPEN) {
                targetConn.ws.send(gameData);
            }
        } else {
            // Broadcast to all clients
            room.clients.forEach((clientWs) => {
                if (clientWs.readyState === WebSocket.OPEN) {
                    clientWs.send(gameData);
                }
            });
        }
    } else if (conn.type === 'client') {
        // Client sending to host
        if (room.host && room.host.readyState === WebSocket.OPEN) {
            // Include sender info
            room.host.send(JSON.stringify({
                type: 'client_data',
                clientId: connectionId,
                data: gameData
            }));
        }
    }
}

function handleDisconnect(connectionId) {
    const conn = connections.get(connectionId);
    if (!conn) return;

    console.log(`Disconnected: ${connectionId}`);

    if (conn.roomId) {
        const room = rooms.get(conn.roomId);
        if (room) {
            if (conn.type === 'host') {
                // Host disconnected - close the room
                closeRoom(connectionId);
            } else if (conn.type === 'client') {
                // Client disconnected - remove from room
                room.clients.delete(connectionId);

                // Notify host
                if (room.host && room.host.readyState === WebSocket.OPEN) {
                    room.host.send(JSON.stringify({
                        type: 'client_left',
                        clientId: connectionId,
                        playerName: conn.playerName
                    }));
                }
            }
        }
    }

    connections.delete(connectionId);
}

// Periodic cleanup of stale rooms
setInterval(() => {
    const now = Date.now();
    const maxAge = 3600000; // 1 hour

    rooms.forEach((room, roomId) => {
        if (now - room.createdAt > maxAge && room.clients.size === 0) {
            console.log(`Cleaning up stale room: ${roomId}`);
            rooms.delete(roomId);
        }
    });
}, 300000); // Check every 5 minutes

// Keep alive ping
setInterval(() => {
    wss.clients.forEach((ws) => {
        if (ws.readyState === WebSocket.OPEN) {
            ws.ping();
        }
    });
}, 30000);

console.log('Relay server ready for connections!');
