package com.jeffthefate.podcatcher;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FullScreenImageView extends ImageView {

    public FullScreenImageView(Context context) {
        super(context);
    }

    public FullScreenImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullScreenImageView(Context context, AttributeSet attrs, 
            int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width;
        //int height = width * getDrawable().getIntrinsicHeight() / 
        //        getDrawable().getIntrinsicWidth();
        setMeasuredDimension(width, height);
    }
}