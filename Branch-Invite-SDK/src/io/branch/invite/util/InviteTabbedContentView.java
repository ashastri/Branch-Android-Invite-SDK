package io.branch.invite.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.branch.invite.InviteContactListView;
import io.branch.invite.TabBuilderParams;
import io.branch.referral.BranchError;

/**
 * Tabbed view to show the contact list items.
 * The view contains a tab view and its content which is a list of contact. Also add top action button
 * for positive and negative actions.
 */
@SuppressLint("ViewConstructor")
class InviteTabbedContentView extends LinearLayout {
    /* Tab host for this tabbed view */
    TabHost host_;
    /* Context for creating the view */
    Context context_;
    private int padding_;

    /* Callback for tab events */
    IContactTabViewEvents contactTabViewEventsCallback_;
    /* Builder params for the invite tabbed dialog */
    TabBuilderParams inviteBuilderParams_;
    /* Map for keeping the tabs added to the tabbed view */
    final Map<String, InviteContactListView> tabContentMap_;

    Cursor emailCursor_;
    Cursor phoneCursor_;

    /**
     * Creates a Invite content with action buttons and default tabs.
     *
     * @param context               A {@link Context} for the view
     * @param IContactTabViewEvents Instance of {@link InviteTabbedContentView.IContactTabViewEvents} to update invite view events
     */
    public InviteTabbedContentView(Context context, IContactTabViewEvents IContactTabViewEvents, TabBuilderParams inviteBuilderParams) {
        super(context);
        context_ = context;
        setOrientation(VERTICAL);
        inviteBuilderParams_ = inviteBuilderParams;
        BranchInviteUtil.setViewBackground(this, inviteBuilderParams_.backgroundDrawable_);
        contactTabViewEventsCallback_ = IContactTabViewEvents;
        tabContentMap_ = new HashMap<>();

        padding_ = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        initTabView();
    }

    public void onClose(){
        if(phoneCursor_ != null) {
            phoneCursor_.close();
        }
        if(emailCursor_ != null) {
            emailCursor_.close();
        }
    }
    /**
     * Initialise  the invite  view and setup the default tabs.
     */
    private void initTabView() {
        // Add Title if specified
        if (inviteBuilderParams_.titleTxtVew_ != null) {
            this.addView(inviteBuilderParams_.titleTxtVew_, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // Add action buttons
        LinearLayout controlCover = new LinearLayout(context_);
        controlCover.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        controlCover.setPadding(padding_ * 2, padding_, padding_ * 2, padding_);

        TextView negativeButton = new TextView(context_);
        negativeButton.setText(inviteBuilderParams_.negativeButtonText_);
        negativeButton.setBackgroundColor(Color.TRANSPARENT);
        negativeButton.setTextAppearance(context_, android.R.style.TextAppearance_Large);
        negativeButton.setTypeface(null, Typeface.BOLD);
        negativeButton.setGravity(Gravity.CENTER);
        negativeButton.setTextColor(inviteBuilderParams_.negativeBtnTextColor);
        BranchInviteUtil.setViewBackground(negativeButton, inviteBuilderParams_.negativeBtnBackground);

        negativeButton.setOnClickListener(negativeButtonClickListener_);


        TextView positiveButton = new TextView(context_);
        positiveButton.setText(inviteBuilderParams_.positiveButtonText_);
        positiveButton.setBackgroundColor(Color.TRANSPARENT);
        positiveButton.setTextAppearance(context_, android.R.style.TextAppearance_Large);
        positiveButton.setTypeface(null, Typeface.BOLD);
        positiveButton.setGravity(Gravity.CENTER);

        positiveButton.setTextColor(inviteBuilderParams_.positiveBtnTextColor);
        BranchInviteUtil.setViewBackground(positiveButton, inviteBuilderParams_.positiveBtnBackground);

        positiveButton.setOnClickListener(positiveButtonClickListener_);

        RelativeLayout leftLayout = new RelativeLayout(context_); //Cover layout for buttons
        RelativeLayout rightLayout = new RelativeLayout(context_);

        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftLayout.addView(negativeButton, params1);
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rightLayout.addView(positiveButton, params2);

        controlCover.addView(leftLayout, params);
        controlCover.addView(rightLayout, params);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.addView(controlCover, params);


        //Add Tab view
        host_ = new TabHost(context_, null);
        host_.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        host_.setPadding(padding_, padding_, padding_, padding_);

        TabWidget widget = new TabWidget(context_);
        widget.setId(android.R.id.tabs);
        host_.addView(widget, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        widget.setBackgroundColor(Color.DKGRAY);

        host_.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                setTabBackGround();
            }
        });


        FrameLayout contentLayout = new FrameLayout(context_);
        contentLayout.setId(android.R.id.tabcontent);
        host_.addView(contentLayout, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        host_.setup();

        addEmailTab();
        addTextTab();

        addCustomTabs();

        this.addView(host_);
        //Set content offset
        int contentOffset = host_.getTabWidget().getChildAt(host_.getTabWidget().getChildCount() - 1).getLayoutParams().height + padding_;
        contentLayout.setPadding(0, contentOffset, 0, 0);
        setTabBackGround();
    }

    private void addCustomTabs() {
        Set<String> keys = inviteBuilderParams_.customTabMap_.keySet();
        for (String key : keys) {
            addTab(key, inviteBuilderParams_.customTabMap_.get(key));
        }
    }

    /**
     * Adds the email contact list to the view. Query the Email Content URI for the available emails.
     */
    private void addEmailTab() {
        Uri uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String[] projection;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            projection = new String[]{ContactsContract.CommonDataKinds.Email._ID,
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.PHOTO_THUMBNAIL_URI};
        } else {
            projection = new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.CommonDataKinds.Email._ID,
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Email.TYPE};
        }

        emailCursor_ = context_.getContentResolver()
                .query(uri, projection, null, null, ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " ASC");
        ContactListAdapter adapter = new ContactListAdapterEmail(context_, emailCursor_,
                contactTabViewEventsCallback_, inviteBuilderParams_);

        if (emailCursor_ != null && emailCursor_.getCount() > 0) {
            addTab(inviteBuilderParams_.emailTabText_, addIndexViewToList(new ContactListView(context_, adapter, "Email", "com.google.android.gm")));
        } else {
            addTabForEmptyContactList(inviteBuilderParams_.emailTabText_);
        }
    }

    /**
     * Adds the phone contact list to the view. Query the Phone Content URI for the available phone numbers.
     */
    private void addTextTab() {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI};
        } else {
            projection = new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE};
        }
        phoneCursor_ = context_.getContentResolver()
                .query(uri, projection, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        ContactListAdapter adapter = new ContactListAdapterPhone(context_, phoneCursor_,
                contactTabViewEventsCallback_, inviteBuilderParams_);

        if (phoneCursor_ != null && phoneCursor_.getCount() > 0) {
            addTab(inviteBuilderParams_.textTabText_, addIndexViewToList(new ContactListView(context_, adapter, "Message", "vnd.android-dir/mms-sms")));

        } else {
            addTabForEmptyContactList(inviteBuilderParams_.textTabText_);
        }
    }

    private void addTab(String tabName, final View view) {
        TabHost.TabSpec textTab = host_.newTabSpec(tabName).setIndicator(tabName).setContent(new TabHost.TabContentFactory() {
            @SuppressLint("NewApi")
            @Override
            public View createTabContent(String tag) {
                tabContentMap_.put(tag, (ContactListView)view.getTag());
                return view;
            }
        });

        host_.addTab(textTab);
    }

    private void addTabForEmptyContactList(String tabName) {
        TabHost.TabSpec textTab = host_.newTabSpec(tabName).setIndicator(tabName).setContent(new TabHost.TabContentFactory() {
            @SuppressLint("NewApi")
            @Override
            public View createTabContent(String tag) {
                TextView noContactTxt = new TextView(context_);
                noContactTxt.setText(inviteBuilderParams_.noContactAvailableMsg_);
                noContactTxt.setTextAppearance(context_, android.R.style.TextAppearance_Medium);
                noContactTxt.setTextColor(Color.GRAY);
                noContactTxt.setGravity(Gravity.CENTER);
                noContactTxt.setPadding(padding_, padding_ * 2, padding_, padding_);
                return noContactTxt;
            }
        });

        host_.addTab(textTab);
    }

    private void addTab(String tabName, final InviteContactListView listView) {
        TabHost.TabSpec textTab = host_.newTabSpec(tabName).setIndicator(tabName).setContent(new TabHost.TabContentFactory() {
            @SuppressLint("NewApi")
            @Override
            public View createTabContent(String tag) {
                tabContentMap_.put(tag, listView);
                return listView;
            }
        });

        host_.addTab(textTab);
    }

    private View addIndexViewToList(ContactListView contactListView) {
        RelativeLayout layout = new RelativeLayout(context_);
        String[] indexArray = (String[]) ((ContactListAdapter) contactListView.getAdapter()).getSections();
        IndexList indexView = new IndexList(context_, new ArrayList<>(Arrays.asList(indexArray)), contactListView);
        RelativeLayout.LayoutParams indexViewParam = new RelativeLayout.LayoutParams(padding_ * 6, ViewGroup.LayoutParams.MATCH_PARENT);
        indexViewParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        layout.addView(indexView, indexViewParam);
        layout.addView(contactListView, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setTag(contactListView);
        return layout;
    }


    private OnClickListener negativeButtonClickListener_ = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (contactTabViewEventsCallback_ != null) {
                contactTabViewEventsCallback_.onNegativeButtonClicked();
            }
        }
    };

    private OnClickListener positiveButtonClickListener_ = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (contactTabViewEventsCallback_ != null) {
                contactTabViewEventsCallback_.onPositiveButtonClicked(getSelectedContactList(), getInviteChannel(), getContactListView());
            }
        }
    };

    public void setTabBackGround() {
        for (int i = 0; i < host_.getTabWidget().getChildCount(); i++) {
            //noinspection deprecation
            host_.getTabWidget().getChildAt(i).setBackgroundDrawable(inviteBuilderParams_.tabUnselectedBackground_); // Unselected Tabs
        }
        //noinspection deprecation Note:SetBackground is not working for tab widget
        host_.getTabWidget().getChildAt(host_.getCurrentTab()).setBackgroundDrawable(inviteBuilderParams_.tabSelectedBackground_);// selected tab
    }

    public interface IContactTabViewEvents {
        /* Called on user selecting the negative button */
        void onNegativeButtonClicked();

        /* Called on user selecting the positive button */
        void onPositiveButtonClicked(ArrayList<String> selectedContactName, String selectedChannelName, InviteContactListView listView);

        /* Called when user select a tab*/
        void onContactSelected(ContactListAdapter.MyContact contact);

    }

    private class ContactListView extends InviteContactListView {
        final ContactListAdapter listAdapter_;
        final String channelName_;
        final String targetPackageName_;

        @SuppressLint("NewApi")
        public ContactListView(Context context, ContactListAdapter adapter, String channelName, String targetPackage) {
            super(context);
            listAdapter_ = adapter;
            channelName_ = channelName;
            targetPackageName_ = targetPackage;
            setAdapter(adapter);
            BranchInviteUtil.setViewBackground(this, new ColorDrawable(Color.TRANSPARENT));
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                this.setFastScrollAlwaysVisible(true);
            }
            this.setFastScrollEnabled(true);


        }

        @Override
        public String getInviteChannelName() {
            return channelName_;
        }

        @Override
        public ArrayList<String> getSelectedContacts() {
            return listAdapter_.getSelectedContacts();
        }

        @Override
        public void onInvitationLinkCreated(String invitationUrl, BranchError error) {

        }


        @Override
        public Intent getInviteIntent(String referralUrl, ArrayList<String> selectedUsers, String subject, String message) {
            return listAdapter_.getInviteIntent(referralUrl, selectedUsers, subject, message);
        }


    }

    /**
     * Gets the list of contact selected on the current tab.
     *
     * @return An {@link ArrayList<String>} of selected contacts
     */
    private ArrayList<String> getSelectedContactList() {
        ArrayList<String> selectedContacts = new ArrayList<>();
        InviteContactListView inviteContactListView = tabContentMap_.get(host_.getCurrentTabTag());
        if (inviteContactListView != null) {
            selectedContacts = inviteContactListView.getSelectedContacts();
        }
        return selectedContacts;
    }

    /**
     * Get the invite channel for the current tab
     *
     * @return Channel name for the current tab.
     */
    private String getInviteChannel() {
        String channel = null;
        InviteContactListView inviteContactListView = tabContentMap_.get(host_.getCurrentTabTag());
        if (inviteContactListView != null) {
            channel = inviteContactListView.getInviteChannelName();
        }
        return channel;
    }

    /**
     * Get the content view for the current tab
     *
     * @return A {@link InviteContactListView} instance, which is the content view for current slected tab.
     */
    private InviteContactListView getContactListView() {
        return tabContentMap_.get(host_.getCurrentTabTag());
    }



}
