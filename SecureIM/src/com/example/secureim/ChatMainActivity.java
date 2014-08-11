package com.example.secureim;

import java.util.ArrayList;

import com.example.secureim.MessageUtil.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ChatMainActivity extends Activity {

	private TextView userNameTextView;
	private EditText inputText;
	private Button sendButton;
	private Button addChatButton;
	public ListView chatListView;
	
	public String userNameString;
	
	private Chat chat = null;
	
	class sendMessageMessageClient implements Runnable{
		
		String[] receiverList;
		String text;
		
		public sendMessageMessageClient(String[] dest, String content){
			
			receiverList = dest;
			text = content;
			
		}
		
		public void run(){
			
			MainActivity.messageClient.sendMessage(receiverList, text);
			//Toast.makeText(ChatMainActivity.this, "send: " + text, Toast.LENGTH_SHORT).show();
			
		}
		
	}
	
	public void refreshChatListView(){
		
		chatListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, chat.getMessageArrayList()));
		
	}
	
	public void onCreate(Bundle savedInstanceState){

		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_activity_main);

		Bundle bundle = getIntent().getExtras();
		
		userNameTextView = (TextView)findViewById(R.id.userNameTextView);
		inputText = (EditText)findViewById(R.id.inputText);
		sendButton = (Button)findViewById(R.id.sendButton);
		addChatButton = (Button)findViewById(R.id.addChatButton);
		chatListView = (ListView)findViewById(R.id.chatListView);
		
		userNameString = bundle.getString("userName");
		
		userNameTextView.setText(userNameString);
		
		MainActivity.messageClient.chatMainActivity = this;
		
		chat = MainActivity.chatsHashMap.get(userNameString);
		
		if(chat == null){
			
			//Toast.makeText(ChatMainActivity.this, "new chat", Toast.LENGTH_SHORT).show();
			
			chat = new Chat(userNameString);
			
			MainActivity.chatsHashMap.put(userNameString, chat);
			
		}else{
			
			chatListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, chat.getMessageArrayList()));
			
		}
		
		sendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				String text = inputText.getText().toString();
				if(text.length() > 0){
					
					String[] receiverList = chat.receiverList.toArray(new String[1]);
					new Thread(new sendMessageMessageClient(receiverList, text)).start();
					chat.addMessage(new Message(MainActivity.messageClient.userName, receiverList, text));
					chatListView.setAdapter(new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_list_item_1, chat.getMessageArrayList()));
					inputText.setText("");
					
				}
				
			}
		});
		
		addChatButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				final ListView contactListView = new ListView(v.getContext());
				contactListView.setAdapter(new ArrayAdapter<String>(ChatMainActivity.this, android.R.layout.simple_list_item_1, ContactListMainActivity.contactArrayList));
				contactListView.setClickable(true);
				contactListView.setOnItemClickListener(new OnItemClickListener() {
					
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						// TODO Auto-generated method stub
						String addUserName = arg0.getItemAtPosition(arg2).toString();
						if(!chat.receiverList.contains(addUserName)){
							
							ArrayList<String> receiverArrayList = new ArrayList<String>(chat.receiverList);
							
							receiverArrayList.add(addUserName);
							
							userNameString = MessageUtil.getReceiverListString(receiverArrayList.toArray(new String[1]), MainActivity.messageClient.userName);
							
							userNameTextView.setText(userNameString);
							
							Chat tmpChat = MainActivity.chatsHashMap.get(userNameString);
							
							if(tmpChat == null){
								
								//Toast.makeText(ChatMainActivity.this, "new chat", Toast.LENGTH_SHORT).show();
								
								chat = chat.addReceiver(addUserName);
								
								MainActivity.chatsHashMap.put(userNameString, chat);
								
								chatListView.setAdapter(new ArrayAdapter<String>(arg0.getContext(), android.R.layout.simple_list_item_1, chat.getMessageArrayList()));
								
							}else{
								
								chat = tmpChat;
								
								chatListView.setAdapter(new ArrayAdapter<String>(arg0.getContext(), android.R.layout.simple_list_item_1, chat.getMessageArrayList()));
								
							}
						}
						
					}
					
				});
				
				new AlertDialog.Builder(v.getContext())
				.setTitle("Add user to chat")
				.setView(contactListView)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.dismiss();
						
					}
					
				}).show();
				
			}
			
		});
		
	}
	
	public void onDestroy(){
		
		MainActivity.messageClient.chatMainActivity = null;
		super.onDestroy();
		
	}
}
