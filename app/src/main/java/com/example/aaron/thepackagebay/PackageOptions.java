package com.example.aaron.thepackagebay;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class PackageOptions extends ActionBarActivity {
    DB myDB = new DB();
    String senderID = "";
    String receiverID = "";

    public void popup(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_options);

        Intent i = getIntent();
        senderID = i.getStringExtra("sender");
        receiverID = i.getStringExtra("receiver");

        //((TextView) findViewById(R.id.textBLSelected)).setText(receiverID);
        setContentView(R.layout.activity_package_options);
        ((TextView) findViewById(R.id.receiverName)).setText(" ["+receiverID+"]");

    }

    public void packageCancel(View view) {
        finish();
    }

    public void packageSend(View view) {
        RadioGroup optionGroup = (RadioGroup) findViewById(R.id.optionGroup);
        int selectedOption = optionGroup.indexOfChild(findViewById(optionGroup.getCheckedRadioButtonId()));

        String options = selectedOption + "";
        String msg = ((EditText) findViewById(R.id.packageMsg)).getText().toString();

        if(selectedOption == 0 )
            myDB.sendPackageRequest(senderID, receiverID, " "+ options + " " + msg);
        else {
            ArrayList<String> sortedIntermediaries = myDB.getIntermediaryList(senderID, receiverID);
            popup(sortedIntermediaries.get(0) + " has been selected as your intermediary");
            myDB.sendIntermediaryReq(senderID, sortedIntermediaries.get(0), receiverID + " " +msg);
        }
        finish();
    }
}
