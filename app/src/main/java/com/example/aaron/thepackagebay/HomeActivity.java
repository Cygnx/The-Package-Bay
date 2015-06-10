package com.example.aaron.thepackagebay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class HomeActivity extends ActionBarActivity {
    DB myDB = new DB();
    String myUserId = "";
    int monitorRate = 1000; // ms
    boolean bMonitor = false;
    Handler mHandler = new Handler(Looper.getMainLooper());
    String selectedUser = "";

    public void popup(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public AlertDialog.Builder consentMsgBox(String title, String msg) {
        return new AlertDialog.Builder(this).setTitle(title).setMessage(msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Intent i = getIntent();
        myUserId = i.getStringExtra("user");
        getSupportActionBar().setTitle("Logged in as: " + myUserId);
    }

    @Override
    public void onResume() {
        super.onResume();
        bMonitor = true;
        startMonitoringMsgs();
        populateFriendsList();
        updateLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        bMonitor = false;
    }

    public void homeAddFriend(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Add Friend");
        alert.setMessage("Name:");

        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String FriendID = input.getText().toString();
                dialog.dismiss();
                if (myDB.isFriends(myUserId, FriendID) || !myDB.sendFriendReq(myUserId, FriendID)) {
                    popup("Failed to send msg friend");
                } else {
                    popup("Successfully sent friend request");
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }


    public void homeRemoveFriend(View view) {
       // TextView selectedFriend = (TextView) findViewById(R.id.textBLSelected);
        String selFriend = selectedUser; //selectedFriend.getText().toString();

        if (selFriend.isEmpty()) {
            popup("Select a valid user.");
            return;
        }

        myDB.removeFriend(myUserId, selFriend);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        populateFriendsList();
    }


    private boolean isGpsEnabled() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        return service.isProviderEnabled(LocationManager.GPS_PROVIDER) && service.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void homeLogout(View view) {
        bMonitor = false;
        finish();
    }

    protected void startMonitoringMsgs() {
        Thread t = new Thread() {
            public void run() {
                ArrayList<String> msg;
                while (bMonitor) {
                    msg = myDB.getMyMsg(myUserId);

                    if (!msg.isEmpty()) {
                        handleMsg(msg);
                    }
                    try {
                        Thread.sleep(monitorRate);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }

    public void updateLocation() {
        LocationManager lm;
        String provider;
        Location l;
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        provider = lm.getBestProvider(c, false);
        l = lm.getLastKnownLocation(provider);
        if (l != null) {
            double lng = l.getLongitude();
            double lat = l.getLatitude();
            myDB.updateLocation(myUserId, lng+"",lat+"");
        } else {
            popup("No Provider");
        }

    }

    public void handleMsg(final ArrayList<String> msg) {
        int sender = 0, receiver = 1, message = 2, id = 3;

        final String msgName = msg.get(message).split(" ")[0];

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (msgName.equals("FriendReqFrom"))
                    handleFriendRequest(msg);
                if (msgName.equals("FriendReqResponse"))
                    handleFriendReqResponse(msg);
                if (msgName.equals("PackageReqFrom"))
                    handlePackageRequest(msg);
                if (msgName.equals("PackageReqResponse"))
                    handlePackageResponse(msg);
                if (msgName.equals("IntermediaryReqFrom"))
                    handleIntermediaryRequest(msg);
                if (msgName.equals("txtFrom"))
                    handleTxtFrom(msg);
            }
        });
    }

    private void handleTxtFrom(ArrayList<String> msg) {
        int sender = 0, receiver = 1, message = 2, id = 3;
        final String senderID = msg.get(sender);

        String msgToDisplay = "";
        msgToDisplay += "Sender: " + senderID + "\n";
        msgToDisplay += "Message: \n";

        for (int i = 1; i < msg.get(message).split(" ").length; i++)
            msgToDisplay += msg.get(message).split(" ")[i];
        msgToDisplay += "\n";

        consentMsgBox("New text msg", msgToDisplay).show();
    }

    private void handleIntermediaryRequest(ArrayList<String> msg) {
        int sender = 0, receiver = 1, message = 2, id = 3;
        final String senderID = msg.get(sender);
        final String receiverID = msg.get(message).split(" ")[1];
        String receiverMsg = "";

        String msgToDisplay = "";
        msgToDisplay += "You've been selected by "+senderID+" to act as an intermediary to deliver a package to "+receiverID;

        for (int i = 2; i < msg.get(message).split(" ").length; i++)
            receiverMsg += msg.get(message).split(" ")[i];

        final String receiverMsg2 = receiverMsg;

        AlertDialog.Builder consentBox = consentMsgBox("You've been chosen as an intermediary", msgToDisplay);
        consentBox.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                myDB.sendPackageRequest(senderID, receiverID, " 1 " + receiverMsg2);
            }
        });
        consentBox.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                myDB.respondToPackageRequest(receiverID, senderID, "declined");
            }
        });
        consentBox.show();
    }

    private void handlePackageResponse(ArrayList<String> msg) {
        int sender = 0, receiver = 1, message = 2, id = 3;
        final String consent = msg.get(message).split(" ")[1];
        final String text = msg.get(sender) + " has " + consent + " your package request.";

        popup(text);
    }

    private void handlePackageRequest(ArrayList<String> msg) {
        int sender = 0, receiver = 1, message = 2, id = 3;
        final String senderID = msg.get(sender);
        final String receiverID = msg.get(receiver);

        String msgToDisplay = "";
        msgToDisplay += "Sender: " + senderID + "\n";
        msgToDisplay += "Delivery method: " + msg.get(message).split(" ")[1] == "0" ? "Direct Delivery" : "Intermediary Delivery" + "\n";
        msgToDisplay += "Message: ";

        for (int i = 2; i < msg.get(message).split(" ").length; i++)
            msgToDisplay += msg.get(message).split(" ")[i];
        msgToDisplay += "\n";


        AlertDialog.Builder consentBox = consentMsgBox("Incoming package request", msgToDisplay);
        consentBox.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                myDB.addPackage(senderID, receiverID);
                myDB.respondToPackageRequest(receiverID, senderID, "accepted");
                populateFriendsList();
            }
        });
        consentBox.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                myDB.respondToPackageRequest(receiverID, senderID, "declined");
            }
        });
        consentBox.show();
    }

    public void handleFriendRequest(ArrayList<String> msg) {
        int sender = 0, receiver = 1, message = 2, id = 3;
        final String senderID = msg.get(sender);
        final String receiverID = msg.get(receiver);


        AlertDialog.Builder consentBox = consentMsgBox("Incoming Friend Request", senderID + " has sent you a friend request");
        consentBox.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                myDB.addFriend(receiverID, senderID);
                myDB.addFriend(senderID, receiverID);
                myDB.RespondToFriendReq(receiverID, senderID, "accepted");
                populateFriendsList();
            }
        });
        consentBox.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                myDB.RespondToFriendReq(receiverID, senderID, "declined");
            }
        });
        consentBox.show();
    }

    public void handleFriendReqResponse(ArrayList<String> msg) {
        int sender = 0, receiver = 1, message = 2, id = 3;
        final String consent = msg.get(message).split(" ")[1];
        final String text = msg.get(sender) + " has " + consent + " your friend request.";
        popup(text);
    }

    public void populateFriendsList() {
        ArrayList<String> friendList = myDB.getBuddyList(myUserId);
        ArrayList<ArrayList<String>> packageList = myDB.getPackageList(myUserId);
        ArrayList<String> receivingFrom = packageList.get(0);
        ArrayList<String> sendingTo = packageList.get(1);

        int i = 0;
        for (String a : friendList) {
            if (receivingFrom.contains(a))
                friendList.set(i, a + " is sending a package to you");
            else if (sendingTo.contains(a))
                friendList.set(i, a + " is awaiting a package from you");
            i++;
        }

        final ListView friendLV = (ListView) findViewById(R.id.LVBuddyList);
        final TextView selectedFriend = (TextView) findViewById(R.id.textBLSelected);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, friendList);
        friendLV.setAdapter(arrayAdapter);
        friendLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemString = friendLV.getItemAtPosition(position).toString();
                String friend = itemString.split(" ")[0];
                selectedUser = friend;
                String friendsFeedbackRating = myDB.getTrustLevel(friend);
                selectedFriend.setText(friend + "/ Feedback Rating: " + friendsFeedbackRating);

            }
        });
    }

    public void homeSendPackage(View view) {
        //TextView selectedFriend = (TextView) findViewById(R.id.textBLSelected);
        String receiver = selectedUser;//selectedFriend.getText().toString();

        if (receiver.isEmpty()) {
            popup("Select a valid user.");
            return;
        }

        Intent nextActivity = new Intent(this, PackageOptions.class);
        nextActivity.putExtra("sender", myUserId);
        nextActivity.putExtra("receiver", receiver);
        startActivity(nextActivity);
    }

    public void homeFinishedTransaction(View view) {
        //TextView selectedFriend = (TextView) findViewById(R.id.textBLSelected);
        final String sender = selectedUser;//selectedFriend.getText().toString();

        if (sender.isEmpty()) {
            popup("Select a valid user.");
            return;
        }

        myDB.finalizePackage(sender, myUserId);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        populateFriendsList();

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Would you like to rate your sender?");
        alert.setMessage("Specify your rating(-10 to 10):");

        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Rate!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String rating = input.getText().toString();
                dialog.dismiss();
                myDB.updateTrustLevel(sender, Integer.parseInt(rating));
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }

    public void homeSendText(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Send msg");
        alert.setMessage("Specify your text:");

        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String msg = input.getText().toString();
                dialog.dismiss();
                //TextView selectedFriend = (TextView) findViewById(R.id.textBLSelected);
                String receiver = selectedUser;
                if (receiver.isEmpty()) {
                    popup("Select a valid user.");
                    return;
                }
                myDB.sendTxtRequest(myUserId, receiver, msg);
                popup("Your msg has been sent to " + receiver );
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }
}