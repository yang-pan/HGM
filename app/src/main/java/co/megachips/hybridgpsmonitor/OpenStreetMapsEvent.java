package co.megachips.hybridgpsmonitor;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMap;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.Polygon;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

/**
 * @brief
 */
@SuppressLint("DefaultLocale") public class OpenStreetMapsEvent{

	private int geradius  = 100;
	private Context context;

	private static final OpenStreetMapsEvent instance = new OpenStreetMapsEvent();
	private static final String TAG = null;
	private static Polygon circle, circle_2;

	private ArrayList<OverlayItem> Marker = new ArrayList<OverlayItem>();

	ItemizedIconOverlay<OverlayItem> marker_currentLocationOverlay;
	DefaultResourceProxyImpl resourceProxy = null;

	IMapController m_mapController;
	MapView OpenStreetMap;
	IMap imap;

	//private static Context mcontext;
	public OpenStreetMapsEvent(){
		this.context = null;
	}

	public OpenStreetMapsEvent(Context con){
		this.context = con;
	}

	public void setContext(Context con){
		this.context = con;
	}

	MapEventListener listener = null;
	public IMap enableRouteEvent(Context context, final FragmentActivity activity) {
		if(imap!=null){
			//Set marker on Android location
			imap.setMyLocationEnabled(false);
	    }
		return imap;
	}


	public String getAddressByLocation(Double latitude, Double  longitude) {
		String returnAddress = "";
		try {
			Geocoder gc = new Geocoder(context, Locale.TRADITIONAL_CHINESE);
			List<android.location.Address> lstAddress = gc.getFromLocation(latitude, longitude, 1);
            if (!Geocoder.isPresent()){ //Since: API Level 9
              returnAddress = "Sorry! Geocoder service not Present.";
            }
		    returnAddress = lstAddress.get(0).getAddressLine(0);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return returnAddress;
	}

	public interface MapEventListener extends EventListener {
		public void deleteRoute();
	}

   public void setListener(MapEventListener  listener){
       this.listener = listener;
   }

   public void removeListener(){
       this.listener = null;
   }
	public static OpenStreetMapsEvent getInstance() {
		return instance;
	}

	public void setMap(MapView OpenStreetMap) {
		this.OpenStreetMap = OpenStreetMap;
	}

    public int getRadius() {
		return geradius;
	}

	public void setRadius(int radius) {
		this.geradius = radius;
	}

	public void resetOffset() {
		OpenStreetMap.clearFocus();
	}
	public void MapPosition_init() {
		if (OpenStreetMap != null) {
	 		m_mapController = OpenStreetMap.getController();
			resourceProxy = new DefaultResourceProxyImpl(context);
			GeoPoint center_gpt = new GeoPoint(34.74091,135.48231);
			m_mapController.setZoom(16);
			m_mapController.setCenter(center_gpt);
		}
		else
		{
			Log.d(TAG,"Map pointer is null.");
		}
	}

	/**
	 * Draw point
	 */
	public void draw_point(float lat, float lon, int point_image_id) {
		GeoPoint geo_locaton = new GeoPoint(lat, lon);
		OverlayItem myLocationOverlayItem = new OverlayItem("", "", geo_locaton);
		Drawable myCurrentLocationMarker = OpenStreetMap.getResources().getDrawable(point_image_id);
	    myLocationOverlayItem.setMarker(myCurrentLocationMarker);
		myLocationOverlayItem.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
		Marker.add(myLocationOverlayItem);
		OpenStreetMap.getOverlays().remove(marker_currentLocationOverlay);
		marker_currentLocationOverlay = new ItemizedIconOverlay<OverlayItem>(Marker,
	                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
	                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
	                        return true;
	                    }
	                    public boolean onItemLongPress(final int index, final OverlayItem item) {
	                        return true;
	                    }
	                }, resourceProxy);
	    OpenStreetMap.getOverlays().add(marker_currentLocationOverlay);
		OpenStreetMap.invalidate();
	}

	/**
	 * Draw circle
	 */
	public void draw_circle(float lat,float lon, float radius, int color) {
		//Delete old circle
		delete_circle();
		GeoPoint geo_locaton = new GeoPoint(lat, lon);
		circle = new Polygon(context);
		circle.setPoints(Polygon.pointsAsCircle(geo_locaton, radius));
		circle.setFillColor(color);
		circle.setStrokeColor(color);
		circle.setStrokeWidth(2);
		OpenStreetMap.getOverlays().add(circle);
		OpenStreetMap.invalidate();
	}

	/**
	 * Delete circle
	 */
	public void delete_circle() {
		//Delate small circle
		 if (circle != null) {
			OpenStreetMap.getOverlays().remove(circle);
			circle = null;
		}
	}

	/**
	 * Clear
	 */
	public void ClearLocationArray() {
		//Delete marker
		OpenStreetMap.getOverlays().clear();
		Marker.clear();
		OpenStreetMap.invalidate();
	}

	public void MapPosition_center(float lat, float lon) {
		if (OpenStreetMap != null) {;
			GeoPoint center_gpt = new GeoPoint(lat,lon);
			m_mapController.setCenter(center_gpt);
		}
		else
		{
			Log.d(TAG,"Map pointer is null.");
		}
	}

	/**
	 * Draw line
	 */
	public void draw_line(double pre_lat, double pre_lon, double lat, double lon, int color) {
		Polygon line = new Polygon(context);
		ArrayList<GeoPoint> pointsList = new ArrayList<GeoPoint>();
		pointsList.add(new GeoPoint(pre_lat, pre_lon));
		pointsList.add(new GeoPoint(lat, lon));
		line.setPoints(pointsList);
		line.setStrokeColor(color);
		line.setStrokeWidth(5);
		OpenStreetMap.getOverlays().add(line);
		OpenStreetMap.invalidate();
	}
}