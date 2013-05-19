package com.jeffthefate.podcatcher;

import android.annotation.SuppressLint;
import android.os.Build;
import android.widget.ExpandableListView;

public abstract class VersionedExpandGroup {
    
    public abstract VersionedExpandGroup create(ExpandableListView listView);
    public abstract void expandGroup(int groupPosition);
    
    public static VersionedExpandGroup newInstance() {
        final int sdkVersion = Build.VERSION.SDK_INT;
        VersionedExpandGroup expandGroup = null;
        if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            expandGroup = new GingerbreadExpandGroup();
        else
            expandGroup = new HoneycombActionBar();

        return expandGroup;
    }
    
    private static class GingerbreadExpandGroup extends VersionedExpandGroup {
        ExpandableListView listView;
        
        @Override
        public VersionedExpandGroup create(ExpandableListView listView) {
            this.listView = listView;
            return this;
        }

        @Override
        public void expandGroup(int groupPosition) {
            listView.expandGroup(groupPosition);
        }
    }
    
    private static class HoneycombActionBar extends GingerbreadExpandGroup {
        @SuppressLint("NewApi")
        @Override
        public void expandGroup(int groupPosition) {
            listView.expandGroup(groupPosition, true);
        }
    }
    
}
