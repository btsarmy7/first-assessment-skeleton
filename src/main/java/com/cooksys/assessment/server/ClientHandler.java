package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	
	private ConcurrentHashMap<String, PrintWriter> connectedUsers; // map that contains all connected users
	

	public ClientHandler(Socket socket, ConcurrentHashMap<String, PrintWriter> CU) {
		super();
		this.socket = socket;
		this.connectedUsers = CU;
	}
	
			
	//returns list of connected users 
	public String getUsers() {
		String s = new String();
		for(String user : this.connectedUsers.keySet()) {
			s = s + " < " + user +  " > online" + "\n";
			} 
		return s;
	}
	

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			String currentTime = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + ":";

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				switch (message.getCommand()) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						connectedUsers.put(message.getUsername(), writer); // add to connectedUsers
						//log.info("user <{}> connected", connectedUsers.keySet());
						message.setContents(currentTime + " <" + message.getUsername() + "> has connected");
						
						/*String a = mapper.writeValueAsString(message);
						writer.write(a);
						writer.flush();*/
						// send message to all connected users
						for (String user : connectedUsers.keySet()) {
							/*if (user.equals(message.getUsername())) {

							} else {*/
								PrintWriter sendWriter = connectedUsers.get(user);
								String sendM = mapper.writeValueAsString(message);
								sendWriter.write(sendM);
								sendWriter.flush();
							//}
						}
						break; 
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						connectedUsers.remove(message.getUsername()); // remove from connectedUsers
						message.setContents(currentTime + " <" + message.getUsername() + "> has disconnected");
					
						/*String b = mapper.writeValueAsString(message);
						writer.write(b);
						writer.flush();*/
						// send message to all connected users
						for (String user : connectedUsers.keySet()) {
							/*if (user.equals(message.getUsername())) {

							} else {*/
								PrintWriter sendWriter = connectedUsers.get(user);
								String sendM = mapper.writeValueAsString(message);
								sendWriter.write(sendM);
								sendWriter.flush();
							//}
						}
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						message.setContents(currentTime + "<" + message.getUsername() + "> echoed: " + message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
						message.setContents(currentTime + " <" + message.getUsername() + "> broadcasted " + message.getContents()); 
						
						/*String c = mapper.writeValueAsString(message);
						writer.write(c);
						writer.flush();*/
						// send message to all connected users
						for (String user : connectedUsers.keySet()) {
							/*if (user.equals(message.getUsername())) {

							} else {*/
								PrintWriter sendWriter = connectedUsers.get(user);
								String sendM = mapper.writeValueAsString(message);
								sendWriter.write(sendM);
								sendWriter.flush();
							//}
						}
						break;
					case "@":
						log.info("user <{}> sent a message to ", message.getUsername());
						
						/*PrintWriter sendWriter = connectedUsers.get(user);
						String sendM = mapper.writeValueAsString(message);
						sendWriter.write(sendM);
						sendWriter.flush();*/
						break;
					case "users":
						log.info("user <{}> requested for the currently connected users", message.getUsername());
						message.setContents(currentTime + " Currently connected users: \n " + getUsers());
						String d = mapper.writeValueAsString(message);
						writer.write(d);
						writer.flush();
						break;
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	

}

