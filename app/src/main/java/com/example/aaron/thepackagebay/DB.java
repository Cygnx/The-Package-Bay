package com.example.aaron.thepackagebay;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class DB {

    public boolean registerUser(String userID, String userPass) {
        if (isUserInDB(userID))
            return false;

        String sqlQuery = "INSERT INTO \"Users\" (id,\"Password\")" + "VALUES ('" + userID + "', '" + userPass + "');";
        sqlExec(sqlQuery);

        sqlQuery = "INSERT INTO \"Location\" (\"userid\")" + "VALUES ('" + userID + "');";
        sqlExec(sqlQuery);
        return true;
    }

    public boolean checkLoginInfo(String userID, String userPass) {
        String sqlQuery = "SELECT COUNT(*) FROM \"Users\" WHERE id = '" + userID + "' AND \"Password\" = '" + userPass + "';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);
        String count = result.get(0).get(0);
        return !count.equals("0");
    }

    public boolean isUserInDB(String userID) {
        String sqlQuery = "SELECT COUNT(*) FROM \"Users\" WHERE id = '" + userID + "';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);
        String count = result.get(0).get(0);
        return !count.equals("0");
    }

    //-----------------------------------------------------------------------------------------------------
    public boolean isFriends(String userID, String friendID) {
        String sqlQuery = "SELECT COUNT(*) FROM \"Friends\" WHERE id = '" + userID + "' AND \"friendID\" = '" + friendID + "';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);
        String count = result.get(0).get(0);
        return !count.equals("0");
    }

    public boolean addFriend(String userID, String friendID) {
        if (!isUserInDB(friendID) && isFriends(userID, friendID))
            return false;

        String sqlQuery = "INSERT INTO \"Friends\" (id,\"friendID\")" + "VALUES ('" + userID + "', '" + friendID + "');";
        sqlExec(sqlQuery);
        return true;
    }

    public boolean removeFriend(String userID, String friendID) {
        if (!isUserInDB(friendID) && !isFriends(userID, friendID))
            return false;

        String sqlQuery = "DELETE FROM \"Friends\" WHERE id = '" + userID + "' AND \"friendID\" = '" + friendID + "';";
        sqlExec(sqlQuery);

        sqlQuery = "DELETE FROM \"Friends\" WHERE \"friendID\" = '" + userID + "' AND id = '" + friendID + "';";
        sqlExec(sqlQuery);
        return true;
    }
    //-----------------------------------------------------------------------------------------------------

    public ArrayList<String> getMyMsg(String userID) {
        ArrayList<String> msg = new ArrayList<String>();
        if (!isUserInDB(userID))
            return msg;

        String sqlQuery = "SELECT * FROM \"Messages\" WHERE \"receiver\" = '" + userID + "';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);
        if (!result.isEmpty()) {
            msg = result.get(0);

            sqlQuery = "DELETE FROM \"Messages\" WHERE id = '" + msg.get(3) + "';";
            sqlExec(sqlQuery);
        }

        return msg;
    }

    public ArrayList<String> getBuddyList(String userID) {
        ArrayList<String> msg = new ArrayList<String>();
        if (!isUserInDB(userID))
            return msg;

        String sqlQuery = "SELECT \"friendID\" FROM \"Friends\" WHERE \"id\" = '" + userID + "';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);

        for (ArrayList<String> rows : result) {
            Log.i("Friends", rows.get(0));
            msg.add(rows.get(0));
        }

        return msg;
    }

    public ArrayList<ArrayList<String>> getPackageList(String userID) {
        ArrayList<String> receivingFrom = new ArrayList<String>();
        ArrayList<String> sendingTo = new ArrayList<String>();
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        String sqlQuery;

        if (!isUserInDB(userID))
            return result;

        sqlQuery = "SELECT \"sender\" FROM \"Deliveries\" WHERE \"receiver\" = '" + userID + "';";
        result = sqlExec(sqlQuery);
        for (ArrayList<String> rows : result) {
            receivingFrom.add(rows.get(0));
        }

        sqlQuery = "SELECT \"receiver\" FROM \"Deliveries\" WHERE \"sender\" = '" + userID + "';";
        result = sqlExec(sqlQuery);
        for (ArrayList<String> rows : result) {
            sendingTo.add(rows.get(0));
        }

        result.clear();
        result.add(receivingFrom);
        result.add(sendingTo);
        return result;
    }

    public boolean sendMsgToFriend(String userID, String friendID, String theMsg) {
        if (!isUserInDB(friendID))
            return false;

        String sqlQuery = "SELECT COUNT(*) FROM \"Messages\" WHERE \"sender\" = '" + userID + "' AND \"receiver\" = '" + friendID + "' AND \"msg\" = '" + theMsg + "';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);
        String count = result.get(0).get(0);
        if (!count.equals("0")) {
            Log.i("Error", "msg already exists");
            return false;
        }

        sqlQuery = "INSERT INTO \"Messages\" (\"sender\", \"receiver\",\"msg\")" + "VALUES ('" + userID + "', '" + friendID + "', '" + theMsg + "');";
        sqlExec(sqlQuery);
        return true;
    }

    public boolean sendFriendReq(String userID, String friendID) {
        if (isFriends(userID, friendID))
            return false;
        String msg = "FriendReqFrom";
        sendMsgToFriend(userID, friendID, msg);
        return true;
    }

    public void RespondToFriendReq(String userID, String friendID, String consent) {
        String msg = "FriendReqResponse " + consent;
        sendMsgToFriend(userID, friendID, msg);
    }

    public boolean sendIntermediaryReq(String userID, String friendID, String option) {
        String msg = "IntermediaryReqFrom";
        sendMsgToFriend(userID, friendID, msg + " " + option);
        return true;
    }

    public void RespondToIntermediaryRequest(String userID, String friendID, String consent) {
        String msg = "IntermediaryReqResponse " + consent;
        sendMsgToFriend(userID, friendID, msg);
    }

    public void sendPackageRequest(String userID, String friendID, String option) {
        String msg = "PackageReqFrom" + option;
        sendMsgToFriend(userID, friendID, msg);
    }


    public void respondToPackageRequest(String userID, String friendID, String consent) {
        String msg = "PackageReqResponse " + consent;
        sendMsgToFriend(userID, friendID, msg);
    }

    public void sendTxtRequest(String sender, String receiver, String option) {
        String msg = "txtFrom " + option;
        sendMsgToFriend(sender, receiver, msg);
    }

    public void addPackage(String sender, String receiver) {
        String sqlQuery = "INSERT INTO \"Deliveries\" (\"sender\", \"receiver\",\"status\")" + "VALUES ('" + sender + "', '" + receiver + "', 'Sending');";
        sqlExec(sqlQuery);
    }

    public void finalizePackage(String sender, String receiver) {
        String sqlQuery = "DELETE FROM \"Deliveries\" WHERE \"receiver\" = '" + receiver + "' AND \"sender\" = '" + sender + "';";
        sqlExec(sqlQuery);
    }

    public void updateLocation(String userID, String longitude, String latitude) {
        String sqlQuery = "UPDATE \"Location\" SET \"longitude\" = '"+longitude+"' , \"latitude\" =" +latitude+ " WHERE \"userid\" = '"+userID+"';";
        sqlExec(sqlQuery);
    }

    public void updateTrustLevel(String userID, int feedback) {
        String sqlQuery = "SELECT \"FeedbackLevel\" FROM  \"Users\" WHERE \"id\" = '"+userID+"';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);
        Log.i("feedback", result.get(0).get(0));
        int updatedFeedback = Integer.parseInt(result.get(0).get(0)) + feedback;

        sqlQuery = "UPDATE \"Users\" SET \"FeedbackLevel\" = '"+(updatedFeedback+"")+"' WHERE \"id\" = '"+userID+"';";
        sqlExec(sqlQuery);
    }

    public String getTrustLevel(String userID) {
        String sqlQuery = "SELECT \"FeedbackLevel\" FROM  \"Users\" WHERE \"id\" = '"+userID+"';";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);
        return result.get(0).get(0);
    }

    public float getDistance(String lon1, String lat1, String lon2, String lat2){
        Location l1=new Location("One");
        l1.setLatitude(Double.parseDouble(lat1));
        l1.setLongitude(Double.parseDouble(lon1));

        Location l2=new Location("Two");
        l2.setLatitude(Double.parseDouble(lat2));
        l2.setLongitude(Double.parseDouble(lon2));

        return l1.distanceTo(l2);
    }
    public ArrayList<String> getIntermediaryList(String sender, String receiver) {
        ArrayList<String> receiverFriendList = getBuddyList(receiver);
        ArrayList<String> senderFriendList = getBuddyList(sender);
        ArrayList<String> mutualFriends = new ArrayList<>();
        Map<Float,String> SortedMutualFriends = new TreeMap<Float,String>();

        for(String a : receiverFriendList) {                // builds list of mutual friends
            if(senderFriendList.contains(a))
                mutualFriends.add(a);
        }

        String sqlQuery = "SELECT * FROM \"Location\";";
        ArrayList<ArrayList<String>> result = sqlExec(sqlQuery);

        String senderLon = "", senderLat = "";
        String receiverLon = "" , receiverLat = "";
        for(ArrayList<String> a: result) {                // gets sender and receiver's long
            if(a.get(0).equals(sender)) {
                senderLon = a.get(1);
                senderLat = a.get(2);
            }
            if(a.get(0).equals(receiver)){
                receiverLon = a.get(1);
                receiverLat = a.get(2);
            }
        }

        for (ArrayList<String> a : result) {                // find the best mutual friend
            if(mutualFriends.contains(a.get(0))) {
                float distance = getDistance(senderLon,senderLat,a.get(1),a.get(2)) +  getDistance(receiverLon,receiverLat,a.get(1),a.get(2));
                SortedMutualFriends.put(distance, a.get(0));
            }
        }

        mutualFriends.clear();
        for(Map.Entry<Float,String> entry : SortedMutualFriends.entrySet()) {   // building mutual friends by sorted order
            mutualFriends.add(entry.getValue());
        }
        return mutualFriends;
    }
    //-------------------------------------------------------------------------------------------------
    public ArrayList<ArrayList<String>> sqlExec(String query) {
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        AsyncTask a = new DB.establishCon().execute(query);
        try {
            result = (ArrayList<ArrayList<String>>) a.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static class establishCon extends AsyncTask<String, Void, ArrayList<ArrayList<String>>> {
        protected ArrayList<ArrayList<String>> doInBackground(String... params) {
            String sql = params[0];
            Log.i("DB", sql);
            Connection conn;
            ArrayList<ArrayList<String>> retval = new ArrayList<ArrayList<String>>();
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                Log.i("sql error", e.toString());
            }
            String url = "jdbc:postgresql://localhost";
            try {
                DriverManager.setLoginTimeout(5);
                conn = DriverManager.getConnection(url);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql);
                while (rs.next()) {
                    ArrayList<String> temp = new ArrayList<String>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
                        temp.add(rs.getString(i));
                    retval.add(temp);
                }
                rs.close();
                st.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
                Log.i("Establish Con", e.toString());
            }
            return retval;
        }
    }
}
