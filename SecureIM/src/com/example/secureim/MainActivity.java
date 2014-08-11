package com.example.secureim;

import java.util.HashMap;

import com.example.secureim.MessageUtil.Chat;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button loginButton;
	private EditText loginNameEditText;
	private EditText passwordEditText;
	
	public static MessageClient messageClient;
	
	public static HashMap<String, Chat> chatsHashMap;
	
	private int loginState = 0;
	
	protected class createMessageClient implements Runnable{
		
		public void run(){
			
			messageClient = new MessageClient();
			
		}
		
	}
	
	protected class loginMessageClient implements Runnable{
		
		String userName = null;
		String password = null;
		
		public loginMessageClient(String user, String pw){
			
			userName = user;
			password = pw;
			
		}
		
		public void run(){
			
			if(messageClient.login(userName, password)){
				
				loginState = 1;
				
			}else{
				
				loginState = 2;
				
			}
			
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        loginButton = (Button)findViewById(R.id.loginButton);
        loginNameEditText = (EditText)findViewById(R.id.loginNameText);
        passwordEditText = (EditText)findViewById(R.id.passwordText);
        
        chatsHashMap = new HashMap<String, Chat>();
        
        loginButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				
		        if(messageClient == null || loginState == 1){
		        	
		        	if(messageClient != null){
		        		
		        		messageClient.logout();
		        		
		        	}
		        	
		        	messageClient = null;
		        	
			        new Thread(new createMessageClient()).start();
			        
			        while(messageClient == null){
			        	
			        	try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							
							e.printStackTrace();
						}
			        	
			        }
			        
		        }
				
				loginState = 0;
				
				new Thread(new loginMessageClient(loginNameEditText.getText().toString(), passwordEditText.getText().toString())).start();
				
				while(loginState == 0){

		        	try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					}
					
				}
				
				if(loginState == 1){
					
					// login success
					Toast.makeText(MainActivity.this, "Login success", Toast.LENGTH_SHORT).show();
					
					chatsHashMap.clear();
					
					Intent intent = new Intent(v.getContext(), ContactListMainActivity.class);
					startActivityForResult(intent, 0);
					
				}else{
					
					Toast.makeText(MainActivity.this, "Login fail, Please try again.", Toast.LENGTH_SHORT).show();
					
				}
				
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
