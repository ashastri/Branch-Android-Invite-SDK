package io.branch.invite.welcome;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import io.branch.invite.util.AnimatedDialog;
import io.branch.invite.util.BranchInviteUtil;
import io.branch.invite.util.CircularImageView;
import io.branch.invite.util.Defines;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;

/**
 * <p>Calls for creating a welcome dialog. Welcome dialog provide a custom view for the Dialog, which can be customised
 * by providing styles .{@see WelcomeViewStyle} for more details.
 * This class  provides an option to set the custom view for the Welcome Dialog. Dialog content view can be inflated with any
 * custom view provided.{@see WelcomeCallback#getCustomInvitationView()} for more details </p>
 */
class WelcomeHandler {
    private static WelcomeHandler thisInstance_;

    /* The custom chooser dialog for selecting an application to share the link */
    AnimatedDialog invitationHandlerDialog_;
    /* {@link Context} for the invite manager */
    Context context_;
    /* Callback instance for notifying welcome UI events */
    WelcomeCallback callback_;
    /* The view to inflate the dialog content */
    View invitationView_;
    /* Styling parameters for welcome view*/
    WelcomeViewStyle invitationStyle_;
    /* Background task for loading image URl for inviter*/
    LoadBitmapFromUrlTask imageLoadTask_;

    private String fullNameKey_;
    private String shortNameKey_;
    private String imageUrlKey_;

    private WelcomeHandler(String fullNameKey, String shortNameKey, String imageUrlKey) {
        thisInstance_ = this;
        fullNameKey_ = fullNameKey;
        shortNameKey_ = shortNameKey;
        imageUrlKey_ = imageUrlKey;
    }

    /**
     * Check for invitation parameters in the latest referring parameters and creates a welcome dialog
     * with specified styles or custom view. Do nothing if there is no invitation params available for this session.
     *
     * @param context         Context for showing welcome Dialog
     * @param invitationStyle {@link WelcomeViewStyle} instance  to specify the welcome Dialog style
     * @param callback        {@link WelcomeCallback} instance to callback the UI events
     * @return A Dialog instance if a welcome dialog is created. Null if invitation parameters are not available or in case of error
     */
    public static Dialog HandleInvitations(Context context, WelcomeViewStyle invitationStyle, WelcomeCallback callback, ArrayList<String> additionalLookUpKeys,
                                           String fullNameKey, String shortNameKey, String imageUrlKey) {
        if (thisInstance_ == null) {
            thisInstance_ = new WelcomeHandler(fullNameKey, shortNameKey, imageUrlKey);
        }
        return thisInstance_.checkAndHandleInvitations(context, invitationStyle, callback, additionalLookUpKeys);
    }

    private Dialog checkAndHandleInvitations(Context context, WelcomeViewStyle invitationStyle, WelcomeCallback callback, ArrayList<String> additionalLookUpKeys) {
        context_ = context;
        invitationStyle_ = invitationStyle;
        callback_ = callback;
        invitationView_ = null;

        if (Branch.getInstance() != null) {
            JSONObject latestReferringParams = Branch.getInstance().getLatestReferringParams();
            // Check if the link has inviter info. Checking for userID and user Full name which are mandatory if invitation is created with invite SDK
            if (latestReferringParams.has(Defines.INVITE_USER_ID.getKey())
                    && latestReferringParams.has(Defines.INVITE_USER_FULLNAME.getKey())) {

                // The link is referral type.Then get the inviter info
                createInvitationHandlerDialog();
            } else if (isLookUpKeyPresent(additionalLookUpKeys)) {  //Check for any custom look up keys.
                // Found matching key. so Launch welcome dialog.
                createInvitationHandlerDialog();
            } else {
                if (callback_ != null) {
                    callback_.onBranchError(new BranchError("No invitations available for this session ", BranchError.ERR_NO_SESSION));
                }
            }
        } else {
            if (callback_ != null) {
                callback_.onBranchError(new BranchError("Trouble instantiating Branch", BranchError.ERR_BRANCH_NOT_INSTANTIATED));
            }
        }
        return invitationHandlerDialog_;
    }

    /**
     * Check if a lookup key is present in the last reference params
     *
     * @param lookUpKeys list of keys to check for match in latest referring params.
     * @return true if a  matching key is found in the latest referring params.
     */
    private boolean isLookUpKeyPresent(ArrayList<String> lookUpKeys) {
        JSONObject latestReferringParams = Branch.getInstance().getLatestReferringParams();
        for (String lookupKey : lookUpKeys) {
            if (latestReferringParams.has(lookupKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create and show an invitation dialog with the given options.
     */
    private void createInvitationHandlerDialog() {
        JSONObject latestReferringParams = Branch.getInstance().getLatestReferringParams();
        String userID = "";
        if (latestReferringParams.has(Defines.INVITE_USER_ID.getKey())) {
            userID = (String) latestReferringParams.remove(Defines.INVITE_USER_ID.getKey());
        }
        String userFullName = "";
        if (latestReferringParams.has(fullNameKey_)) {
            userFullName = (String) latestReferringParams.remove(fullNameKey_);
        }
        
        String userShortName = "";
        if (latestReferringParams.has(Defines.INVITE_USER_ID.getKey())) {
            userShortName = (String) latestReferringParams.remove(shortNameKey_);
        }
        String userImageUrl = "";
        if (latestReferringParams.has(Defines.INVITE_USER_IMAGE_URL.getKey())) {
            userImageUrl = (String) latestReferringParams.remove(imageUrlKey_);
        }


        // Check if a custom view is desired for invitation.
        if (callback_ != null) {
            invitationView_ = callback_.getCustomInvitationView(userID, userFullName, userShortName, userImageUrl, latestReferringParams);
        }
        //If user has not provided a custom view create a view with style specified.
        if (invitationView_ == null) {
            invitationView_ = new InvitationShowView(context_);
            ((InvitationShowView) invitationView_).updateView(userFullName, userShortName, userImageUrl);
        }

        RelativeLayout invitationCoverLayout = new RelativeLayout(context_);
        invitationCoverLayout.setBackgroundColor(Color.TRANSPARENT);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        invitationCoverLayout.addView(invitationView_, params);
        invitationCoverLayout.setBackgroundColor(Color.WHITE);
        if (invitationHandlerDialog_ != null && invitationHandlerDialog_.isShowing()) {
            invitationHandlerDialog_.cancel();
        }
        invitationHandlerDialog_ = new AnimatedDialog(context_);
        invitationHandlerDialog_.setContentView(invitationCoverLayout);

        if (callback_ != null) {
            callback_.onWelcomeDialogLaunched();
        }

        invitationHandlerDialog_.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (imageLoadTask_ != null) {
                    imageLoadTask_.cancel(true);
                }

                if (callback_ != null) {
                    callback_.onWelcomeDialogDismissed();
                }
            }
        });

        invitationCoverLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (invitationHandlerDialog_ != null && invitationHandlerDialog_.isShowing()) {
                    invitationHandlerDialog_.cancel();
                }
            }
        });
    }

    /**
     * Create the default view for welcome Dialog
     */
    private class InvitationShowView extends LinearLayout {

        int inviterInfoBackground_ = invitationStyle_.getInviteTextColor();
        int inviteMsgBackground_ = invitationStyle_.getWelcomeTextColor();
        int contactPicSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());

        CircularImageView contactImg_;
        TextView inviterInfoText_;
        TextView welcomeMsgText_;
        TextView proceedToAppText;

        public InvitationShowView(Context context) {
            super(context);
            this.setOrientation(VERTICAL);
            LinearLayout.LayoutParams paramsInfo = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, .6f);
            LinearLayout.LayoutParams paramsMsg = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, .4f);

            RelativeLayout inviterInfoLayout = new RelativeLayout(context);
            inviterInfoLayout.setBackgroundColor(inviterInfoBackground_);
            inviterInfoLayout.setPadding(padding, padding, padding, padding);

            contactImg_ = new CircularImageView(context);
            contactImg_.setId(BranchInviteUtil.generateViewId());
            contactImg_.setScaleType(ImageView.ScaleType.FIT_CENTER);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(contactPicSize, contactPicSize);
            layoutParams.topMargin = 3 * padding;
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            inviterInfoLayout.addView(contactImg_, layoutParams);

            inviterInfoText_ = new TextView(context_);
            inviterInfoText_.setBackgroundColor(inviterInfoBackground_);
            inviterInfoText_.setGravity(Gravity.CENTER);
            inviterInfoText_.setTextAppearance(context_, android.R.style.TextAppearance_Large);
            inviterInfoText_.setTextColor(inviteMsgBackground_);
            inviterInfoText_.setMaxLines(3);
            inviterInfoText_.setEllipsize(TextUtils.TruncateAt.END);
            layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = padding;
            layoutParams.addRule(RelativeLayout.BELOW, contactImg_.getId());
            inviterInfoLayout.addView(inviterInfoText_, layoutParams);


            RelativeLayout inviteMsgLayout = new RelativeLayout(context);
            inviteMsgLayout.setBackgroundColor(inviteMsgBackground_);
            inviteMsgLayout.setPadding(padding, padding, padding, padding);

            welcomeMsgText_ = new TextView(context_);
            welcomeMsgText_.setBackgroundColor(inviteMsgBackground_);
            welcomeMsgText_.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            welcomeMsgText_.setTextColor(inviterInfoBackground_);
            welcomeMsgText_.setTextAppearance(context_, android.R.style.TextAppearance_Medium);
            layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            inviteMsgLayout.addView(welcomeMsgText_, layoutParams);

            proceedToAppText = new TextView(context_);
            proceedToAppText.setBackgroundColor(inviteMsgBackground_);
            proceedToAppText.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            proceedToAppText.setTextAppearance(context_, android.R.style.TextAppearance_Small);
            layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            welcomeMsgText_.setTextColor(inviterInfoBackground_);
            layoutParams.bottomMargin = padding;
            inviteMsgLayout.addView(proceedToAppText, layoutParams);

            this.addView(inviterInfoLayout, paramsInfo);
            this.addView(inviteMsgLayout, paramsMsg);

        }

        /**
         * Update the welcome dialog with the inviter info
         *
         * @param userFullName  name of the inviter
         * @param userShortName Short name for the inviter
         * @param imageUrl      Image Url for inviter
         */
        private void updateView(String userFullName, String userShortName, String imageUrl) {
            //Set invitation message
            inviterInfoText_.setText(formatWithName(invitationStyle_.getInvitationMessageText(), userFullName, userShortName));

            //Set Welcome message
            welcomeMsgText_.setText(formatWithName(invitationStyle_.getWelcomeMessageText(), userFullName, userShortName));

            //Set proceed to app text
            proceedToAppText.setText(formatWithName(invitationStyle_.getProceedToAppText(), userFullName, userShortName));

            //Load user image
            imageLoadTask_ = new LoadBitmapFromUrlTask(contactImg_, imageUrl, invitationStyle_.getDefaultContactImg());
            imageLoadTask_.execute();
        }

        private String formatWithName(String rawString, String userFullName, String userShortName) {
            if (rawString.contains(Defines.FULL_NAME_SUB.getKey())) {
                rawString = rawString.replace(Defines.FULL_NAME_SUB.getKey(), userFullName);
            }
            if (rawString.contains(Defines.SHORT_NAME_SUB.getKey())) {
                //ShortName is optional. So fall back to full name in case short name not available
                if (userShortName == null || userShortName.trim().length() < 1) {
                    userShortName = userFullName;
                }
                rawString = rawString.replace(Defines.SHORT_NAME_SUB.getKey(), userShortName);
            }
            return rawString;
        }

    }

    /**
     * Asynchronous task for downloading image specified by the URL
     */
    private class LoadBitmapFromUrlTask extends AsyncTask<Void, Void, Bitmap> {
        final CircularImageView imageView_;
        final String url_;

        LoadBitmapFromUrlTask(CircularImageView imgView, String url, Drawable defaultImage) {
            imageView_ = imgView;
            url_ = url;
            imageView_.setCircularDrawable(defaultImage);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(new URL(url_).openConnection().getInputStream());

            } catch (IOException ignore) {
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            super.onPostExecute(image);
            if (image != null) {
                imageView_.setCircularBitmap(image);
            }
        }
    }

}
