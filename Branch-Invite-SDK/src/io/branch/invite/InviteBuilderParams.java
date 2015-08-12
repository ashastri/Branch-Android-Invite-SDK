package io.branch.invite;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

/**
 * <p>
 * Class for representing parameters for invite  builder.
 * </p>
 */
class InviteBuilderParams {
    /* Selected background for the tab */
    public Drawable tabSelectedBackground_;
    /* Unselected background for the tab */
    public Drawable tabUnselectedBackground_;

    /* Tab action bar positive button text */
    public String positiveButtonText_;
    /* Tab action bar negative button text */
    public String negativeButtonText_;
    /* Text color for negative button */
    public int negativeBtnTextColor;
    /* Text color for positive button */
    public int positiveBtnTextColor;
    /* Background drawable for positive button */
    public Drawable positiveBtnBackground;
    /* Drawable background for negative button */
    public Drawable negativeBtnBackground;
    /* Background drawable for the tab view.*/
    public Drawable backgroundDrawable_;

    /* Name for email contact tab */
    public String emailTabText_;
    /* Name for phone contact tab */
    public String textTabText_;

    /* Drawable to show when there is no profile picture. */
    public Drawable defaultContactPic_;
    /* Drawable mark list item selected. */
    public Drawable selectedIndicator_;
    /* Drawable to mark list item unselected. */
    public Drawable nonSelectedIndicator_;

    public InviteBuilderParams(Context context) {
        tabSelectedBackground_ = new ColorDrawable(Color.parseColor("#FF000088")); //Default selected color for the tabs
        tabUnselectedBackground_ = new ColorDrawable(Color.parseColor("#FF0000DD"));//Default non-selected color for the tabs

        positiveButtonText_ = "Done";
        negativeButtonText_ = "Cancel";
        negativeBtnTextColor = Color.BLUE;
        positiveBtnTextColor = Color.BLUE;
        positiveBtnBackground = new ColorDrawable(Color.TRANSPARENT);
        negativeBtnBackground = new ColorDrawable(Color.TRANSPARENT);
        backgroundDrawable_ = new ColorDrawable(Color.WHITE);

        emailTabText_ = "Email";
        textTabText_ = "Text";

        defaultContactPic_ = new ColorDrawable(Color.GRAY);
        selectedIndicator_ = context.getResources().getDrawable(android.R.drawable.checkbox_on_background);
        nonSelectedIndicator_ = context.getResources().getDrawable(android.R.drawable.checkbox_off_background);
    }

}