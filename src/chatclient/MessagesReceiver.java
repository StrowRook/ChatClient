/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.Socket;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rocchi Francesco
 */
public class MessagesReceiver extends Thread {
	private BufferedReader br;
	private final ChatClient chat;
	private String message;
	private Socket socket;

	public MessagesReceiver(Socket socket, ChatClient chat) {
		this.chat = chat;
		this.socket = socket;
		message = "";
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException ex) {
			Logger.getLogger(MessagesReceiver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public String receiveMessage() {
		try {
			return br.readLine();
		} catch (IOException ex) {
			Logger.getLogger(MessagesReceiver.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
	
	@Override
	public void run() {
		while (!socket.isClosed()) {
			message = receiveMessage();
			if(message != null)
				chat.parse(message);
			else
				break;
		}
	}
}
