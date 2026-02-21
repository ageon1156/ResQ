

package org.meshtastic.feature.map.cluster;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.view.MotionEvent;

import org.meshtastic.feature.map.model.MarkerWithLabel;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

public abstract class MarkerClusterer extends Overlay {

	protected static final int FORCE_CLUSTERING = -1;
	
	protected ArrayList<MarkerWithLabel> mItems = new ArrayList<MarkerWithLabel>();
	protected Point mPoint = new Point();
	protected ArrayList<StaticCluster> mClusters = new ArrayList<StaticCluster>();
	protected int mLastZoomLevel;
	protected Bitmap mClusterIcon;
	protected String mName, mDescription;

	public abstract ArrayList<StaticCluster> clusterer(MapView mapView);
	
	public abstract MarkerWithLabel buildClusterMarker(StaticCluster cluster, MapView mapView);
	
	public abstract void renderer(ArrayList<StaticCluster> clusters, Canvas canvas, MapView mapView);
	
	public MarkerClusterer() {
		super();
		mLastZoomLevel = FORCE_CLUSTERING;
	}

	public void setName(String name){
		mName = name;
	}
	
	public String getName(){
		return mName;
	}
	
	public void setDescription(String description){
		mDescription = description;
	}
	
	public String getDescription(){
		return mDescription;
	}

	public void setIcon(Bitmap icon){
		mClusterIcon = icon;
	}

	public void add(MarkerWithLabel marker){
		mItems.add(marker);
	}

	public void invalidate(){
		mLastZoomLevel = FORCE_CLUSTERING; 
	}

	public MarkerWithLabel getItem(int id){
		return mItems.get(id);
	}

	public ArrayList<MarkerWithLabel> getItems(){
		return mItems;
	}

	protected void hideInfoWindows(){
		for (MarkerWithLabel m : mItems){
			if (m.isInfoWindowShown())
				m.closeInfoWindow();
		}
	}

	@Override public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow)
			return;
		
		int zoomLevel = mapView.getZoomLevel();
		if (zoomLevel != mLastZoomLevel && !mapView.isAnimating()){
			hideInfoWindows();
        	mClusters = clusterer(mapView);
        	renderer(mClusters, canvas, mapView);
			mLastZoomLevel = zoomLevel;
		}
		
		for (StaticCluster cluster:mClusters){
			MarkerWithLabel marker = cluster.getMarker();
			marker.draw(canvas, mapView, false);
        }
	}

	public Iterable<StaticCluster> reversedClusters() {
		return new Iterable<StaticCluster>() {
			@Override
			public Iterator<StaticCluster> iterator() {
				final ListIterator<StaticCluster> i = mClusters.listIterator(mClusters.size());
				return new Iterator<StaticCluster>() {
					@Override
					public boolean hasNext() {
						return i.hasPrevious();
					}

					@Override
					public StaticCluster next() {
						return i.previous();
					}

					@Override
					public void remove() {
						i.remove();
					}
				};
			}
		};
	}

	@Override public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView){
		for (final StaticCluster cluster : reversedClusters()) {
			if (cluster.getMarker().onSingleTapConfirmed(event, mapView))
				return true;
		}
		return false;
	}
	
	@Override public boolean onLongPress(final MotionEvent event, final MapView mapView) {
		for (final StaticCluster cluster : reversedClusters()) {
			if (cluster.getMarker().onLongPress(event, mapView))
				return true;
		}
		return false;
	}

	@Override public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
		for (StaticCluster cluster : reversedClusters()) {
			if (cluster.getMarker().onTouchEvent(event, mapView))
				return true;
		}
		return false;
	}

	@Override public boolean onDoubleTap(final MotionEvent event, final MapView mapView) {
		for (final StaticCluster cluster : reversedClusters()) {
			if (cluster.getMarker().onDoubleTap(event, mapView))
				return true;
		}
		return false;
	}

	@Override public BoundingBox getBounds(){
		if (mItems.size() == 0)
				return null;
		double minLat = Double.MAX_VALUE;
		double minLon = Double.MAX_VALUE;
		double maxLat = -Double.MAX_VALUE;
		double maxLon = -Double.MAX_VALUE;
		for (final MarkerWithLabel item : mItems) {
			final double latitude = item.getPosition().getLatitude();
			final double longitude = item.getPosition().getLongitude();
			minLat = Math.min(minLat, latitude);
			minLon = Math.min(minLon, longitude);
			maxLat = Math.max(maxLat, latitude);
			maxLon = Math.max(maxLon, longitude);
		}
		return new BoundingBox(maxLat, maxLon, minLat, minLon);
	}

}

