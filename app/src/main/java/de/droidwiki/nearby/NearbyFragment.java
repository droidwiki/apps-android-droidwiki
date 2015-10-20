package de.droidwiki.nearby;

import de.droidwiki.page.PageActivityLongPressHandler;
import de.droidwiki.page.PageLongPressHandler;
import de.droidwiki.page.PageTitle;
import de.droidwiki.R;
import de.droidwiki.Site;
import de.droidwiki.Utils;
import de.droidwiki.WikipediaApp;
import de.droidwiki.history.HistoryEntry;
import de.droidwiki.page.PageActivity;
import de.droidwiki.util.FeedbackUtil;
import de.droidwiki.util.log.L;

import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.RotateEvent;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.squareup.picasso.Picasso;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Displays a list of nearby pages.
 */
public class NearbyFragment extends Fragment implements SensorEventListener {
    private static final String PREF_KEY_UNITS = "nearbyUnits";
    private static final String NEARBY_LAST_RESULT = "lastRes";
    private static final String NEARBY_CURRENT_LOCATION = "currentLoc";
    private static final int ONE_THOUSAND = 1000;
    private static final double METER_TO_FEET = 3.280839895;
    private static final int ONE_MILE = 5280;

    private final List<Marker> mMarkerList = new ArrayList<>();
    private View nearbyListContainer;
    private ListView nearbyList;
    private MapView mapView;
    private NearbyAdapter adapter;
    private Icon mMarkerIconPassive;
    private Icon mMarkerIconActive;

    private WikipediaApp app;
    private Site site;
    private NearbyResult lastResult;
    @Nullable private Location currentLocation;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    //this holds the actual data from the accelerometer and magnetometer, and automatically
    //maintains a moving average (low-pass filter) to reduce jitter.
    private MovingAverageArray accelData;
    private MovingAverageArray magneticData;

    //The size with which we'll initialize our low-pass filters. This size seems like
    //a good balance between effectively removing jitter, and good response speed.
    //(Mimics a physical compass needle)
    private static final int MOVING_AVERAGE_SIZE = 8;

    //geomagnetic field data, to be updated whenever we update location.
    //(will provide us with declination from true north)
    private GeomagneticField geomagneticField;

    //we'll maintain a list of CompassViews that are currently being displayed, and update them
    //whenever we receive updates from sensors.
    private List<NearbyCompassView> compassViews;

    //whether to display distances in imperial units (feet/miles) instead of metric
    private boolean showImperial = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        site = app.getPrimarySite();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        rootView.setPadding(0, Utils.getContentTopOffsetPx(getActivity()), 0, 0);

        nearbyListContainer = rootView.findViewById(R.id.nearby_list_container);
        nearbyListContainer.setVisibility(View.GONE);

        nearbyList = (ListView) rootView.findViewById(R.id.nearby_list);
        mapView = (MapView) rootView.findViewById(R.id.mapview);
        rootView.findViewById(R.id.user_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // don't change zoom level: https://github.com/mapbox/mapbox-android-sdk/issues/453
                mapView.setUserLocationRequiredZoom(mapView.getZoomLevel());

                mapView.goToUserLocation(true);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new NearbyAdapter(getActivity(), new ArrayList<NearbyPage>());
        nearbyList.setAdapter(adapter);

        nearbyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NearbyPage nearbyPage = adapter.getItem(position);
                PageTitle title = new PageTitle(nearbyPage.getTitle(), site, nearbyPage.getThumblUrl());
                ((PageActivity) getActivity()).showLinkPreview(title, HistoryEntry.SOURCE_NEARBY);
            }
        });

        PageLongPressHandler.ListViewContextMenuListener contextMenuListener = new LongPressHandler((PageActivity) getActivity());
        new PageLongPressHandler(getActivity(), nearbyList, HistoryEntry.SOURCE_NEARBY,
                contextMenuListener);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        compassViews = new ArrayList<>();
        mMarkerIconPassive = makeMarkerIcon(false);
        mMarkerIconActive = makeMarkerIcon(true);

        if (!adapter.isEmpty()) {
            setupGeomagneticField();
            showNearbyPages(lastResult);
        } else if (savedInstanceState != null) {
            currentLocation = savedInstanceState.getParcelable(NEARBY_CURRENT_LOCATION);
            if (currentLocation != null) {
                lastResult = savedInstanceState.getParcelable(NEARBY_LAST_RESULT);
                setupGeomagneticField();
                showNearbyPages(lastResult);
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //do we already have a preference for metric/imperial units?
        if (prefs.contains(PREF_KEY_UNITS)) {
            setImperialUnits(prefs.getBoolean(PREF_KEY_UNITS, false));
        } else {
            //if our locale is set to US, then use imperial units by default.
            try {
                if (Locale.getDefault().getISO3Country().equalsIgnoreCase(Locale.US.getISO3Country())) {
                    setImperialUnits(true);
                }
            } catch (MissingResourceException e) {
                // getISO3Country can throw MissingResourceException: No 3-letter country code for locale: zz_ZZ
                // Just ignore it.
            }
        }

        setRefreshingState(true);
        initializeMap();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastResult != null) {
            outState.putParcelable(NEARBY_CURRENT_LOCATION, currentLocation);
            outState.putParcelable(NEARBY_LAST_RESULT, lastResult);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.setUserLocationEnabled(true);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.setUserLocationEnabled(false);
        mSensorManager.unregisterListener(this);
        compassViews.clear();
    }

    private void initializeMap() {
        WebSourceTileLayer tileSource = new WebSourceTileLayer(
                "openstreetmap",
                getString(R.string.map_tile_source_url),
                true
        );

        mapView.setBubbleEnabled(false);
        mapView.setDiskCacheEnabled(true);
        mapView.setTileSource(tileSource);
        mapView.setZoom(getResources().getInteger(R.integer.map_default_zoom));
        mapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        mapView.getUserLocationOverlay().runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                if (!isResumed()) {
                    return;
                }
                currentLocation = mapView.getUserLocationOverlay().getLastFix();
                makeUseOfNewLocation(currentLocation);
                fetchNearbyPages();
            }
        });

        mapView.setMapViewListener(new DefaultMapViewListener() {
            @Override
            public void onTapMarker(MapView mapView, Marker marker) {
                highlightMarker(marker);
                int index = adapter.getPosition((NearbyPage) marker.getRelatedObject());
                if (index == -1) {
                    return;
                }
                nearbyList.setSelection(index);
            }
        });

        mapView.addListener(new MapListener() {
            @Override
            public void onScroll(ScrollEvent scrollEvent) {
                fetchNearbyPages();
            }

            @Override
            public void onZoom(ZoomEvent zoomEvent) {
                fetchNearbyPages();
            }

            @Override
            public void onRotate(RotateEvent rotateEvent) {
            }
        });
    }

    private Icon makeMarkerIcon(boolean isActive) {
        int iconSize = (int) getResources().getDimension(R.dimen.map_marker_icon_size);
        Bitmap bmp = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Drawable d;
        if (isActive) {
            paint.setColor(getResources().getColor(R.color.blue_liberal));
            int circleSize = bmp.getWidth() / 2;
            canvas.drawCircle(circleSize, circleSize, circleSize, paint);

            Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_place_dark);
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
                    iconSize, iconSize, true));
            d = DrawableCompat.wrap(d).mutate();
            DrawableCompat.setTint(d, getResources().getColor(R.color.blue_liberal));

        } else {
            paint.setColor(getResources().getColor(R.color.green_progressive));
            int circleSize = bmp.getWidth() / 2;
            canvas.drawCircle(circleSize, circleSize, circleSize / 2, paint);
            d = new BitmapDrawable(getResources(), bmp);
        }
        return new Icon(d);
    }

    private void highlightMarker(Marker marker) {
        for (Marker m : mMarkerList) {
            if (m.equals(marker)) {
                m.setIcon(mMarkerIconActive);
                m.setHotspot(Marker.HotspotPlace.BOTTOM_CENTER);
            } else {
                m.setIcon(mMarkerIconPassive);
                m.setHotspot(Marker.HotspotPlace.BOTTOM_CENTER);
            }
        }
    }

    private void showDialogForSettings() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(de.droidwiki.R.string.nearby_dialog_goto_settings);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                try {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    // it's highly unusual for a device not to have a Settings activity,
                    // but nevertheless, let's not crash in case it happens.
                    e.printStackTrace();
                }
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alert.setCancelable(false);
        AlertDialog ad = alert.create();
        ad.show();
    }

    private void makeUseOfNewLocation(Location location) {
        if (!isBetterLocation(location, currentLocation)) {
            return;
        }
        currentLocation = location;
        setupGeomagneticField();
        updateDistances();
    }

    private void fetchNearbyPages() {
        final int fetchTaskDelayMillis = 500;
        mapView.removeCallbacks(fetchTaskRunnable);
        mapView.postDelayed(fetchTaskRunnable, fetchTaskDelayMillis);
    }

    private Runnable fetchTaskRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isResumed()) {
                return;
            }
            LatLng latLng = mapView.getCenter();
            setRefreshingState(true);
            new NearbyFetchTask(getActivity(), site, latLng.getLatitude(), latLng.getLongitude(), getMapRadius()) {
                @Override
                public void onFinish(NearbyResult result) {
                    if (!isResumed()) {
                        return;
                    }
                    lastResult = result;
                    showNearbyPages(result);
                    setRefreshingState(false);
                }

                @Override
                public void onCatch(Throwable caught) {
                    if (!isResumed()) {
                        return;
                    }
                    L.e(caught);
                    FeedbackUtil.showError(getActivity(), caught);
                    setRefreshingState(false);
                }
            }.execute();
        }
    };

    private double getMapRadius() {
        LatLng leftTop = new LatLng(mapView.getBoundingBox().getLatNorth(), mapView.getBoundingBox().getLonWest());
        LatLng rightTop = new LatLng(mapView.getBoundingBox().getLatNorth(), mapView.getBoundingBox().getLonEast());
        LatLng leftBottom = new LatLng(mapView.getBoundingBox().getLatSouth(), mapView.getBoundingBox().getLonWest());
        double width = leftTop.distanceTo(rightTop);
        double height = leftTop.distanceTo(leftBottom);
        return Math.min(width, height) / 2;
    }

    /** Updates geomagnetic field data, to give us our precise declination from true north. */
    private void setupGeomagneticField() {
        geomagneticField = new GeomagneticField((float)currentLocation.getLatitude(), (float)currentLocation.getLongitude(), 0, (new Date()).getTime());
    }

    /** Determines whether one Location reading is better than the current Location fix.
     * lifted from http://developer.android.com/guide/topics/location/strategies.html
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        final int twoMinutes = 1000 * 60 * 2;
        final int accuracyThreshold = 200;
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > twoMinutes;
        boolean isSignificantlyOlder = timeDelta < -twoMinutes;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > accuracyThreshold;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                                                    currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private void showNearbyPages(NearbyResult result) {
        getActivity().invalidateOptionsMenu();
        if (currentLocation != null) {
            sortByDistance(result.getList());
        }
        adapter.clear();
        addResultsToAdapter(result.getList());
        compassViews.clear();
        mMarkerList.clear();
        mapView.clear();
        nearbyListContainer.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
        for (int i = 0; i < adapter.getCount(); i++) {
            NearbyPage item = adapter.getItem(i);
            Location location = item.getLocation();
            Marker marker = new Marker(mapView, item.getTitle(), item.getDescription(),
                    new LatLng(location.getLatitude(), location.getLongitude()));
            marker.setIcon(mMarkerIconPassive);
            marker.setRelatedObject(item);
            marker.setHotspot(Marker.HotspotPlace.BOTTOM_CENTER);
            mMarkerList.add(marker);
        }
        mapView.addMarkers(mMarkerList);
    }

    private void addResultsToAdapter(List<NearbyPage> result) {
        adapter.addAll(result);
    }

    private void setRefreshingState(boolean isRefreshing) {
        ((PageActivity)getActivity()).updateProgressBar(isRefreshing, true, 0);
    }

    private void sortByDistance(List<NearbyPage> nearbyPages) {
        calcDistances(nearbyPages);

        Collections.sort(nearbyPages, new Comparator<NearbyPage>() {
            public int compare(NearbyPage a, NearbyPage b) {
                return a.getDistance() - b.getDistance();
            }
        });
    }

    /**
     * Calculates the distances from the origin to the given pages.
     * This method should be called before sorting.
     */
    private void calcDistances(List<NearbyPage> pages) {
        for (NearbyPage page : pages) {
            page.setDistance(getDistance(page.getLocation()));
        }
    }

    private int getDistance(Location otherLocation) {
        if (otherLocation == null) {
            return Integer.MAX_VALUE;
        } else {
            return (int) currentLocation.distanceTo(otherLocation);
        }
    }

    private String getDistanceLabel(Location otherLocation) {
        final int meters = getDistance(otherLocation);
        if (showImperial) {
            final double feet = meters * METER_TO_FEET;
            if (feet < ONE_THOUSAND) {
                return getString(de.droidwiki.R.string.nearby_distance_in_feet, (int)feet);
            } else {
                return getString(de.droidwiki.R.string.nearby_distance_in_miles, feet / ONE_MILE);
            }
        } else {
            if (meters < ONE_THOUSAND) {
                return getString(de.droidwiki.R.string.nearby_distance_in_meters, meters);
            } else {
                return getString(R.string.nearby_distance_in_kilometers, meters / (double)ONE_THOUSAND);
            }
        }
    }

    private void updateDistances() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        inflater.inflate(de.droidwiki.R.menu.menu_nearby, menu);
        menu.findItem(de.droidwiki.R.id.menu_metric_imperial).setTitle(showImperial
                ? getString(de.droidwiki.R.string.nearby_set_metric)
                : getString(de.droidwiki.R.string.nearby_set_imperial));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        menu.findItem(de.droidwiki.R.id.menu_metric_imperial).setTitle(showImperial
                ? getString(de.droidwiki.R.string.nearby_set_metric)
                : getString(de.droidwiki.R.string.nearby_set_imperial));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_metric_imperial:
                setImperialUnits(!showImperial);
                adapter.notifyDataSetInvalidated();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setImperialUnits(boolean imperial) {
        showImperial = imperial;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(PREF_KEY_UNITS, showImperial).apply();
        getActivity().supportInvalidateOptionsMenu();
    }


    private View.OnClickListener markerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Marker marker = findPageMarker((NearbyPage) v.getTag());
            if (marker != null) {
                highlightMarker(marker);
            }
        }
    };

    @Nullable
    private Marker findPageMarker(NearbyPage nearbyPage) {
        Marker result = null;
        for (Marker marker : mMarkerList) {
            if (marker.getRelatedObject().equals(nearbyPage)) {
                result = marker;
                break;
            }
        }
        return result;
    }

    private View.OnLongClickListener markerLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int[] pos = new int[2];
            v.getLocationInWindow(pos);
            // display a toast that shows a tooltip based on the button's content description,
            // like the standard ActionBar does.
            Toast t = Toast.makeText(getActivity(), v.getContentDescription(), Toast.LENGTH_SHORT);
            t.setGravity(Gravity.TOP | Gravity.END, 0, pos[1]);
            t.show();
            return true;
        }
    };

    private class NearbyAdapter extends ArrayAdapter<NearbyPage> {
        private static final int LAYOUT_ID = de.droidwiki.R.layout.item_nearby_entry;

        public NearbyAdapter(Context context, ArrayList<NearbyPage> pages) {
            super(context, LAYOUT_ID, pages);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NearbyPage nearbyPage = getItem(position);
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(LAYOUT_ID, parent, false);
                viewHolder.thumbnail = (NearbyCompassView) convertView.findViewById(de.droidwiki.R.id.nearby_thumbnail);
                viewHolder.title = (TextView) convertView.findViewById(de.droidwiki.R.id.nearby_title);
                viewHolder.description = (TextView) convertView.findViewById(de.droidwiki.R.id.nearby_description);
                viewHolder.distance = (TextView) convertView.findViewById(de.droidwiki.R.id.nearby_distance);
                viewHolder.markerButton = convertView.findViewById(de.droidwiki.R.id.nearby_marker);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.title.setText(nearbyPage.getTitle());
            if (TextUtils.isEmpty(nearbyPage.getDescription())) {
                viewHolder.description.setVisibility(View.GONE);
            } else {
                viewHolder.description.setText(nearbyPage.getDescription());
                viewHolder.description.setVisibility(View.VISIBLE);
            }

            viewHolder.markerButton.setTag(nearbyPage);
            viewHolder.markerButton.setOnClickListener(markerClickListener);
            viewHolder.markerButton.setOnLongClickListener(markerLongClickListener);

            viewHolder.thumbnail.setMaskColor(getResources().getColor(Utils.getThemedAttributeId(getActivity(), R.attr.page_background_color)));
            if (currentLocation == null) {
                viewHolder.distance.setVisibility(View.INVISIBLE);
                viewHolder.thumbnail.setEnabled(false);
            } else {
                // set the calculated angle as the base angle for our compass view
                viewHolder.thumbnail.setAngle((float) calculateAngle(nearbyPage.getLocation()));
                viewHolder.thumbnail.setTickColor(getResources().getColor(R.color.button_light));
                viewHolder.thumbnail.setEnabled(true);
                if (!compassViews.contains(viewHolder.thumbnail)) {
                    compassViews.add(viewHolder.thumbnail);
                }
                viewHolder.distance.setText(getDistanceLabel(nearbyPage.getLocation()));
                viewHolder.distance.setVisibility(View.VISIBLE);
            }

            if (app.isImageDownloadEnabled()) {
                Picasso.with(getActivity())
                       .load(nearbyPage.getThumblUrl())
                       .placeholder(de.droidwiki.R.drawable.ic_pageimage_placeholder)
                       .error(de.droidwiki.R.drawable.ic_pageimage_placeholder)
                       .into(viewHolder.thumbnail);
            } else {
                Picasso.with(getActivity())
                       .load(de.droidwiki.R.drawable.ic_pageimage_placeholder)
                       .into(viewHolder.thumbnail);
            }
            return convertView;
        }

        private double calculateAngle(Location otherLocation) {
            // simplified angle between two vectors...
            // vector pointing towards north from our location = [0, 1]
            // vector pointing towards destination from our location = [a1, a2]
            double a1 = otherLocation.getLongitude() - currentLocation.getLongitude();
            double a2 = otherLocation.getLatitude() - currentLocation.getLatitude();
            // cos θ = (v1*a1 + v2*a2) / (√(v1²+v2²) * √(a1²+a2²))
            double angle = Math.toDegrees(Math.acos(a2 / Math.sqrt(a1 * a1 + a2 * a2)));
            // since the acos function only goes between 0 to 180 degrees, we'll manually
            // negate the angle if the destination's longitude is on the opposite side.
            if (a1 < 0f) {
                angle = -angle;
            }
            return angle;
        }


        private class ViewHolder {
            private NearbyCompassView thumbnail;
            private TextView title;
            private TextView description;
            private TextView distance;
            private View markerButton;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isAdded()) {
            return;
        }
        //acquire raw data from sensors...
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (accelData == null) {
                accelData = new MovingAverageArray(event.values.length, MOVING_AVERAGE_SIZE);
            }
            accelData.addData(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (magneticData == null) {
                magneticData = new MovingAverageArray(event.values.length, MOVING_AVERAGE_SIZE);
            }
            magneticData.addData(event.values);
        }
        if (accelData == null || magneticData == null) {
            return;
        }

        final int matrixSize = 9;
        final int orientationSize = 3;
        final int quarterTurn = 90;
        float[] mR = new float[matrixSize];
        //get the device's rotation matrix with respect to world coordinates, based on the sensor data
        if (!SensorManager.getRotationMatrix(mR, null, accelData.getData(), magneticData.getData())) {
            Log.e("NearbyActivity", "getRotationMatrix failed.");
            return;
        }

        //get device's orientation with respect to world coordinates, based on the
        //rotation matrix acquired above.
        float[] orientation = new float[orientationSize];
        SensorManager.getOrientation(mR, orientation);
        // orientation[0] = azimuth
        // orientation[1] = pitch
        // orientation[2] = roll
        float azimuth = (float) Math.toDegrees(orientation[0]);

        //adjust for declination from magnetic north...
        float declination = 0f;
        if (geomagneticField != null) {
            declination = geomagneticField.getDeclination();
        }
        azimuth += declination;

        //adjust for device screen rotation
        int rotation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                azimuth += quarterTurn;
                break;
            case Surface.ROTATION_180:
                azimuth += quarterTurn * 2;
                break;
            case Surface.ROTATION_270:
                azimuth -= quarterTurn;
                break;
            default:
                break;
        }

        //update views!
        for (NearbyCompassView view : compassViews) {
            view.setAzimuth(-azimuth);
        }
    }

    private class LongPressHandler extends PageActivityLongPressHandler
            implements PageLongPressHandler.ListViewContextMenuListener {
        public LongPressHandler(@NonNull PageActivity activity) {
            super(activity);
        }

        @Override
        public PageTitle getTitleForListPosition(int position) {
            NearbyPage page = adapter.getItem(position);
            return new PageTitle(page.getTitle(), site, page.getThumblUrl());
        }
    }
}
