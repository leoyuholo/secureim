package com.example.secureim;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.security.*;

import android.app.Activity;
import android.widget.Toast;

import com.example.secureim.MessageUtil.*;

public class MessageClient{
	
	static final String charSet = "utf-8";
	static final String defaultHost = "124.244.62.196";
	static final int defaultPort = 12345;
	
	static final int QUEUE_DEFAULT_SIZE = 64;
	
	Socket socket = null;
	PubPriKeyPair keyPair = null;
	DataInputStream socketInputStream = null;
	DataOutputStream socketOutputStream = null;
	PublicKey serverPublicKey = null;
	
	public String userName = null;
	
	public ChatMainActivity chatMainActivity = null;

	public ContactListMainActivity contactListMainActivity = null;
	
	protected ArrayBlockingQueue<byte[]> packetQueue = null;
	
	public MessageClient(){
		
		try{
			
			socket = new Socket(defaultHost, defaultPort);
			
			socketInputStream = new DataInputStream(socket.getInputStream());
			socketOutputStream = new DataOutputStream(socket.getOutputStream());
			
			// generate public/private key pair
			keyPair = MessageUtil.genPubPriKeyPair();
			
			// receive server public key
			byte[] receivedContent = MessageUtil.readPacket(socketInputStream);
			
			serverPublicKey = MessageUtil.bytesToPublicKey(receivedContent);
			
			// send encrypted client public key to server
			MessageUtil.sendEncryptedPacket(socketOutputStream, MessageUtil.keyToBytes(keyPair.publicKey), serverPublicKey);
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
	}
	
	private void sendPacket(byte[] packet){
		
		MessageUtil.sendEncryptedPacket(socketOutputStream, packet, serverPublicKey);
		
	}
	
	public boolean login(String user, String password){
		
		LoginPacket loginPacket = new LoginPacket(user, password);
		
		sendPacket(loginPacket.packet);
		
		// receive responds from server
		byte[] receivedContent = MessageUtil.readDecryptedPacket(socketInputStream, keyPair.privateKey);
		
		// check responds
		if(receivedContent[0] == MessageUtil.LOGIN_ACCEPT){
			
			userName = user;
			
			packetQueue = new ArrayBlockingQueue<byte[]>(QUEUE_DEFAULT_SIZE);
			
			new Thread(new MessageReceiver()).start();
			
			return true;
			
		}
		
		return false;
		
	}
	
	public boolean logout(){
		
		try{
			
			socket.close();
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		return true;
		
	}
	
	public boolean addContact(String contact){
		
		ContactAddPacket contactAddPacket = new ContactAddPacket(contact);
		
		sendPacket(contactAddPacket.packet);
		
		// receive responds from server
		byte[] receivedPacket = null;
		
		try{
			
			receivedPacket = packetQueue.take();
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		// check responds
		if(receivedPacket[0] == MessageUtil.CONTACT_ADD_ACCEPT){
			
			return true;
			
		}
		
		return false;
		
	}
	
	public ArrayList<String> getContact(){
		
		MessageUtil.sendEncryptedPacket(socketOutputStream, new ReplyPacket(MessageUtil.CONTACT_GET).packet, serverPublicKey);
		
		// receive responds from server
		byte[] receivedPacket = null;
		
		try{
			
			receivedPacket = packetQueue.take();
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		// check responds
		if(receivedPacket[0] == MessageUtil.CONTACT_GET_ACCEPT){
			
			ContactGetPacket contactGetPacket = new ContactGetPacket(receivedPacket);
			
			return new ArrayList<String>(contactGetPacket.contactList);
			
		}else{
			
			return new ArrayList<String>(0);
			
		}
		
	}
	
	public boolean sendMessage(String[] receiverList, String text){
		
		Message message = new Message(userName, receiverList, text);
		
		sendPacket(message.packet);
		
		// receive responds from server
		byte[] receivedPacket = null;
		
		try{
			
			receivedPacket = packetQueue.take();
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		// check responds
		if(receivedPacket[0] == MessageUtil.MESSAGE_ACCEPT){
			
			return true;
			
		}
		
		return false;
		
	}
	
	class Worker implements Runnable{
		
		String data;
		Activity activity;
		
		public Worker(String aString, Activity anActivity){
			
			data = aString;
			activity = anActivity;
			
		}
		
		public void run(){
			
			Toast.makeText(activity, "You get a message from " + data, Toast.LENGTH_SHORT).show();
			
		}
		
	}
	
	class MessageReceiver implements Runnable{
		
		public void run(){
			
			byte[] receivedPacket = null;
			
			while(true){
				
				receivedPacket = MessageUtil.readDecryptedPacket(socketInputStream, keyPair.privateKey);
				
				if(receivedPacket == null){
					
					break;
					
				}
				
				if(receivedPacket[0] == MessageUtil.MESSAGE){
					
					Message message = new Message(receivedPacket);
					
					ArrayList<String> receiverArrayList = new ArrayList<String>(Arrays.asList(message.receiverList));
					
					receiverArrayList.add(message.sender);
					
					String receiverListString = MessageUtil.getReceiverListString(receiverArrayList.toArray(new String[1]), userName);
					
					Chat chat = MainActivity.chatsHashMap.get(receiverListString);
					
					if(chat == null){
						
						chat = new Chat(message, userName);
						MainActivity.chatsHashMap.put(receiverListString, chat);
						
					}else{
						
						chat.addMessage(message);
						
					}
					
					if(chatMainActivity != null){
						if(receiverListString.equals(chatMainActivity.userNameString)){
							chatMainActivity.runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									chatMainActivity.refreshChatListView();
									
								}
							});
						}else{
							chatMainActivity.runOnUiThread(new Worker(message.sender, chatMainActivity));
						}
					}else{
						if(contactListMainActivity != null){
							contactListMainActivity.runOnUiThread(new Worker(message.sender, contactListMainActivity));
						}
					}
					
					/*if(receiverListString.equals(chatMainActivity.userNameString)){
						
						chatMainActivity.refreshChatListView();
						
					}*/
					
				}else{
					
					try{
						
						packetQueue.put(receivedPacket);
						
					}catch(Exception e){
						
						e.printStackTrace();
						
					}
				}
				
			}
			
		}
		
	}
	
}

