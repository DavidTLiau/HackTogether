package apt.hacktogether.activity;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import apt.hacktogether.R;
import apt.hacktogether.event.AddInterestToCreateGroupEvent;

import apt.hacktogether.event.AddInterestToCreateProfileEvent;
import apt.hacktogether.event.AddInterestToEditGroupEvent;
import apt.hacktogether.parse.ParseImpl;
import apt.hacktogether.utils.Common;
import apt.hacktogether.utils.Utils;
import de.greenrobot.event.EventBus;


public class AddInterestActivity extends BaseActivity {
    private ArrayList<String> mInterestIdList;
    private String receiveTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_interest);

        Intent it = getIntent();
        receiveTag = it.getStringExtra(Common.EXTRA_TAG);

        // it.getStringArrayListExtra(Common.EXTRA_INTEREST_ID_LIST) might be null
        mInterestIdList = it.getStringArrayListExtra(Common.EXTRA_INTEREST_ID_LIST);

        //Update interest list from Parse
//        ParseImpl.cacheAllInterests();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.select_interests);

        LinearLayout ll_vertical = (LinearLayout) findViewById(R.id.vertical_ll);

        LinearLayout ll_child_vertical = new LinearLayout(this);
        ll_child_vertical.setOrientation(LinearLayout.VERTICAL);
        ll_child_vertical.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Set interestIds = ParseImpl.getAllInterestIds();

        //A Map of the CheckBox with the Parse Object ID
        final HashMap<CheckBox, String> allInterests = new HashMap<>();

        if (mInterestIdList == null) mInterestIdList = new ArrayList<>();

        //Go through each interestID and create a horizontal linear layout with a human readable name mapped to the
        // Object ID
        Iterator itr = interestIds.iterator();
        while(itr.hasNext()) {
            String interestId = (String)itr.next();


            LinearLayout ll_horizontal = new LinearLayout(this);
            ll_horizontal.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams ll_horizontal_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ll_horizontal.setLayoutParams(ll_horizontal_params);

            CheckBox checkBox = new CheckBox(this);
            LinearLayout.LayoutParams checkBox_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            checkBox_params.setMargins(10, 10, 0, 0);
            checkBox.setLayoutParams(checkBox_params);
            checkBox.setButtonDrawable(R.drawable.custom_checkbox_design);
            checkBox.setText(ParseImpl.getInterestName(interestId));
            checkBox.setTextSize(0);
            checkBox.setTextColor(getResources().getColor(R.color.white));
            //If this interest is already selected, mark the checkbox
            if(mInterestIdList.contains(interestId)) checkBox.setChecked(true);

            TextView textView = new TextView(this);
            LinearLayout.LayoutParams textView_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textView_params.setMargins(20, 16, 0, 0);
            textView.setLayoutParams(textView_params);
            textView.setText(ParseImpl.getInterestName(interestId));
            textView.setTextSize(20);
            textView.setTextColor(getResources().getColor(R.color.black));



            ll_horizontal.addView(checkBox);
            ll_horizontal.addView(textView);

            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardView_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardView_params.setMargins(0, 10, 0, 10);
            cardView.setLayoutParams(cardView_params);

            cardView.addView(ll_horizontal);
            ll_child_vertical.addView(cardView);

            allInterests.put(checkBox, interestId);
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.85f));
        scrollView.getLayoutParams().height = 0;
        scrollView.addView(ll_child_vertical);

        ImageButton confirmButton = new ImageButton(this);
        confirmButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.15f));
        confirmButton.getLayoutParams().height = 0;
        confirmButton.setImageResource(R.drawable.ic_check_black_24dp);
        confirmButton.setBackgroundColor(getResources().getColor(R.color.green));
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInterestIdList.clear();

                Set checkboxes = allInterests.keySet();
                Iterator checkItr = checkboxes.iterator();
                while(checkItr.hasNext()){
                    CheckBox currCheck = (CheckBox)checkItr.next();
                    if(currCheck != null && currCheck.isChecked()){
                        String interestID = allInterests.get(currCheck);
                        mInterestIdList.add(interestID);
                    }
                }


                if(receiveTag.equals(Common.TAG_CREATE_GROUP_ACTIVITY)) {
                    // in the corresponding event in CreateGroupActivity, simply do assignment and populate to fields
                    EventBus.getDefault().post(new AddInterestToCreateGroupEvent(mInterestIdList));
                }
                else if(receiveTag.equals(Common.TAG_EDIT_GROUP_ACTIVITY)){
                    EventBus.getDefault().post(new AddInterestToEditGroupEvent(mInterestIdList));
                }
                else if(receiveTag.equals(Common.TAG_CREATE_PROFILE_ACTIVITY)){
                    EventBus.getDefault().post(new AddInterestToCreateProfileEvent(mInterestIdList));
                }
                else if(receiveTag.equals(Common.TAG_EDIT_PROFILE_ACTIVITY)){
                    // TODO
                }

                AddInterestActivity.this.finish();
            }
        });

        ll_vertical.addView(scrollView);
        ll_vertical.addView(confirmButton);
    }

    //Called when the Activity starts, or when the App is coming to the foreground.
    public void onResume() {
        super.onResume();

        // Check to see the state of the LayerClient, and if everything is set up, then good; do nothing.
        Utils.checkSetup(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_interest, menu);
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
}
