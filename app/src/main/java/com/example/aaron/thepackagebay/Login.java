package com.example.aaron.thepackagebay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.os.AsyncTask;
import android.widget.Toast;

import java.sql.*;

public class Login extends Activity {
    Connection conn;
    DB myDB = new DB();

    public void popup(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void login(View view)  {
        EditText eUsername = (EditText)findViewById(R.id.eUsername);
        EditText ePassword = (EditText)findViewById(R.id.ePassword);
        String username = eUsername.getText().toString().toLowerCase();
        String password = ePassword.getText().toString();

        if(username.isEmpty())
            popup("Invalid username");
        else if(password.isEmpty())
            popup("Invalid password");
        else {
            if(!myDB.checkLoginInfo(username, password))
                popup("Invalid credentials");
            else{
                Intent nextActivity = new Intent(this, HomeActivity.class);
                nextActivity.putExtra("user", username);
                startActivity(nextActivity);
            }
        }
    }
    public void register(View view)  {
        //myDB.addColumn();
        EditText eUsername = (EditText)findViewById(R.id.eUsername);
        EditText ePassword = (EditText)findViewById(R.id.ePassword);
        String username = eUsername.getText().toString().toLowerCase();
        String password = ePassword.getText().toString();

        if(username.isEmpty())
            popup("Invalid username");
        else if (password.isEmpty())
            popup("Invalid password");
        else
            if(myDB.registerUser(username, password))
                popup("Succesfully registered user");
            else
                popup("This user already exist");
    }
}
