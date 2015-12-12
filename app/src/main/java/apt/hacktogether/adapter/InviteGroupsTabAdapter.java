package apt.hacktogether.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import java.util.List;

import apt.hacktogether.R;
import apt.hacktogether.utils.Common;
import apt.hacktogether.utils.ParseUtils;
import apt.hacktogether.utils.Utils;
import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by de-weikung on 12/7/15.
 */
public class InviteGroupsTabAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private List<ParseObject> mList;

    static class ViewHolder {
        @Bind(R.id.txt_group_name) public TextView txtGroupName;
        @Bind(R.id.txt_hackathon_attend) public TextView txtHackathonAttend;
        @Bind(R.id.ll_members) public LinearLayout ll_Members;
        @Bind(R.id.ll_pending_members) public LinearLayout ll_pendingMembers;
        @Bind(R.id.txt_group_interests) public TextView txtGroupInterests;
        @Bind(R.id.txt_look_for_skills) public TextView txtLookForSkills;
        @Bind(R.id.btn_accept) public ImageButton acceptButton;
        @Bind(R.id.btn_reject) public ImageButton rejectButton;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public InviteGroupsTabAdapter(Context context, List<ParseObject> list) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mList.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.card_invite_groups_tab, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ParseObject inviteGroup = mList.get(position);
        holder.txtGroupName.setText(inviteGroup.getString(Common.OBJECT_GROUP_NAME));
        holder.txtHackathonAttend.setText(inviteGroup.getString(Common.OBJECT_GROUP_HACKATHONATTEND));
        getMembers(inviteGroup, holder);
        getPendingMembers(inviteGroup, holder);
        getGroupInterests(inviteGroup, holder);
        getLookForSkills(inviteGroup, holder);

        final View finalConvertView = convertView;
        holder.acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalConvertView.setVisibility(View.GONE);
                ParseUtils.acceptInvitation(inviteGroup);
            }
        });
        holder.rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalConvertView.setVisibility(View.GONE);
                ParseUtils.rejectInvitation(inviteGroup);
            }
        });

        return convertView;
    }

    private void getMembers(ParseObject group, final ViewHolder holder){
        ParseRelation<ParseUser> membersRelation = group.getRelation(Common.OBJECT_GROUP_MEMBERS);
        membersRelation.getQuery().findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> members, ParseException e) {
                // remove first. Otherwise, will have repeated icons.
                holder.ll_Members.removeAllViews();

                for (ParseUser member: members){
                    ParseFile imgFile = member.getParseFile(Common.OBJECT_USER_PROFILE_PIC);

                    CircleImageView imgProfile = new CircleImageView(mContext);
                    LinearLayout.LayoutParams imgProfile_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    imgProfile_params.setMargins(0, 0, 10, 10);
                    imgProfile.setLayoutParams(imgProfile_params);
                    imgProfile.getLayoutParams().height = 80;
                    imgProfile.getLayoutParams().width = 80;
                    imgProfile.setImageResource(R.drawable.ic_account_circle_black_48dp);
                    Picasso.with(mContext)
                            .load(imgFile.getUrl())
                            .into(imgProfile);

                    holder.ll_Members.addView(imgProfile);
                }
            }
        });
    }

    private void getPendingMembers(final ParseObject group, final ViewHolder holder){
        ParseRelation<ParseUser> pendingMembersRelation = group.getRelation(Common.OBJECT_GROUP_PENDINGMEMBERS);
        pendingMembersRelation.getQuery().findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> pendingMembers, ParseException e) {
                // remove first. Otherwise, will have repeated icons.
                holder.ll_pendingMembers.removeAllViews();

                for (ParseUser pendingMember: pendingMembers){
                    ParseFile imgFile = pendingMember.getParseFile(Common.OBJECT_USER_PROFILE_PIC);

                    CircleImageView imgProfile = new CircleImageView(mContext);
                    LinearLayout.LayoutParams imgProfile_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    imgProfile_params.setMargins(0, 0, 10, 0);
                    imgProfile.setLayoutParams(imgProfile_params);
                    imgProfile.getLayoutParams().height = 80;
                    imgProfile.getLayoutParams().width = 80;
                    imgProfile.setImageResource(R.drawable.ic_account_circle_black_48dp);
                    Picasso.with(mContext)
                            .load(imgFile.getUrl())
                            .into(imgProfile);
                    Utils.toGrayScale(imgProfile);

                    holder.ll_pendingMembers.addView(imgProfile);
                }

            }
        });
    }

    private void getGroupInterests(ParseObject group, final ViewHolder holder){
        ParseRelation<ParseObject> groupInterestsRelation = group.getRelation(Common.OBJECT_GROUP_GROUPINTERESTS);
        groupInterestsRelation.getQuery().findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> interests, ParseException e) {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i< interests.size(); i++) {
                    s.append(interests.get(i).getString(Common.OBJECT_INTEREST_NAME));
                    if (i != interests.size()-1) s.append(", ");
                }
                holder.txtGroupInterests.setText(s.toString());
            }
        });
    }

    private void getLookForSkills(ParseObject group, final ViewHolder holder){
        ParseRelation<ParseObject> lookForSkillsRelation = group.getRelation(Common.OBJECT_GROUP_LOOKFORSKILLS);
        lookForSkillsRelation.getQuery().findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> skills, ParseException e) {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i< skills.size(); i++) {
                    s.append(skills.get(i).getString(Common.OBJECT_SKILL_NAME));
                    if (i != skills.size()-1) s.append(", ");
                }
                holder.txtLookForSkills.setText(s.toString());
            }
        });
    }

}
