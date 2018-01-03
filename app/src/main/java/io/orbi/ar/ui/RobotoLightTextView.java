package io.orbi.ar.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * Created by pacific1 on 8/18/17.
 */

public class RobotoLightTextView extends AppCompatTextView
{

    public RobotoLightTextView(Context context) {
        super(context);
        setTypeface(context);
    }

    public RobotoLightTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setTypeface(context);
    }

    public RobotoLightTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        setTypeface(context);
    }

    private void setTypeface(Context context)
    {
        if (context != null && !isInEditMode())
        {
            Typeface tf= Typeface.createFromAsset(context.getAssets(), "fonts/Roboto_Light.ttf");
            super.setTypeface(tf);
            //setTypeface(io.orbi.ar.MainActivity.getRobotoLightItalic());
        }
    }
}
