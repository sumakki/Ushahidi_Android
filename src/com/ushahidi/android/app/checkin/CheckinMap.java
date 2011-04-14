
package com.ushahidi.android.app.checkin;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.google.android.maps.*;
import com.ushahidi.android.app.About;
import com.ushahidi.android.app.IncidentsTab;
import com.ushahidi.android.app.R;
import com.ushahidi.android.app.Settings;
import com.ushahidi.android.app.Ushahidi;
import com.ushahidi.android.app.UshahidiApplication;
import com.ushahidi.android.app.UshahidiPref;
import com.ushahidi.android.app.Util;
import com.ushahidi.android.app.data.UshahidiDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: Ahmed Date: 2/17/11 Time: 2:21 PM To change
 * this template use File | Settings | File Templates.
 */
public class CheckinMap extends MapActivity {

    private MapView mapView;

    private List<Checkin> checkinsList = null;

    private List<Checkin> checkins = null;

    private Cursor cursor;

    protected double latitude = 0;

    protected double longitude = 0;

    protected String locationName = "";

    protected String name = "";

    private Bundle extras = new Bundle();
    
    private Bundle checkinsBundle = new Bundle();
    
    private static final int HOME = Menu.FIRST + 1;

    private static final int ADD_CHECKIN = Menu.FIRST + 2;

    private static final int CHECKIN_MAP = Menu.FIRST + 3;

    private static final int CHECKIN_REFRESH = Menu.FIRST + 4;

    private static final int SETTINGS = Menu.FIRST + 5;

    private static final int ABOUT = Menu.FIRST + 6;

    private static final int GOTOHOME = 0;

    private static final int POST_CHECKIN = 1;

    private static final int CHECKINS_MAP = 2;

    private static final int REQUEST_CODE_SETTINGS = 4;

    private static final int REQUEST_CODE_ABOUT = 5;

    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        UshahidiPref.loadSettings(CheckinMap.this);
        setContentView(R.layout.checkin_map);

        mapView = (MapView)findViewById(R.id.checkin_mapview);
        mapView.setBuiltInZoomControls(true);
        name = UshahidiPref.firstname + " " + UshahidiPref.lastname;
        checkins = new ArrayList<Checkin>();
        setDeviceLocation();

        // checkinsList = showCheckins();
        CheckinsTask checkinTask = new CheckinsTask();
        checkinTask.appContext = this;
        checkinTask.execute();

    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    /**
     * add marker to the map
     */
    private void populateMap() {
        Drawable marker = getResources().getDrawable(R.drawable.marker);
        marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        mapView.getOverlays().add(new CheckinsOverlay(marker, mapView));
    }

    // put this stuff in a seperate thread
    final Runnable mMarkersOnMap = new Runnable() {
        public void run() {
            populateMap();
        }
    };
    
 // menu stuff
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        populateMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        populateMenu(menu);

        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // applyMenuChoice(item);

        return (applyMenuChoice(item) || super.onOptionsItemSelected(item));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        return (applyMenuChoice(item) || super.onContextItemSelected(item));
    }

    private void populateMenu(Menu menu) {
        MenuItem i;
        i = menu.add(Menu.NONE, HOME, Menu.NONE, R.string.menu_home);
        i.setIcon(R.drawable.ushahidi_home);

        i = menu.add(Menu.NONE, ADD_CHECKIN, Menu.NONE, R.string.checkin_btn);
        i.setIcon(R.drawable.ushahidi_add);

        i = menu.add(Menu.NONE, CHECKIN_MAP, Menu.NONE, R.string.checkin_list);
        i.setIcon(R.drawable.ushahidi_list);

        i = menu.add(Menu.NONE, CHECKIN_REFRESH, Menu.NONE, R.string.menu_sync);
        i.setIcon(R.drawable.ushahidi_refresh);

        i = menu.add(Menu.NONE, SETTINGS, Menu.NONE, R.string.menu_settings);
        i.setIcon(R.drawable.ushahidi_settings);

        i = menu.add(Menu.NONE, ABOUT, Menu.NONE, R.string.menu_about);
        i.setIcon(R.drawable.ushahidi_about);

    }
    
    private void refreshForNewCheckins() {
        CheckinsTask checkinsTask = new CheckinsTask();
        checkinsTask.appContext = this;
        checkinsTask.execute();
    }
    
    private boolean applyMenuChoice(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case HOME:
                intent = new Intent(CheckinMap.this, Ushahidi.class);
                startActivityForResult(intent, GOTOHOME);
                return true;
            case CHECKIN_REFRESH:
                refreshForNewCheckins();
                return (true);

            case CHECKIN_MAP:
                checkinsBundle.putInt("tab_index", 0);
                intent = new Intent(CheckinMap.this, IncidentsTab.class);
                intent.putExtra("tab", checkinsBundle);
                startActivityForResult(intent, CHECKINS_MAP);
                return (true);

            case ADD_CHECKIN:
                intent = new Intent(CheckinMap.this, CheckinActivity.class);
                startActivityForResult(intent, POST_CHECKIN);
                return (true);

            case ABOUT:
                intent = new Intent(CheckinMap.this, About.class);
                startActivityForResult(intent, REQUEST_CODE_ABOUT);
                setResult(RESULT_OK);
                return true;

            case SETTINGS:
                intent = new Intent(CheckinMap.this, Settings.class);

                // Make it a subactivity so we know when it returns
                startActivityForResult(intent, REQUEST_CODE_SETTINGS);
                return (true);

        }
        return (false);
    }

    /**
     * Add marker for current location of the device
     */
    private void addMarker() {
        Drawable marker = getResources().getDrawable(R.drawable.green_dot);
        marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        mapView.getController().setZoom(12);
        mapView.getOverlays().add(new DeviceLocationOverlay(marker, mapView));
    }

    final Runnable mDeviceLocationMarkerOnMap = new Runnable() {
        public void run() {
            addMarker();
        }
    };

    private void centerLocation(GeoPoint centerGeoPoint) {

        mapView.getController().setCenter(centerGeoPoint);
        addMarker();

    }

    // Fetches the current location of the device.
    private void setDeviceLocation() {

        DeviceLocationListener listener = new DeviceLocationListener();
        LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        long updateTimeMsec = 1000L;

        // get low accuracy provider
        LocationProvider low = manager.getProvider(manager.getBestProvider(
                Util.createCoarseCriteria(), true));

        // get high accuracy provider
        LocationProvider high = manager.getProvider(manager.getBestProvider(
                Util.createFineCriteria(), true));

        manager.requestLocationUpdates(low.getName(), updateTimeMsec, 500.0f, listener);

        manager.requestLocationUpdates(high.getName(), updateTimeMsec, 500.0f, listener);

    }

    public void onDestroy() {
        super.onDestroy();
        ((LocationManager)getSystemService(Context.LOCATION_SERVICE))
                .removeUpdates(new DeviceLocationListener());
    }

    public void onResume() {
        super.onResume();
    }

    // get the current location of the device/user
    public class DeviceLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {

            if (location != null) {

                ((LocationManager)getSystemService(Context.LOCATION_SERVICE)).removeUpdates(this);

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                centerLocation(getPoint(latitude, longitude));

            }
        }

        public void onProviderDisabled(String provider) {
            Util.showToast(CheckinMap.this, R.string.location_not_found);
        }

        public void onProviderEnabled(String provider) {

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }

    public GeoPoint getPoint(double lat, double lon) {
        return (new GeoPoint((int)(lat * 1000000.0), (int)(lon * 1000000.0)));
    }

    private class CheckinsOverlay extends CheckinItemizedOverlay<OverlayItem> {
        private ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        public CheckinsOverlay(Drawable marker, MapView mapView) {
            super(boundCenterBottom(marker), mapView, CheckinMap.this, checkins, extras);
            mapView.getContext();

            for (Checkin checkin : checkinsList) {

                items.add(new OverlayItem(getPoint(Double.valueOf(checkin.getLat()),
                        Double.valueOf(checkin.getLon())),checkin.getName(), Util.limitString(checkin.getMsg(), 30)
                        + "\n" + checkin.getDate()));

            }

            populate();
        }

        @Override
        protected OverlayItem createItem(int i) {
            return items.get(i);
        }

        @Override
        protected boolean onBalloonTap(int i) {
            return true;
        }

        @Override
        public int size() {
            return (items.size());
        }
    }

    private class DeviceLocationOverlay extends CheckinItemizedOverlay<OverlayItem> {
        private ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        private String user = "";

        public DeviceLocationOverlay(Drawable marker, MapView mapView) {
            super(boundCenterBottom(marker), mapView, CheckinMap.this, checkins, extras);
            mapView.getContext();
            if( TextUtils.isEmpty(name.trim())) {
                user = getString(R.string.no_name);
            } else {
                user = name;
            }
            
            items.add(new OverlayItem(getPoint(latitude, longitude), user,
                    getString(R.string.curr_location)));
            populate();

        }

        @Override
        protected OverlayItem createItem(int i) {
            return items.get(i);
        }

        @Override
        protected boolean onBalloonTap(int i) {
            return true;
        }

        @Override
        public int size() {
            return (items.size());
        }
    }

    // get checkins from the db
    public List<Checkin> showCheckins() {

        cursor = UshahidiApplication.mDb.fetchAllCheckins();
        String name;
        String date;
        String mesg;
        String location;

        if (cursor.moveToFirst()) {

            int idIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CHECKIN_ID);
            int userIdIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CHECKIN_USER_ID);
            int dateIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CHECKIN_DATE);
            int locationIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CHECKIN_LOC_NAME);

            int mesgIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CHECKIN_MESG);

            int latitudeIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CHECKIN_LOC_LATITUDE);

            int longitudeIndex = cursor
                    .getColumnIndexOrThrow(UshahidiDatabase.CHECKIN_LOC_LONGITUDE);

            do {

                Checkin checkinsData = new Checkin();
                checkins.add(checkinsData);

                int id = Util.toInt(cursor.getString(idIndex));
                checkinsData.setId(String.valueOf(id));
                checkinsData.setLat(cursor.getString(latitudeIndex));
                checkinsData.setLon(cursor.getString(longitudeIndex));

                name = cursor.getString(userIdIndex);
                checkinsData.setName(name);

                mesg = cursor.getString(mesgIndex);
                checkinsData.setMsg(mesg);

                location = cursor.getString(locationIndex);
                checkinsData.setLoc(location);

                date = Util.formatDate("yyyy-MM-dd HH:mm:ss", cursor.getString(dateIndex),
                        "MMMM dd, yyyy 'at' hh:mm:ss a");

                checkinsData.setDate(date);
                checkinsData.setImage(String.valueOf(id));
                
            } while (cursor.moveToNext());
        }

        cursor.close();
        return checkins;

    }

    private class CheckinsTask extends AsyncTask<Void, Void, Integer> {

        protected Integer status;

        protected Context appContext;

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);

        }

        @Override
        protected Integer doInBackground(Void... params) {
            status = Util.processCheckins(appContext);
            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 2) {

                Util.showToast(appContext, R.string.internet_connection);
            } else if (result == 1) {

                Util.showToast(appContext, R.string.could_not_fetch_reports);
            }

            checkinsList = showCheckins();

            if (checkinsList.size() == 0) {
                Util.showToast(appContext, R.string.no_reports);
            } else {
                populateMap();
            }

            setProgressBarIndeterminateVisibility(false);
        }

    }

}
