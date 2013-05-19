package com.jeffthefate.podcatcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class RelativeLayoutEx extends RelativeLayout {

    public RelativeLayoutEx(Context context) {
        super(context);
    }
    
    public RelativeLayoutEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public RelativeLayoutEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public void setPressed(boolean pressed) {
        if (pressed && (getParent() instanceof View) && 
                ((View) getParent()).isPressed()) {
            return;
        }
        super.setPressed(pressed);
    }

}
