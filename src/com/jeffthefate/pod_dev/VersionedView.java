package com.jeffthefate.pod_dev;

import android.os.Build;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;

public abstract class VersionedView {
    
    public abstract VersionedView create(View view);
    public abstract void setAlpha(float alpha);
    
    public static VersionedView newInstance() {
        final int sdkVersion = Build.VERSION.SDK_INT;
        VersionedView view = null;
        if (sdkVersion < Build.VERSION_CODES.HONEYCOMB)
            view = new GingerbreadView();
        else
            view = new HoneycombView();

        return view;
    }
    
    private static class GingerbreadView extends
            VersionedView {
        View view;
        @Override
        public VersionedView create(View view) {
            this.view = view;
            return this;
        }

        @Override
        public void setAlpha(float alpha) {
            AlphaAnimation invisibleAnim =
                new AlphaAnimation(alpha != 0.0f ? 0.0f : 1.0f, alpha);
            invisibleAnim.setDuration(0);
            invisibleAnim.setInterpolator(new LinearInterpolator());
            view.startAnimation(invisibleAnim);
        }
    }
    
    private static class HoneycombView extends GingerbreadView {
        @Override
        public void setAlpha(float alpha) {
            view.setAlpha(alpha);
        }
    }
    
}
