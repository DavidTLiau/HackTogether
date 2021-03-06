package apt.hacktogether.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import org.apmem.tools.layouts.FlowLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import apt.hacktogether.R;
import apt.hacktogether.adapter.MessageQueryAdapter;
import apt.hacktogether.adapter.QueryAdapter;
import apt.hacktogether.event.AddPersonToMessageEvent;
import apt.hacktogether.layer.LayerImpl;
import apt.hacktogether.parse.ParseImpl;
import apt.hacktogether.utils.Common;
import apt.hacktogether.utils.ParseUtils;
import apt.hacktogether.utils.Utils;
import de.greenrobot.event.EventBus;

/*
 * MessageActivity.java
 * Handles displaying all Messages in a specific Conversation, or allows the user to specify the
 *  participants in a new Conversation. Uses the MessageQueryAdapter to drive the view.
 */

public class MessageActivity extends BaseActivity implements MessageQueryAdapter.MessageClickHandler {
    public static final String TAG = Common.TAG_MESSAGE_ACTIVITY;
    //The owning conversation
    private Conversation mConversation;

    //The Query Adapter that grabs all Messages and displays them based on their Position
    private MessageQueryAdapter mMessagesAdapter;

    //Once a Conversation is started (ie, the first message is sent) disallow adding/removing
    // of participants (this is an implementation choice, you can always choose to allow Participants
    // to add or remove users at any point if you choose)
    private Button mAddUserButton;

    //Layout view of current participants
    private FlowLayout mParticipantsList;

    //This is the actual view that contains all the messages
    private RecyclerView mMessagesView;

    //When starting a new Conversation, we keep a list of all target participants. The Conversation
    // is only created when the first message is sent
    private ArrayList<String> mTargetParticipants;


    //Grab all the view objects on the message_screen layout when the Activity starts
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        EventBus.getDefault().register(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Instant Message");

        //View containing all messages in the target Conversastion
        mMessagesView = (RecyclerView) findViewById(R.id.mRecyclerView);

        //Check to see when the locally Authenticated user is trying to send a message
        ImageButton sendButton = (ImageButton)findViewById(R.id.sendButton);
        if(sendButton != null)
            sendButton.setOnClickListener(this);

        //If this is a new conversation, we will want to allow the user to add his/her friends
        mAddUserButton = (Button)findViewById(R.id.addParticipants);
        if(mAddUserButton != null)
            mAddUserButton.setOnClickListener(this);

        //A view containing a list of all the Participants in the Conversation (not including the
        // locally authenticated user)
        mParticipantsList = (FlowLayout)findViewById(R.id.participantList);

        //If the soft keyboard changes the size of the mMessagesView, we want to force the scroll to
        // the bottom of the view so the latest message is always displayed
        attachKeyboardListeners(mMessagesView);
    }

    //Checks the state of the LayerClient and whether this is an existing Conversation or a new
    // Conversation
    public void onResume() {
        super.onResume();

        //If the user is not Authenticated, check to see if we need to return to the Login Screen,
        // or if the User can be Authenticated silently in the background
        if (!LayerImpl.isAuthenticated()) {

            if (ParseUser.getCurrentUser() == null) {

                Intent intent = new Intent(MessageActivity.this, LoginActivity.class);
                startActivity(intent);

            } else {

                LayerImpl.authenticateUser();

            }

        } else {

            //Now check to see if this is a new Conversation, or if the Activity needs to render an
            // existing Conversation
            Uri conversationURI = getIntent().getParcelableExtra("conversation-id");
            if(conversationURI != null)
                mConversation = LayerImpl.getLayerClient().getConversation(conversationURI);
            Intent it = getIntent();
            //This is an existing Conversation, display the messages, otherwise, allow the user to
            // add/remove participants and create a new Conversation
            if (mConversation != null) {
                setupMessagesView();
            }
            else if(it.getStringArrayListExtra("mTargetParticipants") != null){
                Log.d("DebugBefore","List"+mTargetParticipants);
                // the intent content would overwrite add participant every time
                mTargetParticipants  = it.getStringArrayListExtra("mTargetParticipants");
                populateToField(mTargetParticipants);
                setupMessagesView_new();
            }
            else {
                createNewConversationView();
            }
        }

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("inMessageActivity", true);
        installation.saveInBackground();
    }

    private void setupMessagesView_new() {

        Log.d("Activity", "Conversation exists, setting up view");

        //Hide the "add users" button
        hideAddParticipantsButton();

        //Create the appropriate RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mMessagesView.setLayoutManager(layoutManager);

        //And attach it to the appropriate QueryAdapter, which will automatically update the view
        // when a new Message is added to the Conversation
        createMessagesAdapter();

        //Grab all the Participants and add them at the top of the screen (the "To:" field)
    }
    //Existing Conversation, so render the messages in the RecyclerView
    private void setupMessagesView() {

        Log.d("Activity", "Conversation exists, setting up view");

        //Hide the "add users" button
        hideAddParticipantsButton();

        //Create the appropriate RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mMessagesView.setLayoutManager(layoutManager);

        //And attach it to the appropriate QueryAdapter, which will automatically update the view
        // when a new Message is added to the Conversation
        createMessagesAdapter();

        //Grab all the Participants and add them at the top of the screen (the "To:" field)
        populateToField(mConversation.getParticipants());
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("inMessageActivity", false);
        installation.saveInBackground();
        super.onDestroy();
    }

    public void onEvent(AddPersonToMessageEvent event) {
        mTargetParticipants = event.mPersonIdList;
        populateToField(mTargetParticipants);
    }

    //Takes a String Array of user IDs, finds the display name, and adds them to the "To:" field
    // at the top of the Messages screen
    private void populateToField(List<String> participantIds){
        //We will not include the Authenticated user in the "To:" field, since they know they are
        // already part of the Conversation
        TextView[] participantList = new TextView[participantIds.size()-1];
        int idx = 0;
        for(String id : participantIds){
            if(!id.equals(LayerImpl.getLayerClient().getAuthenticatedUserId())){

                //Create a new stylized text view
                TextView tv = new TextView(this);
                tv.setText(ParseImpl.getUsername(id));
                tv.setTextSize(16);
                tv.setTextColor(getResources().getColor(R.color.white));
                tv.setPadding(5, 5, 5, 5);
                tv.setBackgroundColor(getResources().getColor(R.color.hack_together_blue));
                participantList[idx] = tv;

                idx++;
            }
        }

        //Uses the helper function to make sure all participant names are appropriately displayed
        // and not cut off due to size constraints
        addViewsToFlowLayout(mParticipantsList, participantList, this);
    }

    //If a Conversation ID was not passed into this Activity, we assume that a new Conversation is
    // being created
    private void createNewConversationView() {

        Log.d("Activity", "Creating a new Conversation");

        //Create the appropriate RecyclerView which will be attached to the QueryController when it
        // is created (after the first message is sent and the Conversation is actually created)
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mMessagesView.setLayoutManager(layoutManager);
    }

    //This is called when there is a valid Conversation to attach the RecyclerView to the appropriate
    // QueryAdapter. Whenever a new Message is sent to the Conversation, the RecyclerView will be updated
    private void createMessagesAdapter(){

        //The Query Adapter drives the RecyclerView, and handles all the heavy lifting of checking
        // for new Messages, and updating the RecyclerView
        mMessagesAdapter = new MessageQueryAdapter(getApplicationContext(), LayerImpl.getLayerClient(), mMessagesView, mConversation, this, new QueryAdapter.Callback() {

            public void onItemInserted() {
                //When a new item is inserted into the RecyclerView, scroll to the bottom so the
                // most recent Message is always displayed
                mMessagesView.smoothScrollToPosition(Integer.MAX_VALUE);
            }
        });
        mMessagesView.setAdapter(mMessagesAdapter);

        //Execute the Query
        mMessagesAdapter.refresh();

        //Start by scrolling to the bottom (newest Message)
        mMessagesView.smoothScrollToPosition(Integer.MAX_VALUE);
    }

    //You can choose to present additional options when a Message is tapped
    public void onMessageClick(Message message) {

    }

    //You can choose to present additional options when a Message is long tapped
    public boolean onMessageLongClick(Message message) {
        return false;
    }


    //Handle the sendButton and Add/Remove Participants button (if displayed)
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.sendButton:
                Log.d("Activity", "Send button pressed");
                sendMessage();
                break;

            case R.id.addParticipants:
                Log.d("Activity", "Add participant button pressed");
                Utils.gotoAddPersonActivity(MessageActivity.this, mTargetParticipants, TAG);
                break;
        }
    }

    //The Authenticated User is actually sending a Message to this Conversation
    private void sendMessage(){

        //First Check to see if we have a valid Conversation object
        if(mConversation == null){
            //Make sure there are valid participants. Since the Authenticated user will always be
            // included in a new Conversation, we check to see if there is more than one target participant
            if(mTargetParticipants != null) {
                if(mTargetParticipants.size() > 1){
                                //Create a new conversation, and tie it to the QueryAdapter
                    mConversation = LayerImpl.getLayerClient().newConversation(mTargetParticipants);
                    createMessagesAdapter();

                    //Once the Conversation object is created, we don't allow changing the Participant List
                    // Note: this is an implementation choice. It is always possible to add/remove participants
                    // after a Conversation has been created
                    hideAddParticipantsButton();}
                else{
                    Toast.makeText(this,Common.ERROR_NO_PERSON_TO_SENT_TO,Toast.LENGTH_SHORT).show();
                }
            } else{
                Toast.makeText(this,Common.ERROR_NO_PERSON_TO_SENT_TO,Toast.LENGTH_SHORT).show();
            }
        }

        //Grab the user's input
        EditText input = (EditText)findViewById(R.id.textInput);
        String text = getTextAsString(input);

        //If the input is valid, create a new Message and send it to the Conversation
        if(mConversation != null && text != null && text.length() > 0){

            MessagePart part = LayerImpl.getLayerClient().newMessagePart(text);
            Message msg = LayerImpl.getLayerClient().newMessage(part);
            mConversation.send(msg);//
            mTargetParticipants = (ArrayList<String>)mConversation.getParticipants();
            Log.d("WILL mTargetParticipants", mTargetParticipants.toString());


            Log.d("WILL mConversation.getId().toString()", mConversation.getId().toString());
            Uri myUri = Uri.parse("layer:///conversations/d5ba6093-d55d-4736-b91e-67d07097ff88");
            Log.d("WILL myUri.toString()", myUri.toString());



            if(mTargetParticipants.size() > 2){
                // Group Chat
                for (String mTargetParticipantId: mTargetParticipants){
                    if(!ParseUser.getCurrentUser().getObjectId().equals(mTargetParticipantId)){
                        ParseUtils.instantMessageNotification(ParseUser.getCurrentUser().getObjectId(), mTargetParticipantId, text, mConversation.getId().toString(), true);
                    }
                }
            }
            else{
                // 1 on 1 Chat
                for (String mTargetParticipantId: mTargetParticipants){
                    if(!ParseUser.getCurrentUser().getObjectId().equals(mTargetParticipantId)){
                        ParseUtils.instantMessageNotification(ParseUser.getCurrentUser().getObjectId(), mTargetParticipantId, text, mConversation.getId().toString(), false);
                    }
                }
            }



            input.setText("");

        } else {
            Toast.makeText(this,Common.ERROR_NO_MESSAGE_WRITTEN, Toast.LENGTH_SHORT).show();
        }
    }

    //When a Conversation has Messages, we disable the ability to Add/Remove participants
    private void hideAddParticipantsButton(){
        if(mAddUserButton != null) {
            mAddUserButton.setVisibility(View.GONE);
        }
    }

    //When the RecyclerView changes size because of the Soft Keyboard, force scroll to the bottom
    // in order to always show the latest message
    protected void onShowKeyboard(int keyboardHeight) {
        mMessagesView.smoothScrollToPosition(Integer.MAX_VALUE);
    }
    protected void onHideKeyboard() {
        mMessagesView.smoothScrollToPosition(Integer.MAX_VALUE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart(){
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("inMessageActivity", true);
        installation.saveInBackground();

        super.onStart();
    }

    @Override
    public void onPause() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("inMessageActivity", false);
        installation.saveInBackground();

        super.onPause();
    }

    @Override
    public void onStop() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("inMessageActivity", false);
        installation.saveInBackground();

        super.onStop();
    }
    //Shows a list of all users that can be added to the Conversation
//    private void showParticipantPicker(){
//
//        //Update user list from Parse
//        ParseImpl.cacheAllUsers();
//
//        //Create a new Dialog Box
//        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
//        helpBuilder.setTitle("Select Participants");
//        helpBuilder.setMessage("Add or remove participants from this conversation:\n");
//
//        //The Linear Layout View that will hold all the CheckBox views
//        LinearLayout checkboxList = new LinearLayout(this);
//        checkboxList.setOrientation(LinearLayout.VERTICAL);
//
//        //Grab a list of all friends (For now, all friends are equivalent to all users except current user)
//        Set friends = ParseImpl.getAllFriends();
//
//        //A Map of the CheckBox with the human readable username and the Parse Object ID (The Value of Key Value pair is Object ID)
//        final HashMap<CheckBox, String> allUsers = new HashMap<>();
//
//        //Create the list of participants if it hasn't been instantiated
//        if(mTargetParticipants == null) mTargetParticipants = new ArrayList<>();
//
//        //Go through each friend and create a Checkbox with a human readable name mapped to the
//        // Object ID
//        Iterator itr = friends.iterator();
//        while(itr.hasNext()) {
//            String friendId = (String)itr.next();
//
//            CheckBox friend = new CheckBox(this);
//            friend.setText(ParseImpl.getUsername(friendId));
//
//            //If this user is already selected, mark the checkbox
//            if(mTargetParticipants.contains(friendId)) friend.setChecked(true);
//
//            checkboxList.addView(friend);
//
//            allUsers.put(friend, friendId);
//        }
//
//        //Add the list of CheckBoxes to the Alert Dialog
//        helpBuilder.setView(checkboxList);
//
//        //When the user is done adding/removing participants, update the list of target users
//        helpBuilder.setPositiveButton("Done",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        // Do nothing but close the dialog
//
//                        //Reset the target user list, and rebuild it based on which checkboxes are selected
//                        mTargetParticipants.clear();
//
////                        mTargetParticipants.add(LayerImpl.getLayerClient().getAuthenticatedUserId());
//                        mTargetParticipants.add(ParseUser.getCurrentUser().getObjectId()); // equivalent to the above line.
//
//                        Set checkboxes = allUsers.keySet();
//                        Iterator checkItr = checkboxes.iterator();
//                        while(checkItr.hasNext()){
//                            CheckBox currCheck = (CheckBox)checkItr.next();
//                            if(currCheck != null && currCheck.isChecked()){
//                                String friendID = allUsers.get(currCheck);
//                                mTargetParticipants.add(friendID);
//                            }
//                        }
//
//                        Log.d("Activity", "Current participants: " + mTargetParticipants.toString());
//
//                        //Draw the list of target users
//                        populateToField(mTargetParticipants);
//                    }
//                }
//        );
//
//
//        // Create and show the dialog box with list of all participants
//        AlertDialog helpDialog = helpBuilder.create();
//        helpDialog.show();
//
//    }
}