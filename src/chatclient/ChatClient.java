/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatclient;

import java.io.DataOutputStream;
import java.io.IOException;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Date;
import java.sql.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * @version 0.0.1
 * @author Rocchi Francesco
 */
public final class ChatClient {
	private MessagesReceiver mr;
	private InetAddress serverHost;
	private int serverPort;
	private Socket socket;
	private GUI gui;
	private final JSONParser parser;
	private String username;
	private Date date;
	private Time time;
	private DataOutputStream dos;
	private final Vector users;
	private final Map<String, ArrayList<String>> usersAndMessages;

	public ChatClient() {
		parser = new JSONParser();
		try {
			serverHost = InetAddress.getLocalHost();	//InetAddress.getByName("192.168.1.101");	//InetAddress.getByName("strowrook.duckdns.org");	// Da modificare, mettendo l'indirizzo del server
			serverPort = 9989;
			socket = new Socket(serverHost, serverPort);
			dos = new DataOutputStream(socket.getOutputStream());
		} catch (ConnectException ex) {
			JOptionPane.showMessageDialog(gui, "Server non raggiungibile\nRiprovare in un secondo momento", "Errore! Server non raggiungibile!", JOptionPane.ERROR_MESSAGE);
			Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
		}
		users = new Vector();
		usersAndMessages = new HashMap<>();
		gui = new GUI(this);
	}

	public Map<String, ArrayList<String>> getUsersAndMessages() {
		return usersAndMessages;
	}
	
	public void parse(String message) {
		try {	
			if(!message.isEmpty()) {
				JSONObject root = (JSONObject) parser.parse(message);
				switch((String) root.get("type")) {
					case "text":
						String sender = (String) root.get("sender");
						if(!usersAndMessages.containsKey(sender)) {			// Un utente ha iniziato una nuova conversazione
							usersAndMessages.put(sender, new ArrayList<>());	// Salva la nuova conversazione
							users.add(sender);						// Salva il nuovo utente
							gui.setConversationsList(users);				// Aggiorna la lista
						}
						usersAndMessages.get(sender).add(message);
						gui.printAsAddressee((String) root.get("text"), sender);
						break;
					case "search":
						if(((String) root.get("status")).equals("ok")) {
							String user = (String) root.get("username");
							usersAndMessages.put(user, new ArrayList<>());
							users.add(user);
							gui.setConversationsList(users);
						} else
							JOptionPane.showMessageDialog(gui, "Mi dispiace, utente non esistente", "Utente non esistente!", JOptionPane.ERROR_MESSAGE);
				}
			}
		} catch (ParseException ex) {
			Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public boolean login(String username, String password) {
		try {
			mr = new MessagesReceiver(socket, this);
			JSONObject root = new JSONObject();
			root.put("password", SHA1Cipher(password));
			root.put("username", username);
			root.put("type", "login");
			sendMessage(root.toJSONString());
			root.clear();
			
			root = (JSONObject) parser.parse(mr.receiveMessage());
			String status = (String) root.get("status");
			if(status.equals("ok")) {
				this.username = username;
				JSONArray messages = (JSONArray) parser.parse((String) root.get("messages"));
				JSONObject singleMessage;
				
				String sender, addressee;
				for(Iterator it = messages.iterator(); it.hasNext();) {
					singleMessage = (JSONObject) it.next();
					sender = (String) singleMessage.get("sender");
					addressee = (String) singleMessage.get("addressee");
					if(addressee.equals(username)) {
						if(!usersAndMessages.containsKey(sender)) {
							usersAndMessages.put(sender, new ArrayList<>());
							users.add(sender);
						}
						usersAndMessages.get(sender).add(singleMessage.toJSONString());
					} else {
						if(!usersAndMessages.containsKey(addressee)){
							usersAndMessages.put(addressee, new ArrayList<>());
							users.add(addressee);
						}
						usersAndMessages.get(addressee).add(singleMessage.toJSONString());
					}
				}
				
				gui.setConversationsList(users);
				
				mr.start();
				return true;
			}
		} catch (ParseException ex) {
			Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}
	
	private void sendMessage(String message) {
		try {
			dos.writeBytes(message + "\n");
		} catch (IOException ex) {
			Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.out.println("INVIATO: " + message);
	}
	
	public void prepareAndSendMessage(String text) {
		JSONObject root = new JSONObject();
		// Riempie l'oggetto JSON con i rispettivi valori
		root.put("type", "text");
		root.put("text", text);
		root.put("sender", username);
		root.put("addressee", gui.getSelectedUser());
		date = new Date(System.currentTimeMillis());
		root.put("date", date.toString());
		time = new Time(date.getTime());
		root.put("time", time.toString());
		// Invia il messaggio in formato stringa
		sendMessage(root.toJSONString());
		// Pulisce l'oggetto
		root.clear();
	}
	
	public void searchUser(String userToSearch) {
		if(!users.contains(userToSearch)) {
			JSONObject root = new JSONObject();
			root.put("type", "search");
			root.put("username", userToSearch);
			root.put("searcher", username);
			sendMessage(root.toJSONString());
		} else
			JOptionPane.showMessageDialog(gui, "Hai già iniziato una conversazione con " + userToSearch, "Chat già avviata", JOptionPane.PLAIN_MESSAGE);
	}
	
	private String SHA1Cipher(String str) {
		try {
			// Si crea un oggetto MessageDigest e gli si assegna l'oggetto che implementa l'algoritmo SHA-1
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			// Si aggiorna il contenuto dell'oggetto md con il contenuto della stringa su cui vogliamo
			// effettuare l'hash
			md.update(str.getBytes());
			// Si completa l'elaborazione hash, ottenendo un insieme di bytes
			byte[] digest = md.digest();
			// Si crea di nuovo la stringa per poter visualizzare il risultato
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new ChatClient();
	}
	
	/*
	Da CHIUDERE ABBESTIA:
		private Socket socket;
		private OutputStream os;
		private DataOutputStream dos;
	*/
}
