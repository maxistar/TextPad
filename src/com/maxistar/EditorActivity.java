package com.maxistar;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class EditorActivity extends Activity {
	private static final int OPEN_FILE = 1;
	private static final int SAVE_FILE = 2;
	
	private static final int REQUEST_OPEN = 1;
	private static final int REQUEST_SAVE = 2;
	
	private EditText mText;
	
	private String filename = "";
	private Boolean changed = false;
	
	private Boolean open_when_saved = false; //to figure out better way
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        mText = (EditText) this.findViewById(R.id.editText1);
        mText.setText("");
        mText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            	changed = true;
            }
        });
        changed = false;
        //R.layout.main.
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, OPEN_FILE, 0, R.string.open_file)
                .setIcon(android.R.drawable.ic_menu_edit);
        
        menu.add(0, SAVE_FILE, 0, R.string.save_file)
        		.setIcon(android.R.drawable.ic_menu_save);

        return super.onCreateOptionsMenu(menu);
    }   
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPEN_FILE:
                openFile();
                return true;
            case SAVE_FILE:
                saveFile();
                return true;                
        }
        return super.onOptionsItemSelected(item);
    }    
    
    protected void openFile(){    	
    	//this.showToast(Environment.getExternalStorageDirectory().getName());    	
    	if (changed){
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.file_not_saved)
            .setMessage(R.string.save_current_file)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Stop the activity
                	open_when_saved = true;
                    EditorActivity.this.saveFile();
                }

            })
            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openNewFile();    
                }
            })
            .show();
    	}
    	else {
    		openNewFile();
    	}
    }
    
    protected void openNewFile(){
    	Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
		intent.putExtra(FileDialog.START_PATH, "/sdcard");
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
		this.startActivityForResult(intent, REQUEST_OPEN);
    }
    
    protected void saveFile(){
    	if (filename==""){
    		Intent intent = new Intent(this.getBaseContext(),
                FileDialog.class);
    		intent.putExtra(FileDialog.START_PATH, "/sdcard");
    		this.startActivityForResult(intent, REQUEST_SAVE);
    	}
    	else {
    		saveNamedFile();
    	}
    }
    
    protected void saveNamedFile(){    	
    	//String string = this.mText.toString();
    	try {
    		File f = new File(filename);
    		
    		if (!f.exists()){
    			f.createNewFile();
    		}
    		
    		FileOutputStream fos = new FileOutputStream(f);
    		//fos.write(string.getBytes());
    		//fos.close();    		
    		//DataOutputStream dos = new DataOutputStream(fos);
  	      	String s = this.mText.getText().toString();
  	      	fos.write(s.getBytes("UTF-8"));
  	      	fos.close();
  	      	showToast("File written");
  	      	changed = false;
  	      	
  	      	if (open_when_saved){ //because of multithread nature figure out better way to do it
  	      		open_when_saved = false;
  	      		openNewFile();
  	      	}
    	}
    	catch(FileNotFoundException e){
    		this.showToast("File not found");
    	}
    	catch(IOException e){
    		this.showToast("Can not write file");
    	}
    }
    
    protected void openNamedFile(){
    	try {
    		File f = new File(filename);
    		FileInputStream fis = new FileInputStream(f);
    		
    	    long size = f.length();
            DataInputStream dis = new DataInputStream(fis);
            byte[] b = new byte[(int)size];
            int length = dis.read(b, 0, (int)size);

            dis.close();
            fis.close();

            String ttt = new String(b, 0, length,"UTF-8"); 
    		
    		this.mText.setText(ttt);
    		showToast("File opened");
    		changed = false;
    	}
    	catch(FileNotFoundException e){
    		this.showToast("File not found");
    	}
    	catch(IOException e){
    		this.showToast("Can not read file");
    	}
    	//fis.re
    }
    
    public synchronized void onActivityResult(final int requestCode,
            int resultCode, final Intent data) {
            if (resultCode == Activity.RESULT_OK) {
            		this.filename = data.getStringExtra(FileDialog.RESULT_PATH);
            		//this.showToast(this.filename);
                    if (requestCode == REQUEST_SAVE) {
                    	this.saveNamedFile();	
                    } else if (requestCode == REQUEST_OPEN) {
                    	this.openNamedFile();
                    }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            	showToast("Operation Canceled");
            }
    }    
    
    protected void showToast(String toast_str){
    	Context context = getApplicationContext();
    	CharSequence text = toast_str;
    	int duration = Toast.LENGTH_SHORT;
    	Toast toast = Toast.makeText(context, text, duration);
    	toast.show();
    }
}