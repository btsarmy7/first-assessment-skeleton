package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private  ConcurrentHashMap<String, PrintWriter> userMap; // map that contains all connected users
	
	public ClientHandler(Socket socket, ConcurrentHashMap<String, PrintWriter> uMap) {
		super();
		this.socket = socket;
		this.userMap = uMap;
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
			String timestamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + ":";
			
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				/*
				 * check if the command is @username
				 */
				String command = message.getCommand();
				String cmd = command;
				if( command.charAt(0) == '@' ) 
					cmd = String.valueOf(command.charAt(0));
				
				switch (cmd) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						message.setContents(timestamp + " <" + message.getUsername() + "> has connected");
						userMap.put(message.getUsername(), writer);
				//log.info("size = " + userMap.size() );
						// ---- sent connect message to all except the sender
						for (String user : userMap.keySet()) {
				
							// ---- skip the sender, so as to only send the message to everyone else
							if (user.equals(message.getUsername())) {
							} else {
								PrintWriter sendWriter = userMap.get(user);
								String connectUserSent = mapper.writeValueAsString(message);
								sendWriter.write(connectUserSent);
								sendWriter.flush();
							}
						}
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						message.setContents(timestamp + " <" + message.getUsername() + "> has disconnected");
						userMap.put(message.getUsername().toString(), writer);
						
						// ---- tell other user this user disconnected 
						for (String user : userMap.keySet()) {
							// ---- skip the disconnected user
							if (user.equals(message.getUsername())) {
							} else {
								PrintWriter sendWriter = userMap.get(user);
								String disconnectUserSent = mapper.writeValueAsString(message);
								sendWriter.write(disconnectUserSent);
								sendWriter.flush();
							}
						}
				//log.info("****************** dis");
						this.socket.close();
						// ---- drop the disconnected user
						userMap.remove(message.getUsername());
						break;
					case "echo":
						log.info("user <{}> echo message <{}>", message.getUsername(), message.getContents());
						message.setContents((timestamp + " <" + message.getUsername() + "> (echo): " + message.getContents()));
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						log.info("user <{}> sent a broadcast message <{}>", message.getUsername(), message.getContents());
						message.setContents(timestamp + " <" + message.getUsername() + "> (all): " + message.getContents());
				//log.info("****************** bdcast");
						log.info("size = " + userMap.size() );
						//---- inform every user except the sender
						for (String user : userMap.keySet()) {
							log.info("user = " + user + "received broadcasted message");
							if (user.equals(message.getUsername())) {
							} else {
								PrintWriter broadcastWriter = userMap.get(user);
								String broadcastSent = mapper.writeValueAsString(message);
								broadcastWriter.write(broadcastSent);
								broadcastWriter.flush();
							}
						}
						break;
					case "@":
						log.info("user <{}> sent <{}> a message <{}>", message.getUsername(), message.getCommand(), message.getContents());
						// ---- get the receiver
						String theReceiver = command.substring(1);
						//System.out.println(command.substring(1));
						message.setContents(timestamp + " <" + message.getUsername() + "> (whisper): " + theReceiver + " : " + message.getContents());
						
						// ---- check if the receiver exists and then sent the message 
						for (String user : userMap.keySet()) {
							if (/*user.equals(message.getUsername()) &&*/  user.equals(theReceiver)) {
								PrintWriter whispWriter = userMap.get(user);
								String WhispSent = mapper.writeValueAsString(message);
								whispWriter.write(WhispSent);
								whispWriter.flush();
							}
						}
						break;
					case "users":
						// ---- display all connected users
						log.info("user <{}> requested for all users online", message.getUsername());
						String userNames = getOnlineUsers();
						log.info("users: ", userNames);
						message.setContents(timestamp + " currently connected users: \n"  + userNames);
						String responseUsers = mapper.writeValueAsString(message);
						writer.write(responseUsers);
						writer.flush();
						break;
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	/**
	 *  get users on line
	 * @param userMap
	 * @return
	 */
	public String getOnlineUsers() {
		String users = "";
		for(Map.Entry<String, PrintWriter> entry : userMap.entrySet() ) {
				users = users + "<"+ entry.getKey() + ">" + "\n";
	//log.info( "users: " + users );
		}

		return users;
	}
}

