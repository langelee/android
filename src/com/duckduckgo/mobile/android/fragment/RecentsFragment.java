package com.duckduckgo.mobile.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.duckduckgo.mobile.android.R;
import com.duckduckgo.mobile.android.adapters.DDGPagerAdapter;
import com.duckduckgo.mobile.android.views.SlidingTabLayout;

public class RecentsFragment extends Fragment {

    public static final String TAG = "recents_fragment";

    private ViewPager viewPager;
    private DDGPagerAdapter pagerAdapter;
    private SlidingTabLayout slidingTabLayout;

    private View fragmentView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_saved_recents, container, false);
        return fragmentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pagerAdapter = new DDGPagerAdapter(getChildFragmentManager(), new String[] {"Recent searches", "Recents"}, new Fragment[] {new RecentResultTabFragment(), new RecentSearchFragment()});
        viewPager = (ViewPager) fragmentView.findViewById(R.id.view_pager);
        viewPager.setAdapter(pagerAdapter);
        slidingTabLayout = (SlidingTabLayout) fragmentView.findViewById(R.id.sliding_tabs);
        slidingTabLayout.setViewPager(viewPager);
    }
}
