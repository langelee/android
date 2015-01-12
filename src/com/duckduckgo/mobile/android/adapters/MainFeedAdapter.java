package com.duckduckgo.mobile.android.adapters;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.duckduckgo.mobile.android.DDGApplication;
import com.duckduckgo.mobile.android.R;
import com.duckduckgo.mobile.android.bus.BusProvider;
import com.duckduckgo.mobile.android.download.AsyncImageView;
import com.duckduckgo.mobile.android.download.Holder;
import com.duckduckgo.mobile.android.events.externalEvents.SendToExternalBrowserEvent;
import com.duckduckgo.mobile.android.events.saveEvents.SaveStoryEvent;
import com.duckduckgo.mobile.android.events.saveEvents.UnSaveStoryEvent;
import com.duckduckgo.mobile.android.events.shareEvents.ShareFeedEvent;
import com.duckduckgo.mobile.android.objects.FeedObject;
import com.duckduckgo.mobile.android.util.DDGControlVar;
import com.duckduckgo.mobile.android.util.DDGUtils;
import com.squareup.picasso.Picasso;


public class MainFeedAdapter extends ArrayAdapter<FeedObject> {
	private static final String TAG = "MainFeedAdapter";
	
	private Context context;
	private final LayoutInflater inflater;
		
	public OnClickListener sourceClickListener;
	
	private SimpleDateFormat dateFormat;
	private Date lastFeedDate = null;
	
	private String markedItem = null;
	
	private AlphaAnimation blinkanimation = null;
	
	//TODO: Should share this image downloader with the autocompleteresults adapter instead of creating a second one...
				
	public MainFeedAdapter(Context context, OnClickListener sourceClickListener) {
		super(context, 0);
		this.context = context; 
		inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.sourceClickListener = sourceClickListener;
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		// animation to use for blinking cue
		blinkanimation = new AlphaAnimation(1, 0.3f);
		blinkanimation.setDuration(300);
		blinkanimation.setInterpolator(new LinearInterpolator());
		blinkanimation.setRepeatCount(2);
		blinkanimation.setRepeatMode(Animation.REVERSE);
	}
	
	@Override
	public View getView(int position, View cv, ViewGroup parent) {
		if (cv == null) {
			cv = inflater.inflate(R.layout.temp_main_feed_layout, null);
			Holder holder = new Holder((Toolbar) cv.findViewById(R.id.feedWrapper),
					(TextView)cv.findViewById(R.id.feedTitleTextView),
					(TextView)cv.findViewById(R.id.feedCategotyTextView),
					(AsyncImageView)cv.findViewById(R.id.feedItemBackground),
					(AsyncImageView)cv.findViewById(R.id.feedItemSourceIcon));
			holder.toolbar.inflateMenu(R.menu.feed);
			cv.setTag(holder);
		}
		
		final FeedObject feed = getItem(position);
		
		final Holder holder = (Holder) cv.getTag();
		URL feedUrl = null;

		if (feed != null) {			
			
			//Download the background image
			
			if (feed.getImageUrl() != null && !feed.getImageUrl().equals("null")) {
				Picasso.with(context)
		    	.load(feed.getImageUrl())
		    	.resize(DDGUtils.displayStats.feedItemWidth, DDGUtils.displayStats.feedItemHeight)
		    	.centerCrop()
		    	.placeholder(android.R.color.transparent)
		    	.into(holder.imageViewBackground);
			}

			String feedType = feed.getType();
			
			holder.imageViewFeedIcon.setType(feedType);	// stored source id in imageview
			holder.imageViewFeedIcon.setOnClickListener(sourceClickListener);
			
			final View iconParent = (View) cv.findViewById(R.id.feedWrapper);
			iconParent.post(new Runnable() {
	            public void run() {
	                // Post in the parent's message queue to make sure the parent
	                // lays out its children before we call getHitRect()
	                Rect delegateArea = new Rect();
	                AsyncImageView delegate = holder.imageViewFeedIcon;
	                delegate.getHitRect(delegateArea);
	                delegateArea.top = 0;
	                delegateArea.bottom = iconParent.getBottom();
	                delegateArea.left = 0;
	                // right side limit also considers the space that is available from TextView, without text displayed
	                // in TextView padding area on the left
	                delegateArea.right = holder.textViewTitle.getLeft() + holder.textViewTitle.getPaddingLeft();
	                TouchDelegate expandedArea = new TouchDelegate(delegateArea,
	                        delegate);
	                // give the delegate to an ancestor of the view we're delegating the area to
	                if (View.class.isInstance(delegate.getParent())) {
	                    ((View) delegate.getParent())
	                            .setTouchDelegate(expandedArea);
	                }
	            };
	        });

			//Set the Title
			holder.textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, DDGControlVar.mainTextSize);
			holder.textViewTitle.setText(feed.getTitle());
			
			String feedId = feed.getId();

			if(DDGControlVar.readArticles.contains(feedId)){
				holder.textViewTitle.setTextColor(Color.GRAY);
			}

			//set the category
			//todo insert size
			holder.textViewCategory.setText(feed.getCategory().toUpperCase());

			//set the toolbar Menu
            if(DDGApplication.getDB().isSaved(feedId)) {
                holder.toolbar.getMenu().findItem(R.id.action_add_favourites).setVisible(false);
                holder.toolbar.getMenu().findItem(R.id.action_remove_favourites).setVisible(true);
            } else {
                holder.toolbar.getMenu().findItem(R.id.action_add_favourites).setVisible(true);
                holder.toolbar.getMenu().findItem(R.id.action_remove_favourites).setVisible(false);
            }
			holder.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem menuItem) {
					switch(menuItem.getItemId()) {
						case R.id.action_add_favourites:
							Log.e("aaa", "action add favourites");
                            BusProvider.getInstance().post(new SaveStoryEvent(feed));
                            holder.toolbar.getMenu().findItem(R.id.action_add_favourites).setVisible(false);
                            holder.toolbar.getMenu().findItem(R.id.action_remove_favourites).setVisible(true);
							//add to favourites
							return true;
                        case R.id.action_remove_favourites:
                            BusProvider.getInstance().post(new UnSaveStoryEvent(feed.getId()));
                            holder.toolbar.getMenu().findItem(R.id.action_add_favourites).setVisible(true);
                            holder.toolbar.getMenu().findItem(R.id.action_remove_favourites).setVisible(false);
                            return true;
						case R.id.action_share:
							Log.e("aaa", "action share");
                            BusProvider.getInstance().post(new ShareFeedEvent(feed.getTitle(), feed.getUrl()));
							//action share
							return true;
						case R.id.action_external:
							Log.e("aaa", "action external view in chrome");
                            BusProvider.getInstance().post(new SendToExternalBrowserEvent(getContext(), feed.getUrl()));
							//view in chrome
							return true;
						default:
							return false;
					}
				}
			});
			
			if (feed.getFeed() != null && !feed.getFeed().equals("null")) {
				try {
					feedUrl = new URL(feed.getFeed());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				
				if (feedUrl != null) {
						String host = feedUrl.getHost();
						if (host.indexOf(".") != host.lastIndexOf(".")) {
							//Cut off the beginning, because we don't want/need it
							host = host.substring(host.indexOf(".")+1);
						}
						
						Bitmap bitmap = DDGApplication.getImageCache().getBitmapFromCache("DUCKDUCKICO--" + feedType, false);
						if(bitmap != null){
							holder.imageViewFeedIcon.setBitmap(bitmap);
						}
				}
			}
		}
		
		if(cv != null) {
			if(markedItem != null && markedItem.equals(feed.getId())) {			
				blinkanimation.reset();
				cv.startAnimation(blinkanimation);
			}
			else {
				cv.setAnimation(null);
			}
		}
		
		return cv;
	}
	

	public void setList(List<FeedObject> feed) {
		this.clear();
		for (FeedObject next : feed) {
			this.add(next);
		}
	}
	
	public void addData(List<FeedObject> feed) {
		if(this.lastFeedDate == null) {
			setList(feed);
			return;
		}
		
		Date tmpFeedDate = null;
		for (FeedObject next : feed) {
			try {
				tmpFeedDate = this.dateFormat.parse(next.getTimestamp());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			if(tmpFeedDate == null || !tmpFeedDate.after(lastFeedDate)) {
				return;
			}
			
			this.insert(next, 0);
			this.lastFeedDate = tmpFeedDate;
		}
	}
	
	/**
	 * Mark a list item position to be blinked
	 * @param itemPos
	 */
	public void mark(String itemId) {
		markedItem = itemId;
	}
	
	public void unmark() {
		markedItem = null;
	}

}
