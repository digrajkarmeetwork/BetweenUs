package com.mmog;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * WebSocket client for connecting to the relay server.
 * This allows playing over the internet without port forwarding.
 */
public class RelayClient extends WebSocketClient {

    private static RelayClient instance;
    private String connectionId;
    private String currentRoomId;
    private boolean isHost = false;
    private boolean isConnected = false;

    // Queue for incoming game messages
    private ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();

    // Callback for when connection is established
    private Consumer<Boolean> connectionCallback;

    // Callback for game data received
    private Consumer<String> gameDataCallback;

    private RelayClient(URI serverUri) {
        super(serverUri);
    }

    /**
     * Initialize and connect to relay server
     */
    public static void connect(String relayUrl, Consumer<Boolean> onConnected) {
        try {
            if (instance != null && instance.isOpen()) {
                instance.close();
            }

            URI uri = new URI(relayUrl);
            instance = new RelayClient(uri);
            instance.connectionCallback = onConnected;
            instance.connect();
            System.out.println("Connecting to relay server: " + relayUrl);
        } catch (Exception e) {
            System.err.println("Failed to connect to relay: " + e.getMessage());
            if (onConnected != null) {
                onConnected.accept(false);
            }
        }
    }

    /**
     * Get the singleton instance
     */
    public static RelayClient getInstance() {
        return instance;
    }

    /**
     * Check if connected to relay
     */
    public static boolean isConnected() {
        return instance != null && instance.isOpen() && instance.isConnected;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to relay server!");
        isConnected = true;
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Relay message: " + message);

        // Try to parse as JSON protocol message
        if (message.startsWith("{")) {
            handleProtocolMessage(message);
        } else {
            // Raw game data - queue it for processing
            messageQueue.offer(message);
            if (gameDataCallback != null) {
                gameDataCallback.accept(message);
            }
        }
    }

    private void handleProtocolMessage(String json) {
        // Simple JSON parsing without external library
        if (json.contains("\"type\":\"welcome\"")) {
            // Extract connection ID
            int start = json.indexOf("\"connectionId\":\"") + 16;
            int end = json.indexOf("\"", start);
            if (start > 15 && end > start) {
                connectionId = json.substring(start, end);
                System.out.println("Got connection ID: " + connectionId);
            }
            if (connectionCallback != null) {
                connectionCallback.accept(true);
            }
        } else if (json.contains("\"type\":\"room_created\"")) {
            System.out.println("Room created successfully!");
            isHost = true;
        } else if (json.contains("\"type\":\"joined_room\"")) {
            System.out.println("Joined room successfully!");
            isHost = false;
        } else if (json.contains("\"type\":\"room_list\"")) {
            // Parse room list - this would need proper JSON parsing
            // For now, just queue it
            messageQueue.offer(json);
        } else if (json.contains("\"type\":\"client_joined\"")) {
            // A new player joined our hosted room
            messageQueue.offer(json);
        } else if (json.contains("\"type\":\"client_left\"")) {
            // A player left our hosted room
            messageQueue.offer(json);
        } else if (json.contains("\"type\":\"client_data\"")) {
            // Game data from a client (we're the host)
            int dataStart = json.indexOf("\"data\":\"") + 8;
            int dataEnd = json.lastIndexOf("\"");
            if (dataStart > 7 && dataEnd > dataStart) {
                String data = json.substring(dataStart, dataEnd);
                // Unescape if needed
                data = data.replace("\\\"", "\"");
                messageQueue.offer(data);
                if (gameDataCallback != null) {
                    gameDataCallback.accept(data);
                }
            }
        } else if (json.contains("\"type\":\"room_closed\"")) {
            System.out.println("Room was closed by host");
            currentRoomId = null;
        } else if (json.contains("\"type\":\"error\"")) {
            System.err.println("Relay error: " + json);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Relay connection closed: " + reason);
        isConnected = false;
        currentRoomId = null;
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Relay error: " + ex.getMessage());
        if (connectionCallback != null && !isConnected) {
            connectionCallback.accept(false);
        }
    }

    /**
     * Host a new game room on the relay
     */
    public void hostRoom(String roomId) {
        if (!isOpen()) return;
        currentRoomId = roomId;
        send("{\"type\":\"host_room\",\"roomId\":\"" + roomId + "\"}");
    }

    /**
     * Join an existing room on the relay
     */
    public void joinRoom(String roomId, String playerName) {
        if (!isOpen()) return;
        currentRoomId = roomId;
        send("{\"type\":\"join_room\",\"roomId\":\"" + roomId + "\",\"playerName\":\"" + playerName + "\"}");
    }

    /**
     * Request list of available rooms
     */
    public void requestRoomList() {
        if (!isOpen()) return;
        send("{\"type\":\"list_rooms\"}");
    }

    /**
     * Send game data through the relay
     */
    public void sendGameData(String data) {
        if (!isOpen()) return;
        // Escape quotes in data
        String escaped = data.replace("\"", "\\\"");
        send("{\"type\":\"game_data\",\"data\":\"" + escaped + "\"}");
    }

    /**
     * Send game data to a specific client (host only)
     */
    public void sendGameDataTo(String targetId, String data) {
        if (!isOpen() || !isHost) return;
        String escaped = data.replace("\"", "\\\"");
        send("{\"type\":\"game_data\",\"targetId\":\"" + targetId + "\",\"data\":\"" + escaped + "\"}");
    }

    /**
     * Close the current room (host only)
     */
    public void closeRoom() {
        if (!isOpen() || !isHost) return;
        send("{\"type\":\"close_room\"}");
        currentRoomId = null;
    }

    /**
     * Get next message from queue
     */
    public String pollMessage() {
        return messageQueue.poll();
    }

    /**
     * Check if there are pending messages
     */
    public boolean hasMessages() {
        return !messageQueue.isEmpty();
    }

    /**
     * Set callback for game data
     */
    public void setGameDataCallback(Consumer<String> callback) {
        this.gameDataCallback = callback;
    }

    /**
     * Get connection ID
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Check if we're the host
     */
    public boolean isHost() {
        return isHost;
    }

    /**
     * Get current room ID
     */
    public String getCurrentRoomId() {
        return currentRoomId;
    }

    /**
     * Disconnect from relay
     */
    public static void disconnect() {
        if (instance != null && instance.isOpen()) {
            instance.close();
        }
        instance = null;
    }
}
