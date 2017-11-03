package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);
	
	/*
	 * Each server has a hashmap to hold clients connected it
	 * initiate user hashmap with some values to save memory?
	 */
	private ConcurrentHashMap<String, PrintWriter> userMap = null; // = new ConcurrentHashMap<String, PrintWriter>(8, 0.9f, 1);
	
	private int port;
	private ExecutorService executor;
	
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
		this.userMap = new ConcurrentHashMap<String, PrintWriter>(8, 0.9f, 1);;
	}

	public void run() {
		log.info("server started");
		ServerSocket ss;
		try {
			ss = new ServerSocket(this.port);
			
			while (true) {
				Socket socket = ss.accept();
				ClientHandler handler = new ClientHandler(socket, this.userMap);
				executor.execute(handler);
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
