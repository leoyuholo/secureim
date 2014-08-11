import message.MessageUtil;
import message.MessageUtil.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.sql.*;
import java.security.*;

public class MessageServer{
	
	static final int port = 12345;
	
	static final String driver = "com.mysql.jdbc.Driver";
	static final String url = "jdbc:mysql://localhost:3306/CSCI3310";
	static final String user = "leo";
	static final String password = "123456";
	static final String userTable = "user";
	static final String contactTable = "contact";
	
	static final int QUEUE_DEFAULT_SIZE = 64;
	
	static PubPriKeyPair keyPair = null;
	
	// inter-thread communication
	static HashMap<String, ArrayBlockingQueue<byte[]> > packetHashMap = new HashMap<String, ArrayBlockingQueue<byte[]> >();
	
	private static class ServerSocketResponds implements Runnable{
		
		ArrayBlockingQueue<byte[]> packetQueue = null;
		
		Socket socket = null;
		DataInputStream socketInputStream = null;
		DataOutputStream socketOutputStream = null;
		PublicKey clientPublicKey = null;
		
		Connection sqlConn = null;
		Statement sqlStatement = null;
		
		String userName = null;
		
		public ServerSocketResponds(Socket aSocket){
			
			socket = aSocket;
			
			try{
				
				socketInputStream = new DataInputStream(socket.getInputStream());
				socketOutputStream = new DataOutputStream(socket.getOutputStream());
				
				Class.forName(driver);
				sqlConn = DriverManager.getConnection(url, user, password);
				sqlStatement = sqlConn.createStatement();
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
		}
		
		class ClientWriter implements Runnable{
			
			public void run(){
				
				byte[] bytes = null;
				
				try{
					
					while(true){
						
						bytes = packetQueue.take();
						
						if(bytes[0] == 0){
							
							break;
							
						}
						
						MessageUtil.sendEncryptedPacket(socketOutputStream, bytes, clientPublicKey);
						
					}
					
				}catch(Exception e){
					
					e.printStackTrace();
					
				}
			}
			
		}
		
		private boolean addContact(String contact){
			
// System.out.printf("loginUser: [%s] contact: [%s]\n", loginUser, contact);
			
			try{
				
				ResultSet resultSet = sqlStatement.executeQuery("select * from " + userTable + " where userName='" + userName + "'");
				
				if(resultSet.next()){
					
					if(sqlStatement.executeUpdate("INSERT INTO " + contactTable + " (userName, contact) values ('" + userName + "', '" + contact + "'), ('" + contact + "', '" + userName + "');") > 0){
						
						return true;
						
					}
					
				}
				
			}catch(Exception e){
				
				// e.printStackTrace();
				return false;
				
			}
			
			return false;
			
		}
		
		private LinkedList<String> getContact(){
			
			LinkedList<String> contactList = new LinkedList<String>();
			
			try{
				
				ResultSet resultSet = sqlStatement.executeQuery("select * from " + contactTable + " where userName='" + userName + "' order by contact");
				
				while(resultSet.next()){
					
					contactList.add(resultSet.getString(2));
					
				}
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
			return contactList;
			
		}
		
		class ClientReader implements Runnable{
			
			public void run(){
				
				byte[] receivedPacket = null;
				
				try{
					
					while(true){
						
						// get a packet from client
						receivedPacket = MessageUtil.readDecryptedPacket(socketInputStream, keyPair.privateKey);
						
						if(receivedPacket == null){
							
							// terminate ClientWriter
							packetQueue.put(new ReplyPacket((byte)0).packet);
							
							break;
							
						}
						
						switch(receivedPacket[0]){
							
							case MessageUtil.CONTACT_ADD:
								
								ContactAddPacket contactAddPacket = new ContactAddPacket(receivedPacket);
								
								if(!userName.equals(contactAddPacket.contact)){
								
									if(addContact(contactAddPacket.contact)){
										
										packetQueue.put(new ReplyPacket(MessageUtil.CONTACT_ADD_ACCEPT).packet);
										
										break;
										
									}
									
								}
								
								packetQueue.put(new ReplyPacket(MessageUtil.CONTACT_ADD_REJECT).packet);
								
								break;
								
							case MessageUtil.CONTACT_GET:
								
								// get contact list
								LinkedList<String> contactList = getContact();
								
								ContactGetPacket contactGetPacket = new ContactGetPacket(contactList);
								
								packetQueue.put(contactGetPacket.packet);
								
								break;
								
							case MessageUtil.MESSAGE:
								
								Message message = new Message(receivedPacket);
								
								ArrayBlockingQueue<byte[]> targetQueue = null;
								
								for(String element:message.receiverList){
									
									targetQueue = packetHashMap.get(element);
									
									if(targetQueue != null){
										
										targetQueue.put(receivedPacket);
										
									}else{
										
										// TODO: put offline message into database using SQL
										
System.out.printf("user [%s] offline\n", element);
										
									}
									
								}
								
								packetQueue.put(new ReplyPacket(MessageUtil.MESSAGE_ACCEPT).packet);
								
								break;
								
							default:
								
								packetQueue.put(new ReplyPacket(MessageUtil.REJECT).packet);
								
								break;
						}
						
					}
					
				}catch(Exception e){
					
					e.printStackTrace();
					
				}
				
			}
			
		}
		
		public void run(){
			
			// key exchange
			
			// send server public key to client
			MessageUtil.sendPacket(socketOutputStream, MessageUtil.keyToBytes(keyPair.publicKey));
			
			// receive client public key
			byte[] receivedContent = MessageUtil.readDecryptedPacket(socketInputStream, keyPair.privateKey);
			
			if(receivedContent == null){
				
				return;
				
			}
			
			clientPublicKey = MessageUtil.bytesToPublicKey(receivedContent);
			
			try{
				
				LoginPacket loginPacket = null;
				ResultSet resultSet = null;
				
				while(true){
					
					// get a packet from client
					byte[] receivedPacket = MessageUtil.readDecryptedPacket(socketInputStream, keyPair.privateKey);
					
					if(receivedPacket == null){
						
						break;
						
					}
					
					// check if it is a login request
					if(receivedPacket[0] == MessageUtil.LOGIN){
						
						loginPacket = new LoginPacket(receivedPacket);
						
						if(packetHashMap.get(loginPacket.userName) != null){
							
							MessageUtil.sendEncryptedPacket(socketOutputStream, new ReplyPacket(MessageUtil.REJECT).packet, clientPublicKey);
							continue;
							
						}
						
						// check user name and password
						resultSet = sqlStatement.executeQuery("select * from " + userTable + " where userName='" + loginPacket.userName + "' and pw='" + loginPacket.password + "'");
						
						if(resultSet.next()){
							
							// reply success
							MessageUtil.sendEncryptedPacket(socketOutputStream, new ReplyPacket(MessageUtil.LOGIN_ACCEPT).packet, clientPublicKey);
							
							userName = loginPacket.userName;
							
							break;
							
						}else{
							
							MessageUtil.sendEncryptedPacket(socketOutputStream, new ReplyPacket(MessageUtil.REJECT).packet, clientPublicKey);
							
						}
						
					}else{
						
						MessageUtil.sendEncryptedPacket(socketOutputStream, new ReplyPacket(MessageUtil.REJECT).packet, clientPublicKey);
						
					}
					
				}
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
			packetQueue = new ArrayBlockingQueue<byte[]>(QUEUE_DEFAULT_SIZE);
			
			// TODO: retrieve offline message from database using SQL and put into packetQueue
			
			// client is now available to receive message from other clients
			packetHashMap.put(userName, packetQueue);
			
			Thread clientReaderThread = new Thread(new ClientReader());
			Thread clientWriterThread = new Thread(new ClientWriter());
			
			clientReaderThread.start();
			clientWriterThread.start();
			
			try{
				
				clientReaderThread.join();
				clientWriterThread.join();
				
				// remove from receiving message
				packetHashMap.remove(userName);
				
				sqlConn.close();
				
				socket.close();
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
		}
		
	}
	
	private static class ServerSocketAccept implements Runnable{
		
		int portNumber = 0;
		ServerSocket serverSocket = null;
		Socket socket = null;
		
		public ServerSocketAccept(int bindPort){
			
			portNumber = bindPort;
			
			try{
				
				serverSocket = new ServerSocket(port);
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
			socket = null;
			
		}
		
		public void run(){
			
			try{
				
				ServerSocketResponds serverSocketResponds = null;
				Thread respondsThread = null;
				
				while(true){
					
					socket = serverSocket.accept();
					
					System.out.printf("connected with %s : %d\n", socket.getInetAddress().toString(), socket.getPort());
					
					serverSocketResponds = new ServerSocketResponds(socket);
					
					respondsThread = new Thread(serverSocketResponds);
					
					respondsThread.start();
					
				}
			
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
		}
		
	}
	
	public static void main(String[] args){
		
		String message = "Hello, MessageServer!";
		
		System.out.println(message);
		
		// generate public/private key pair
		keyPair = MessageUtil.genPubPriKeyPair();
		
		ServerSocketAccept serverSocketAccept = new ServerSocketAccept(port);
		
		Thread acceptThread = new Thread(serverSocketAccept);
		
		acceptThread.start();
		
		try{
			
			acceptThread.join();
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
	}
	
}
