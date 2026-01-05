package com.mmog;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Stack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.mmog.players.CrewMember;
import com.mmog.players.Imposter;
import com.mmog.players.Player;
import com.mmog.screens.GameScreen;
import com.mmog.screens.JoinScreen;
import com.mmog.screens.LobbyScreen;
import com.mmog.screens.MainScreen;
import com.mmog.screens.RoleUI;
import com.mmog.screens.ScreenEnum;
import com.mmog.screens.ScreenManager;
import com.mmog.tasks.EmergencyMeeting;
import com.mmog.tasks.ReactorTask;

import java.io.*;
public class Client
{
	static int port = 7077;
	static DatagramSocket socket;
	static InetAddress address;
	private static Player player;
	private static ArrayList<Player> players;
	static float waitTime = 0;

	// Relay mode flag
	private static boolean useRelayMode = false;
	private static boolean relayConnected = false;

	public Client() throws IOException {
		players = new ArrayList<Player>();
		initSockets();
	}

	public static void initSockets() {
		// Load configuration
		GameConfig.loadConfig();
		useRelayMode = GameConfig.useRelay();
		port = GameConfig.getServerPort();

		if (useRelayMode) {
			// Connect to relay server
			System.out.println("Using relay mode: " + GameConfig.getRelayUrl());
			RelayClient.connect(GameConfig.getRelayUrl(), (connected) -> {
				relayConnected = connected;
				if (connected) {
					System.out.println("Connected to relay server!");
					// Set up game data callback
					RelayClient.getInstance().setGameDataCallback(data -> {
						try {
							parseCommand(data);
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				} else {
					System.out.println("Failed to connect to relay, falling back to direct mode");
					useRelayMode = false;
					initDirectSockets();
				}
			});
		} else {
			initDirectSockets();
		}
	}

	/**
	 * Initialize direct UDP sockets (original behavior)
	 */
	private static void initDirectSockets() {
		socket = null;
		try
		{
			// Use configurable server address
			String serverHost = GameConfig.getServerHost();
			address = InetAddress.getByName(serverHost);
			System.out.println("Connecting to server: " + serverHost + ":" + port);

			socket = new DatagramSocket(8000);
		}
		catch (IOException e)
		{
			System.out.println(e);
			System.exit(1);
		}

		UDPEchoReader reader = new UDPEchoReader(socket);
		reader.setDaemon(true);
		reader.start();
	}

	/**
	 * Check if using relay mode
	 */
	public static boolean isUsingRelay() {
		return useRelayMode && relayConnected;
	}

	/**
	 * Send data - routes through relay or direct UDP based on mode
	 */
	private static void sendData(String data) throws IOException {
		if (isUsingRelay()) {
			RelayClient.getInstance().sendGameData(data);
		} else {
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
			socket.send(packet);
		}
	}

	public static ArrayList<Player> getPlayers(){
		return players;
	}

	public static void replacePlayerByRole(Batch batch) {
		if(!player.role.equals("none")) {
			//System.out.println("ROLE ASSIGNED!");
			
			String name = getPlayer().getPlayerName();
			String roomName = getPlayer().connectedRoomName;
			int hatID = getPlayer().getHatID();
			
			if(player.role.equals("CrewMember")) {
				player = new CrewMember(getPlayer().getPlayerID());
				player.setPlayerName(name);
				player.connectedRoomName = roomName;
				player.setHatID(hatID);
				//start the game, roles are assigned.
				//draw the banner for crew member	
				ScreenManager.getInstance().showScreen(ScreenEnum.ROLE_UI);
			}
			else if(Client.getPlayer().role.equals("Imposter")) {
				player = new Imposter(getPlayer().getPlayerID());
				player.setPlayerName(name);
				player.connectedRoomName = roomName;
				player.setHatID(hatID);
				//start the game, roles are assigned.
				ScreenManager.getInstance().showScreen(ScreenEnum.ROLE_UI);
			}

			player.setPlayerName(player.getPlayerName());
		}
	}

	public static class UDPEchoReader extends Thread
	{
		public UDPEchoReader(DatagramSocket socket)
		{
			this.datagramSocket = socket;
			active = true;
		}
		public void run()
		{
			byte[] buffer = new byte[1024];
			DatagramPacket incoming = new DatagramPacket(buffer,
					buffer.length);
			while(active)
			{
				try
				{
					// listen for incoming datagram packet
					datagramSocket.receive(incoming);
					// print out received string
					String received = new String(incoming.getData(),incoming.getOffset(),incoming.getLength());
					parseCommand(received);
				}
				catch(IOException e)
				{
					System.out.println(e);
					active = false;
				}
			}
		}
		public boolean active;
		public DatagramSocket datagramSocket;
	}


	public static void connectClientToServer() throws IOException {
		//command int for connection request.
		String input = "0" + ",";
		input += player.getPlayerName() + ",";
		input += player.connectedRoomName;

		// If using relay, join the room first
		if (isUsingRelay()) {
			RelayClient.getInstance().joinRoom(player.connectedRoomName, player.getPlayerName());
		}

		sendData(input);
	}

	public static void sendUpdate(float x, float y, boolean isFlipped, boolean isDead, boolean isIdle, int hatID, boolean hatIsFlipped) throws Exception
	{
		String toSend = "";
		toSend += 1 + ",";
		toSend += player.getPlayerName() + ",";
		toSend += player.connectedRoomName + ",";
		toSend +=  x +",";
		toSend +=  y +",";
		toSend +=  isFlipped + ",";
		toSend +=  isDead + ",";
		toSend +=  isIdle + ",";
		toSend +=  hatID + ",";
		toSend +=  hatIsFlipped;

		sendData(toSend);
	}
	
	public static void sendLightsCommand() throws IOException{
		String toSend = "";
		toSend += 11 + ",";
		toSend += player.connectedRoomName;

		sendData(toSend);
	}

	public static void sendLightsFixed() throws IOException{
		String toSend = "";
		toSend += 15 + ",";
		toSend += player.connectedRoomName;

		sendData(toSend);
	}

	public static void sendTriggerMeeting() throws IOException{
		String toSend = "";
		toSend += 18 + ",";
		toSend += player.connectedRoomName;

		sendData(toSend);
	}

	public static void sendInVent() throws IOException{
		String toSend = "";
		toSend += 12 + ",";
		toSend += player.connectedRoomName +",";
		toSend += player.getPlayerName();

		sendData(toSend);
	}

	public static void sendVote(boolean voted, String playerName) throws IOException{
		String toSend = "";
		toSend += 16 + ",";
		toSend += player.connectedRoomName +",";
		toSend += voted + ",";
		toSend += playerName;

		sendData(toSend);
	}

	public static void sendOutVent(float x, float y, boolean isFlipped, boolean isDead, boolean isIdle, int hatID, boolean hatIsFlipped) throws IOException{
		String toSend = "";
		toSend += 13 + ",";
		toSend += player.connectedRoomName + ",";
		toSend += player.getPlayerName() + ",";
		toSend +=  x +",";
		toSend +=  y +",";
		toSend +=  isFlipped + ",";
		toSend +=  isDead + ",";
		toSend +=  isIdle + ",";
		toSend += hatID + ",";
		toSend += hatIsFlipped;

		sendData(toSend);
	}

	public static void sendCreateRoomCommand(String roomName, float numCrew, float numImp) throws IOException {
		String toSend = "";
		toSend += 5 + ",";
		toSend += roomName +",";
		toSend += player.getPlayerName() + ",";
		toSend +=  numCrew +",";
		toSend +=  numImp;

		// If using relay, host the room first
		if (isUsingRelay()) {
			RelayClient.getInstance().hostRoom(roomName);
		}

		sendData(toSend);
	}

	public static void sendReactorTaskCompleted() throws IOException {
		String toSend = "";
		toSend += 9 + ",";
		toSend += Client.getPlayer().connectedRoomName;

		sendData(toSend);
	}

	public static void sendGetVotes() throws IOException {
		String toSend = "";
		toSend += 17 + ",";
		toSend += Client.getPlayer().connectedRoomName;

		sendData(toSend);
	}

	public static void sendPlayerKilled(String playerName) throws IOException {
		String toSend = "";
		toSend += 10 + ",";
		toSend += getPlayer().connectedRoomName + ",";
		toSend += playerName;

		sendData(toSend);
	}

	public static void sendReactorCommand() throws IOException{
		String toSend = "";
		toSend += 14 + ",";
		toSend += Client.getPlayer().connectedRoomName + ",";
		toSend += getPlayer().getPlayerName();

		sendData(toSend);
	}

	public static void sendStartCommand() throws IOException {
		String toSend = "";
		toSend += 3 + ",";
		toSend += Client.getPlayer().connectedRoomName +",";

		sendData(toSend);
	}

	public static void sendImpostersWin() throws IOException {
		String toSend = "";
		toSend += 22 + ",";
		toSend += Client.getPlayer().connectedRoomName +",";

		sendData(toSend);
	}

	public static void sendCompletedAllTasks() throws IOException{
		String toSend = "";
		toSend += 20 + ",";
		toSend += Client.getPlayer().connectedRoomName +",";
		toSend += getPlayer().getPlayerName();

		sendData(toSend);
	}

	public static void sendRefreshCommand() throws IOException {
		String toSend = "";
		toSend += 6 + ",";
		toSend += player.getPlayerName();

		System.out.println(toSend);
		sendData(toSend);
	}

	public static void sendMessageCommand(String message) throws IOException {
		String toSend = "";
		toSend += 19 + ",";
		toSend += player.getPlayerName() + ",";
		toSend += player.connectedRoomName + ",";
		toSend += message;

		System.out.println(toSend);
		sendData(toSend);
	}

	public static void createPlayer(String name) {
		player = new Player(-1);
		player.setPlayerName(name);
	}

	//create all the players in the connected players array in the client
	public static void createPlayers() {
		for(int i= 0; i < 8; i ++) {
			players.add(new Player(-1));
		}
	}

	public static Player getPlayer() {
		return player;
	}

	public static void updateConnectedClient(int playerID, float x, float y, boolean isFlipped, boolean isDead, boolean isIdle, int hatID,boolean hatIsFlipped) {
		for(Player p: players) {
			if(p.getPlayerID() == playerID) {
				p.setAll(x, y, isFlipped, isDead, isIdle, hatID, hatIsFlipped);
				return;
			}
		}
	}

	public static boolean removeClient() {
		String toSend = "";
		toSend += "4" + ",";
		toSend += player.connectedRoomName + ",";
		toSend += getPlayer().getPlayerName();

		System.out.println(toSend);

		//clears all the local players information from last game
		for(Player p: players) {
			p.clearAll();
		}

		try {
			sendData(toSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ScreenManager.getInstance().showScreen(ScreenEnum.MAIN_MENU);
		return true;
	}

	/*
	 * This parseCommand method is called when the Client receives a datagram packet from the server (byte array)
	 */
	public static void parseCommand(String received) throws IOException
	{
		System.out.println(received);

		String[] dataArray = parseData(received);

		int size = dataArray.length;
		Integer command = Integer.parseInt(dataArray[size-1]);

		if (command == 0)//connect command
		{
			Stack<Integer> playerIDs = new Stack<Integer>();
			Stack<String> playerNames = new Stack<String>();

			for(int i = 0; i < size-1;i+=2) {
				int playerID = Integer.parseInt(dataArray[i]);
				String playerName = dataArray[i+1];

				playerIDs.push(playerID);
				playerNames.push(playerName);

				System.out.println("Connected with @ClientID: " + playerID + " @Name:" + playerName);
			}

			for(Player p: players) 
			{
				if (playerIDs.isEmpty())
				{
					break;
				}
				
				System.out.println("about to set player...");
				if(p.getPlayerID() == -1) {
					p.setPlayerID(playerIDs.pop());
					p.setPlayerName(playerNames.pop());
					System.out.println("Settting player: @playerID: " + p.getPlayerID() + "@playerName: " + p.getPlayerName());
				}
			}
		}
		else if (command == 1)//update command
		{
			//System.out.println(receivedData);
			float x = Float.valueOf(dataArray[0]);
			float y = Float.valueOf(dataArray[1]);
			boolean isFlipped = Boolean.parseBoolean(dataArray[2]);
			boolean isDead = Boolean.parseBoolean(dataArray[3]);
			boolean isIdle = Boolean.parseBoolean(dataArray[4]);
			int hatID = Integer.parseInt(dataArray[5]);
			boolean hatIsFlipped = Boolean.parseBoolean(dataArray[6]);
			int playerID = Integer.parseInt(dataArray[7]);

			updateConnectedClient(playerID, x, y, isFlipped, isDead, isIdle, hatID, hatIsFlipped);
		}
		else if(command == 3) {
			String role = dataArray[0];
			addRoleToPlayer(role);
		}
		else if(command == 4) {
			String name = dataArray[0];
			int playerID = -1;

			for(Player p: players) {
				if(p.getPlayerName().equals(name)) {
					playerID = p.getPlayerID();
				}
			}
			removePlayerWithID(playerID);
		}
		else if(command == 5) {
			//server has returned the created room's host name and the room name
			String roomName = dataArray[0];
			String hostName = dataArray[1];

			System.out.println("Server added @Room:" + roomName + " @host:" + hostName);
		}
		else if(command == 6) {
			System.out.println("Room Response");
			if(dataArray.length == 1) {
				System.out.println("Reply from Server: No Rooms Available");
			}
			else {
				for(int i = 0; i < size - 1; i ++) {
					JoinScreen.addRoom(dataArray[i]);
				}
			}
		}
		else if(command == 9) {
			boolean reactorTaskCompleted = Boolean.parseBoolean(dataArray[0]);
			ReactorTask.setCompletedTask(reactorTaskCompleted);
		}
		else if(command == 10) {
			String pname = dataArray[0];
			if(getPlayer().getPlayerName().equals(pname)) {
				getPlayer().justKilled = true;
				getPlayer().elapsedTime = 0;
				getPlayer().isDead = true;
			}else {
				for(Player p: players) {
					if(p.getPlayerName().equals(pname)) {
						p.elapsedTime = 0;
						p.justKilled = true;
						p.isDead = true;
					}
				}
			}
		}
		//reduce vision for crew member player
		else if(command == 11) {
			GameScreen.light.setDistance(50);
		}
		//vent command
		else if(command == 12) {
			String ventedPlayer = dataArray[0];
			Boolean vented = Boolean.parseBoolean(dataArray[1]);
			
			for(Player p: players) {
				if(p.getPlayerName().equals(ventedPlayer)) {
					p.inVent = vented;
				}
			}
		}
		//vent out command
		else if(command == 13) {
			String ventedPlayer = dataArray[0];
			Boolean vented = Boolean.parseBoolean(dataArray[1]);
			
			for(Player p: players) {
				if(p.getPlayerName().equals(ventedPlayer)) {
					p.inVent = vented;
					p.setAll(Float.parseFloat(dataArray[2]),Float.parseFloat(dataArray[3]), Boolean.parseBoolean(dataArray[4]), Boolean.parseBoolean(dataArray[5]), Boolean.parseBoolean(dataArray[6]), Integer.parseInt(dataArray[7]),  Boolean.parseBoolean(dataArray[8]));
				}
			}
		}
		//add a reactor task
		else if(command == 14) {
			GameScreen.reactorTaskStarted = true;
		}
		//lights fixed command
		else if(command == 15) {
			GameScreen.light.setDistance(180);
			
			if(Client.getPlayer() instanceof CrewMember) {
				if(((CrewMember) Client.getPlayer()).getCurrentTask() != null) {
					if(((CrewMember) Client.getPlayer()).getCurrentTask().getTaskName().equals("Electrical Task")) {
						((CrewMember) Client.getPlayer()).setCurrentTask(null);
					}
				}
				((CrewMember) Client.getPlayer()).removeTask("Electrical Task");
			}
		}
		else if(command == 17) {
			for(int i = 0; i < size-1;i+=2) {
				String playerName = dataArray[i];
				Integer numOfVotes = Integer.parseInt(dataArray[i+1]);
				
				System.out.println("Recevied a vote:" + "@playername:" + playerName + "@votes:" + numOfVotes);
				
				if(Client.getPlayer() instanceof CrewMember) {
					((EmergencyMeeting)((CrewMember) Client.getPlayer()).getCurrentTask()).addVote(playerName, numOfVotes);
				}
				else if(Client.getPlayer() instanceof Imposter) {
					((EmergencyMeeting)((Imposter) Client.getPlayer()).getCurrentTask()).addVote(playerName, numOfVotes);
				}
				
			}

		}
		//trigger meeting
		else if(command == 18) {
			GameScreen.meetingTriggered = true;
			
			if(Client.getPlayer() instanceof CrewMember) {
				((CrewMember) Client.getPlayer()).setCurrentTask("Emergency Meeting");
			}
			else if(Client.getPlayer() instanceof Imposter) {
				((Imposter) Client.getPlayer()).setCurrentTask("Emergency Meeting");
			}
		}
		
		//receive message
		else if(command == 19) {
			System.out.println("Received Message");
			
			String playerName = dataArray[0];
			String message = dataArray[1];
			
			if(Client.getPlayer() instanceof CrewMember) {
				((EmergencyMeeting)((CrewMember) Client.getPlayer()).getCurrentTask()).updateMessage(playerName, message);
			}
			else if(Client.getPlayer() instanceof Imposter) {
				((EmergencyMeeting)((Imposter) Client.getPlayer()).getCurrentTask()).updateMessage(playerName, message);
			}
		}
		//received end game logic
		else if(command == 21) {
			boolean crewWin = Boolean.parseBoolean(dataArray[0]);
			boolean impostersWin = Boolean.parseBoolean(dataArray[1]);
			
			System.out.println("Crew Members:" + crewWin + "Imposters:" + impostersWin);
			
			//we can switch screens yaaay
			GameScreen.crewWin = crewWin;
			GameScreen.imposterWin = impostersWin;
		}
	}

	public static Player getPlayerWithID(int playerID) {
		Player player = null;
		for(Player p: players) {
			if(p.getPlayerID() == playerID) {
				player = p;
			}
		}
		return player;
	}

	public static void addPlayerWithID(int playerID) {
		for(Player p: players) {
			if(p.getPlayerID() == -1) {
				p.setPlayerID(playerID);
			}
		}
	}

	public static void addRoleToPlayer(String role) {
		player.role = role;
	}

	public static void removePlayerWithID(int playerID) {
		getPlayerWithID(playerID).setPlayerID(-1);
	}

	public static String[] parseData(String receivedData) throws IOException {
		String dataArray[] = receivedData.split(",");
		return dataArray;
	}

} 