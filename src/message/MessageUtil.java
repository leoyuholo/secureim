package message;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

public class MessageUtil{
	
	private static final String algorithm = "RSA";
	private static final String transformation = "RSA/ECB/PKCS1Padding";
	private static final int keyLength = 1024;
	
	public static final String charSet = "utf-8";
	
	public static final byte NULL = 000;
	public static final byte ACCEPT = 010;
	public static final byte REJECT = 020;
	
	public static final byte LOGIN = 001;
	public static final byte LOGIN_ACCEPT = 011;
	public static final byte LOGIN_REJECT = 021;
	
	public static final byte CONTACT_ADD = 002;
	public static final byte CONTACT_ADD_ACCEPT = 012;
	public static final byte CONTACT_ADD_REJECT = 022;
	
	public static final byte CONTACT_GET = 003;
	public static final byte CONTACT_GET_ACCEPT = 013;
	public static final byte CONTACT_GET_REJECT = 023;
	
	public static final byte MESSAGE = 004;
	public static final byte MESSAGE_ACCEPT = 014;
	public static final byte MESSAGE_REJECT = 024;
	
	public static final byte MESSAGE_SEND = 005;
	public static final byte MESSAGE_SEND_ACCEPT = 015;
	public static final byte MESSAGE_SEND_REJECT = 025;
	
	public static final byte MESSAGE_RECEIVE = 006;
	public static final byte MESSAGE_RECEIVE_ACCEPT = 016;
	public static final byte MESSAGE_RECEIVE_REJECT = 026;
	
	public static class ReplyPacket{
		
		public byte protocol = 0;
		public byte[] packet = null;
		
		public ReplyPacket(byte[] data){
			
			packet = data;
			protocol = packet[0];
			
		}
		
		public ReplyPacket(byte protocol){
			
			packet = new byte[1];
			packet[0] = protocol;
			
		}
		
	}
	
	public static class LoginPacket{
		
		public String userName = null;
		public String password = null;
		public byte[] packet = null;
		
		public LoginPacket(byte[] data){
			
			try{
				
				packet = data;
				
				// wrap packet into a byteBuffer
				ByteBuffer byteBuffer = ByteBuffer.wrap(packet);
				
				// throw away the first byte, which is protocol byte
				byteBuffer.get();
				
				// get user name
				int length = byteBuffer.getInt();
				byte[] bytes = new byte[length];
				byteBuffer.get(bytes);
				userName = new String(bytes, charSet);
				
				// get password
				length = byteBuffer.getInt();
				bytes = new byte[length];
				byteBuffer.get(bytes);
				password = new String(bytes, charSet);
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
		}
		
		public LoginPacket(String user, String pw){
			
			try{
				
				userName = user;
				password = pw;
				
				byte protocol = LOGIN;
				byte[] userNameBytes = userName.getBytes(charSet);
				byte[] passwordBytes = password.getBytes(charSet);
				
				ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 4 + userNameBytes.length + 4 + passwordBytes.length);
				
				byteBuffer.put(protocol);
				byteBuffer.putInt(userNameBytes.length);
				byteBuffer.put(userNameBytes);
				byteBuffer.putInt(passwordBytes.length);
				byteBuffer.put(passwordBytes);
				
				packet = byteBuffer.array();
			
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
		}
		
	}
	
	public static class ContactAddPacket{
		
		public String contact = null;
		public byte[] packet = null;
		
		public ContactAddPacket(byte[] data){
			
			try{
				
				packet = data;
				
				// wrap packet into a byteBuffer
				ByteBuffer byteBuffer = ByteBuffer.wrap(packet);
				
				// throw away the first byte, which is protocol byte
				byteBuffer.get();
				
				// get contact user name
				int length = byteBuffer.getInt();
				byte[] bytes = new byte[length];
				byteBuffer.get(bytes);
				contact = new String(bytes, charSet);
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
		}
		
		public ContactAddPacket(String contactName){
			
			try{
				
				contact = contactName;
				
				byte protocol = CONTACT_ADD;
				byte[] contactBytes = contact.getBytes(charSet);
				
				ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 4 + contactBytes.length);
				
				byteBuffer.put(protocol);
				byteBuffer.putInt(contactBytes.length);
				byteBuffer.put(contactBytes);
				
				packet = byteBuffer.array();
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
		}
		
	}
	
	public static class ContactGetPacket{
		
		public LinkedList<String> contactList = null;
		public byte[] packet = null;
		
		public ContactGetPacket(byte[] data){
			
			packet = data;
			
			ByteBuffer byteBuffer = ByteBuffer.wrap(packet);
			
			byteBuffer.get();
			
			int listLength = byteBuffer.getInt();
			
			contactList = new LinkedList<String>();
			
			int length = 0;
			byte[] bytes = null;
			
			for(int i = 0;i < listLength;i++){
				
				length = byteBuffer.getInt();
				bytes = new byte[length];
				
				byteBuffer.get(bytes);
				
				try{
					
					contactList.add(new String(bytes, charSet));
					
				}catch(Exception e){
					
					e.printStackTrace();
					
				}
				
			}
			
		}
		
		public ContactGetPacket(LinkedList<String> list){
			
			contactList = list;
			
			byte protocol = CONTACT_GET_ACCEPT;
			
			// protocol (1 byte) and number of entries (4 bytes)
			int packetLength = 1 + 4;
			byte[] bytes = null;
			
			LinkedList<byte[]> bytesList = new LinkedList<byte[]>();
			
			for(String element:contactList){
				
				try{
					
					bytes = element.getBytes(charSet);
					bytesList.add(bytes);
					
					// length (4 bytes) and content (bytes.length bytes)
					packetLength += (4 + bytes.length);
					
				}catch(Exception e){
					
					e.printStackTrace();
					
				}
				
			}
			
			ByteBuffer byteBuffer = ByteBuffer.allocate(packetLength);
			
			byteBuffer.put(protocol);
			byteBuffer.putInt(contactList.size());
			
			for(byte[] element:bytesList){
				
				byteBuffer.putInt(element.length);
				byteBuffer.put(element);
				
			}
			
			packet = byteBuffer.array();
			
		}
		
	}
	
	public static class Message{
		
		public String sender = null;
		public String[] receiverList = null;
		public String content = null;
		public byte[] packet = null;
		
		public Message(byte[] data){
			
			try{
				
				packet = data;
				
				ByteBuffer byteBuffer = ByteBuffer.wrap(packet);
				
				byteBuffer.get();
				
				// get sender
				int length = byteBuffer.getInt();
				byte[] bytes = new byte[length];
				byteBuffer.get(bytes);
				sender = new String(bytes, charSet);
				
				// get receiver list
				int receiverListLength = byteBuffer.getInt();
				receiverList = new String[receiverListLength];
				
				for(int i = 0;i < receiverListLength;i++){
					
					length = byteBuffer.getInt();
					bytes = new byte[length];
					byteBuffer.get(bytes);
					receiverList[i] = (new String(bytes, charSet));
					
				}
				
				// get message content
				length = byteBuffer.getInt();
				bytes = new byte[length];
				byteBuffer.get(bytes);
				content = new String(bytes, charSet);
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
		}
		
		public Message(String src, String[] dest, String text){
			
			sender = src;
			receiverList = dest;
			content = text;
			
			try{
				
				byte protocol = MESSAGE;
				
				LinkedList<byte[]> bytesList = new LinkedList<byte[]>();
				
				byte[] senderBytes = sender.getBytes(charSet);
				
				// protocol (1 byte), sender length (4 bytes), sender (senderBytes.length) and number of receivers (4 bytes)
				int packetLength = 1 + 4 + senderBytes.length + 4;
				byte[] bytes = null;
				
				for(String element:receiverList){
					
					bytes = element.getBytes(charSet);
					bytesList.add(bytes);
					packetLength += (4 + bytes.length);
					
				}
				
				bytes = content.getBytes(charSet);
				
				packetLength += (4 + bytes.length);
				
				ByteBuffer byteBuffer = ByteBuffer.allocate(packetLength);
				
				byteBuffer.put(protocol);
				byteBuffer.putInt(senderBytes.length);
				byteBuffer.put(senderBytes);
				byteBuffer.putInt(receiverList.length);
				
				// put receiver list
				for(byte[] element:bytesList){
					
					byteBuffer.putInt(element.length);
					byteBuffer.put(element);
					
				}
				
				// put content
				byteBuffer.putInt(bytes.length);
				byteBuffer.put(bytes);
				
				packet = byteBuffer.array();
				
			}catch(Exception e){
				
				e.printStackTrace();
				
			}
			
		}
		
	}
	
	public static byte[] intToBytes(int i){
		
		byte[] result = new byte[4];
		
		result[0] = (byte)(i >> 24);
		result[1] = (byte)(i >> 16);
		result[2] = (byte)(i >> 8);
		result[3] = (byte)(i);
		
		return result;
		
	}
	
	public static int bytesToInt(byte[] bytes) {
		
		int result = 0;
		
		for (int i = 0;i < 4;i++) {
			
			// result = (result << 8) - Byte.MIN_VALUE + (int)bytes[i];
			result = (result << 8) + (int)bytes[i];
			
		}
		
		return result;
		
	}
	
	public static String bytesToHexString(byte[] bytes){
		
		StringBuffer hexString = new StringBuffer();
		String plainText;
		
		for (int i = 0; i < bytes.length; i++){
			
			plainText = Integer.toHexString(0xFF & bytes[i]);
			
			if(plainText.length() < 2){
				
				plainText = "0" + plainText;
				
			}
			
			hexString.append(plainText);
			
		}
		
		return hexString.toString();
		
	}
	
	public static class PubPriKeyPair{
		
		public PublicKey publicKey;
		public PrivateKey privateKey;
		
		public PubPriKeyPair(PublicKey aPublicKey, PrivateKey aPrivateKey){
			
			publicKey = aPublicKey;
			privateKey = aPrivateKey;
			
		}
		
	}
	
	public static byte[] keyToBytes(Key key){
		
		return key.getEncoded();
		
	}
	
	public static PublicKey bytesToPublicKey(byte[] bytes){
		
		PublicKey key = null;
		
		try{
			
			KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
			EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
			key = keyFactory.generatePublic(keySpec);
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
			
		return key;
		
	}
	
	public static PrivateKey bytesToPrivateKey(byte[] bytes){
		
		PrivateKey key = null;
		
		try{
			
			KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
			EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
			
			key = keyFactory.generatePrivate(keySpec);
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		return key;
		
	}
	
	public static PubPriKeyPair genPubPriKeyPair(){
		
		PubPriKeyPair keys = null;
		
		try{
			
			// key generator
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
			keyPairGenerator.initialize(keyLength, new SecureRandom());
			
			// key pair
			KeyPair keyPair = keyPairGenerator.genKeyPair();
			
			keys = new PubPriKeyPair(keyPair.getPublic(), keyPair.getPrivate());
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		return keys;
		
	}
	
	public static byte[] encryptBytes(Key aKey, byte[] content){
		
		byte[] encryptedContent = null;
		
// System.out.println("[EN-in]: " + content.length + " content: " + bytesToHexString(content));
		
		// split content into small data, each less than or equal to 117 bytes before encrypt
		byte[] encryptedBytes = null;
		byte[] intermediateBytes = null;
		LinkedList<byte[]> byteArrayList = new LinkedList<byte[]>();
		int encryptedContentSize = 0;
		
		try{
			
			for(int i = 0;i < content.length;i += 117){
				
				intermediateBytes = Arrays.copyOfRange(content, i, Math.min(i + 117, content.length));
				
				Cipher cipher = Cipher.getInstance(transformation);
				cipher.init(Cipher.ENCRYPT_MODE, aKey);
				
				encryptedBytes = cipher.doFinal(intermediateBytes);
				
				byteArrayList.add(encryptedBytes);
				
				encryptedContentSize += encryptedBytes.length;
				
			}
			
			encryptedContent = new byte[encryptedContentSize];
			
			Iterator<byte[]> iterator = byteArrayList.iterator();
			
			int encryptedContentPos = 0;
			
			while(iterator.hasNext()){
				
				encryptedBytes = iterator.next();
				
				for(int i = 0;i < encryptedBytes.length;i++){
					
					encryptedContent[encryptedContentPos] = encryptedBytes[i];
					encryptedContentPos++;
					
				}
				
			}
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
// System.out.println("[EN-ou]: " + encryptedContent.length + " content: " + bytesToHexString(encryptedContent));
		
		return encryptedContent;
		
	}
	
	public static byte[] decryptBytes(Key aKey, byte[] content){
		
		if(content == null){
			
			return null;
			
		}
		
		byte[] decryptedContent = null;
		
// System.out.println("[DE-in]: " + content.length + " content: " + bytesToHexString(content));
		
		// split content into small data, each less than or equal to 128 bytes before decrypt
		byte[] decryptedBytes = null;
		byte[] intermediateBytes = null;
		LinkedList<byte[]> byteArrayList = new LinkedList<byte[]>();
		int decryptedContentSize = 0;
		
		try{
			
			for(int i = 0;i < content.length;i += 128){
				
				intermediateBytes = Arrays.copyOfRange(content, i, Math.min(i + 128, content.length));
				
				Cipher cipher = Cipher.getInstance(transformation);
				cipher.init(Cipher.DECRYPT_MODE, aKey);
				
				decryptedBytes = cipher.doFinal(intermediateBytes);
				
				byteArrayList.add(decryptedBytes);
				
				decryptedContentSize += decryptedBytes.length;
				
			}
			
			decryptedContent = new byte[decryptedContentSize];
			
			Iterator<byte[]> iterator = byteArrayList.iterator();
			
			int decryptedContentPos = 0;
			
			while(iterator.hasNext()){
				
				decryptedBytes = iterator.next();
				
				for(int i = 0;i < decryptedBytes.length;i++){
					
					decryptedContent[decryptedContentPos] = decryptedBytes[i];
					decryptedContentPos++;
					
				}
				
			}
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
// System.out.println("[DE-ou]: " + decryptedContent.length + " content: " + bytesToHexString(decryptedContent));
		
		return decryptedContent;
		
	}
	
	public static void sendPacket(DataOutputStream stream, byte[] content){
		
		try{
			
			stream.writeInt(content.length);
			stream.write(content);
			
// System.out.println("contentLength: " + content.length + "send: " + bytesToHexString(content));
			
		}catch(IOException e){
			
			e.printStackTrace();
			
		}
		
	}
	
	public static void sendEncryptedPacket(DataOutputStream stream, byte[] content, Key encryptKey){
		
		sendPacket(stream, encryptBytes(encryptKey, content));
		
	}
	
	public static byte[] readPacket(DataInputStream stream){
		
		byte[] receivedData = null;
		
		try{
			
			int dataLength = stream.readInt();
			receivedData = new byte[dataLength];
			stream.read(receivedData, 0, dataLength);
			
// System.out.println("contentLength: " + receivedData.length + "read: " + bytesToHexString(receivedData));			
			
		}catch(IOException e){
			
			//e.printStackTrace();
			return null;
			
		}
		
		return receivedData;
		
	}
	
	public static byte[] readDecryptedPacket(DataInputStream stream, Key decryptKey){
		
		return decryptBytes(decryptKey, readPacket(stream));
		
	}
	
}
