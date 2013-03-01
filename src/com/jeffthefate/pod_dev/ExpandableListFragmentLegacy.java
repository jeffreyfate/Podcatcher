package com.jeffthefate.pod_dev;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This class has originally been taken from
 * http://stackoverflow.com/questions/6051050/expandablelistfragment-with-
 *      loadermanager-for-compatibility-package
 * and then modified by Manfred Moser <manfred@simpligility.com> to get it to 
 * work with the v4 r4 compatibility library. With inspirations from the 
 * library source.
 *
 * All ASLv2 licensed.
 */
public class ExpandableListFragmentLegacy extends Fragment implements 
        OnCreateContextMenuListener, OnChildClickListener, 
        OnGroupCollapseListener, OnGroupExpandListener, OnGroupClickListener {

    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    final private AdapterView.OnItemClickListener mOnClickListener = 
        new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, 
                    long id) {
                onListItemClick((ListView) parent, v, position, id);
        }
    };

    ExpandableListAdapter mAdapter;
    ExpandableListView mList;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mListContainer;
    boolean mSetEmptyText;
    boolean mListShown;

    public ExpandableListFragmentLegacy() {
    }

    /**
     * Provide default implementation to return a simple list view.  Subclasses
     * can override to replace with their own layout.  If doing so, the
     * returned view hierarchy <em>must</em> have a ListView whose id
     * is {@link android.R.id#list android.R.id.list} and can optionally
     * have a sibling view id {@link android.R.id#empty android.R.id.empty}
     * that is to be shown when the list is empty.
     * <p/>
     * <p>If you are overriding this method with your own custom content,
     * consider including the standard layout 
     * {@link android.R.layout#list_content}
     * in your layout file, so that you continue to retain all of the standard
     * behavior of ListFragment.  In particular, this is currently the only
     * way to have the built-in indeterminant progress state be shown.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
            Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(getActivity());

        FrameLayout lframe = new FrameLayout(getActivity());
        lframe.setId(INTERNAL_LIST_CONTAINER_ID);

        TextView tv = new TextView(getActivity());
        tv.setId(INTERNAL_EMPTY_ID);
        tv.setGravity(Gravity.CENTER);
        lframe.addView(tv,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT));

        ExpandableListView lv = new ExpandableListView(getActivity());
        lv.setId(android.R.id.list);
        lv.setDrawSelectorOnTop(false);
        lframe.addView(lv,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(lframe, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT));

        ListView.LayoutParams lp =
                new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT);
        root.setLayoutParams(lp);

        return root;
    }

    /**
     * Attach to list view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    /** Detach from list view. */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        super.onDestroyView();
    }

    /**
     * This method will be called when an item in the list is selected.
     * Subclasses should override. Subclasses can call
     * getListView().getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     * @param l        The ListView where the click happened
     * @param v        The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     */
    public void onListItemClick(ListView l, View v, int position, long id) {
    }

    /** Provide the cursor for the list view. */
    public void setListAdapter(ExpandableListAdapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            if (!mListShown && !hadAdapter) {
                setListShown(true, getView().getWindowToken() != null);
            }
        }
    }

    public ExpandableListView getExpandableListView() {
        ensureList();
        return mList;
    }

    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     * @param shown   If true, the list view is shown; if false, the progress
     *                indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     *                new state.
     */
    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (mListContainer != null) {
            if (shown) {
                if (animate) {
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_in));
                }
                mListContainer.setVisibility(View.VISIBLE);
            } else {
                if (animate) {
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_out));
                }
                mListContainer.setVisibility(View.GONE);
            }
        }
    }

    /** Get the ListAdapter associated with this activity's ListView. */
    public ExpandableListAdapter getExpandableListAdapter() {
        return mAdapter;
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof ExpandableListView) {
            mList = (ExpandableListView) root;
        } else {
            mStandardEmptyView = (TextView)root.findViewById(INTERNAL_EMPTY_ID);
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty);
            }
            mListContainer = root.findViewById(INTERNAL_LIST_CONTAINER_ID);
            View rawListView = root.findViewById(android.R.id.list);
            if (!(rawListView instanceof ExpandableListView)) {
                if (rawListView == null) {
                    throw new RuntimeException("Your content must have a " +
                            "ExpandableListView whose id attribute is " + 
                            "'android.R.id.list'");
                }
                throw new RuntimeException("Content has view with id " + 
                        "attribute 'android.R.id.list' that is not a " + 
                        "ExpandableListView class");
            }
            mList = (ExpandableListView) rawListView;
            if (mEmptyView != null) {
                mList.setEmptyView(mEmptyView);
            }
        }
        mListShown = true;
        mList.setOnItemClickListener(mOnClickListener);
        if (mAdapter != null) {
            setListAdapter(mAdapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            setListShown(false, false);
        }
        mHandler.post(mRequestFocus);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, 
            ContextMenuInfo menuInfo) {
    }
    
    @Override
    public void onResume() {
        super.onResume();
        ExpandableListView eListView = getExpandableListView();
        eListView.setOnGroupClickListener(this);
        eListView.setOnChildClickListener(this);
        eListView.setOnGroupExpandListener(this);
        eListView.setOnGroupCollapseListener(this);
    }
    
    @Override
    public boolean onGroupClick(ExpandableListView parent, View v,
            int groupPosition, long id) {
        if (!parent.isGroupExpanded(groupPosition)) {
            parent.expandGroup(groupPosition);
        }
        else {
            parent.collapseGroup(groupPosition);
        }
        return true;
    }
    
    @Override
    public void onGroupExpand(int groupPosition) {}

    @Override
    public void onGroupCollapse(int groupPosition) {}

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, 
            int groupPosition, int childPosition, long id) {
        return false;
    }
}
