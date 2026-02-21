

package org.meshtastic.feature.map.cluster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.meshtastic.feature.map.model.MarkerWithLabel;

import org.osmdroid.bonuspack.R;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Iterator;

public class RadiusMarkerClusterer extends MarkerClusterer {

    protected int mMaxClusteringZoomLevel = 7;
    protected int mRadiusInPixels = 100;
    protected double mRadiusInMeters;
    protected Paint mTextPaint;
    private ArrayList<MarkerWithLabel> mClonedMarkers;
    protected boolean mAnimated;
    int mDensityDpi;

    public float mAnchorU = MarkerWithLabel.ANCHOR_CENTER, mAnchorV = MarkerWithLabel.ANCHOR_CENTER;
    
    public float mTextAnchorU = MarkerWithLabel.ANCHOR_CENTER, mTextAnchorV = MarkerWithLabel.ANCHOR_CENTER;

    public RadiusMarkerClusterer(Context ctx) {
        super();
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(15 * ctx.getResources().getDisplayMetrics().density);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setAntiAlias(true);
        Drawable clusterIconD = ctx.getResources().getDrawable(R.drawable.marker_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        setIcon(clusterIcon);
        mAnimated = true;
        mDensityDpi = ctx.getResources().getDisplayMetrics().densityDpi;
    }

    public Paint getTextPaint(){
        return mTextPaint;
    }

    public void setRadius(int radius){
        mRadiusInPixels = radius;
    }

    public void setMaxClusteringZoomLevel(int zoom){
        mMaxClusteringZoomLevel = zoom;
    }

    @Override public ArrayList<StaticCluster> clusterer(MapView mapView) {

        ArrayList<StaticCluster> clusters = new ArrayList<StaticCluster>();
        convertRadiusToMeters(mapView);

        mClonedMarkers = new ArrayList<MarkerWithLabel>(mItems); 
        while (!mClonedMarkers.isEmpty()) {
            MarkerWithLabel m = mClonedMarkers.get(0);
            StaticCluster cluster = createCluster(m, mapView);
            clusters.add(cluster);
        }
        return clusters;
    }

    private StaticCluster createCluster(MarkerWithLabel m, MapView mapView) {
        GeoPoint clusterPosition = m.getPosition();

        StaticCluster cluster = new StaticCluster(clusterPosition);
        cluster.add(m);

        mClonedMarkers.remove(m);

        if (mapView.getZoomLevel() > mMaxClusteringZoomLevel) {
        	
        	return cluster;
        }

        Iterator<MarkerWithLabel> it = mClonedMarkers.iterator();
        while (it.hasNext()) {
            MarkerWithLabel neighbor = it.next();
            double distance = clusterPosition.distanceToAsDouble(neighbor.getPosition());
            if (distance <= mRadiusInMeters) {
                cluster.add(neighbor);
                it.remove();
            }
        }

        return cluster;
    }

    @Override public MarkerWithLabel buildClusterMarker(StaticCluster cluster, MapView mapView) {
        MarkerWithLabel m = new MarkerWithLabel(mapView, "", null);
        m.setPosition(cluster.getPosition());
        m.setInfoWindow(null);
        m.setAnchor(mAnchorU, mAnchorV);

        Bitmap finalIcon = Bitmap.createBitmap(mClusterIcon.getScaledWidth(mDensityDpi),
                mClusterIcon.getScaledHeight(mDensityDpi), mClusterIcon.getConfig());
        Canvas iconCanvas = new Canvas(finalIcon);
        iconCanvas.drawBitmap(mClusterIcon, 0, 0, null);
        String text = "" + cluster.getSize();
        int textHeight = (int) (mTextPaint.descent() + mTextPaint.ascent());
        iconCanvas.drawText(text,
                mTextAnchorU * finalIcon.getWidth(),
                mTextAnchorV * finalIcon.getHeight() - textHeight / 2,
                mTextPaint);
        m.setIcon(new BitmapDrawable(mapView.getContext().getResources(), finalIcon));

        return m;
    }

    @Override public void renderer(ArrayList<StaticCluster> clusters, Canvas canvas, MapView mapView) {
        for (StaticCluster cluster : clusters) {
            if (cluster.getSize() == 1) {
                
                cluster.setMarker(cluster.getItem(0));
            } else {
                
                MarkerWithLabel m = buildClusterMarker(cluster, mapView);
                cluster.setMarker(m);
            }
        }
    }

    private void convertRadiusToMeters(MapView mapView) {

        Rect mScreenRect = mapView.getIntrinsicScreenRect(null);

        int screenWidth = mScreenRect.right - mScreenRect.left;
        int screenHeight = mScreenRect.bottom - mScreenRect.top;

        BoundingBox bb = mapView.getBoundingBox();

        double diagonalInMeters = bb.getDiagonalLengthInMeters();
        double diagonalInPixels = Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight);
        double metersInPixel = diagonalInMeters / diagonalInPixels;

        mRadiusInMeters = mRadiusInPixels * metersInPixel;
    }

    public void setAnimation(boolean animate){
        mAnimated = animate;
    }

    public void zoomOnCluster(MapView mapView, StaticCluster cluster){
        BoundingBox bb = cluster.getBoundingBox();
        if (bb.getLatNorth()!=bb.getLatSouth() || bb.getLonEast()!=bb.getLonWest()) {
            bb = bb.increaseByScale(2.3f);
            mapView.zoomToBoundingBox(bb, true);
        } else 
            mapView.setExpectedCenter(bb.getCenterWithDateLine());
    }

    @Override public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView){
        for (final StaticCluster cluster : reversedClusters()) {
            if (cluster.getMarker().onSingleTapConfirmed(event, mapView)) {
                if (mAnimated && cluster.getSize() > 1)
                    zoomOnCluster(mapView, cluster);
                return true;
            }
        }
        return false;
    }

}
