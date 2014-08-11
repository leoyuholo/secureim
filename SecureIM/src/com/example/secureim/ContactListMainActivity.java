package com.example.secureim;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ContactListMainActivity extends Activity {

	private Button addContactButton;
	private Button showChatsButton;
	private ListView contactListView;
	
	public static ArrayList<String> contactArrayList;
	
	private int addContactState = 0;
	
	private class GetContactMessageClient implements Runnable{
		
		public void run(){
			
			contactArrayList = MainActivity.messageClient.getContact();
			
		}
		
	}

	private class AddContactMessageClient implements Runnable{
		
		String contact = null;
		
		public AddContactMessageClient(String aString){
			
			contact = aString;
			
		}
		
		public void run(){
			
			if(MainActivity.messageClient.addContact(contact)){
				
				addContactState = 1;
				
			}else{
				
				addContactState = 2;
				
			}
			
		}
		
	}
	
	public void onCreate(Bundle savedInstanceState){

		super.onCreate(savedInstanceState);
		setContentView(R.layout.contactlist_activity_main);

		addContactButton = (Button)findViewById(R.id.addContactButton);
		showChatsButton = (Button)findViewById(R.id.showChatsButton);
		contactListView = (ListView)findViewById(R.id.contactListView);
		
		contactArrayList = null;
		
		new Thread(new GetContactMessageClient()).start();
		
		while(contactArrayList == null){
			
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
		}
		
		MainActivity.messageClient.contactListMainActivity = this;
		
		contactListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contactArrayList));

		contactListView.setClickable(true);
		
		contactListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				
				Intent intent = new Intent(arg1.getContext(), ChatMainActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("userName", arg0.getItemAtPosition(arg2).toString());
				
				intent.putExtras(bundle);
				startActivityForResult(intent, 0);
				
			}
			
		});
		
		addContactButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				final EditText inputEditText = new EditText(v.getContext());
				
				new AlertDialog.Builder(v.getContext())
						.setTitle("Add contact")
						.setMessage("Enter user name")
						.setView(inputEditText)
						.setPositiveButton("Add",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										
										addContactState = 0;
										
										new Thread(new AddContactMessageClient(inputEditText.getText().toString())).start();
										
										while(addContactState == 0){
											
								        	try {
												Thread.sleep(100);
											} catch (InterruptedException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
								        	
										}
										
										if(addContactState == 1){
											
											Toast.makeText(ContactListMainActivity.this, "Add contact success!", Toast.LENGTH_SHORT).show();

											contactArrayList = null;
											
											new Thread(new GetContactMessageClient()).start();
											
											while(contactArrayList == null){
												
									        	try {
													Thread.sleep(100);
												} catch (InterruptedException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
									        	
											}
											
											contactListView.setAdapter(new ArrayAdapter<String>(ContactListMainActivity.this, android.R.layout.simple_list_item_1, contactArrayList));
											
										}else{

											Toast.makeText(ContactListMainActivity.this, "Add contact fail.", Toast.LENGTH_SHORT).show();
											
										}
										
									}
								})
						.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub

									}
								}).show();
				
			}
			
		});
		
		showChatsButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				final ListView chatListView = new ListView(v.getContext());
				chatListView.setAdapter(new ArrayAdapter<String>(ContactListMainActivity.this, android.R.layout.simple_list_item_1, new ArrayList<String>(MainActivity.chatsHashMap.keySet())));
				chatListView.setClickable(true);
				chatListView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						// TODO Auto-generated method stub
						Intent intent = new Intent(arg1.getContext(), ChatMainActivity.class);
						Bundle bundle = new Bundle();
						bundle.putString("userName", arg0.getItemAtPosition(arg2).toString());
						
						intent.putExtras(bundle);
						startActivityForResult(intent, 0);
						
					}
					
				});
				
				new AlertDialog.Builder(v.getContext())
				.setTitle("Chats")
				.setView(chatListView).show();
				
			}
			
		});
		
	}
	
	public void onDestroy(){
		
		MainActivity.messageClient.contactListMainActivity = null;
		super.onDestroy();
		
	}
	
}
