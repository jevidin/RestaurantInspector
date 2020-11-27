package ca.cmpt276.project.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.cmpt276.project.R;
import ca.cmpt276.project.model.ClusterManagerRenderer;
import ca.cmpt276.project.model.ClusterMarker;
import ca.cmpt276.project.model.CsvInfo;
import ca.cmpt276.project.model.DBAdapter;
import ca.cmpt276.project.model.Inspection;
import ca.cmpt276.project.model.LastModified;
import ca.cmpt276.project.model.Restaurant;
import ca.cmpt276.project.model.RestaurantListManager;
import ca.cmpt276.project.model.SurreyDataDownloader;
import ca.cmpt276.project.model.types.HazardLevel;
import ca.cmpt276.project.model.types.InspectionType;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, UpdateFragment.UpdateDialogListener, LoadingDialogFragment.CancelDialogListener, SearchDialogFragment.SearchDialogListener, ClusterManager.OnClusterClickListener<ClusterMarker>, ClusterManager.OnClusterInfoWindowClickListener<ClusterMarker>, ClusterManager.OnClusterItemClickListener<ClusterMarker>, ClusterManager.OnClusterItemInfoWindowClickListener<ClusterMarker> {
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    //SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private RestaurantListManager restaurantManager;
    private LastModified lastModified;
    private List<CsvInfo> restaurantUpdate;
    private List<LatLng> restaurantlatlog;

    // custom markers
    private ClusterManager<ClusterMarker> mClusterManager;
    private ClusterManagerRenderer mClusterManagerRenderer;
    private static final String REST_DETAILS_INDEX = "restaurant details index";
    private final List<ClusterMarker> Markerlist = new ArrayList<>();

    //User Locations permission
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private static boolean read = false;
    private ListUpdateTask listUpdateTask = null;
    private LoadingDialogFragment loadingDialog;

    DBAdapter myDb;
    List<Restaurant> foundRestaurants;
    private static final String TAG = "MapsTag";
    //Location callBack
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult == null) {
                return;
            }
            for(Location location : locationResult.getLocations()){
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13f));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = findViewById(R.id.map_toolbar);
        setSupportActionBar(toolbar);
        myDb = new DBAdapter(this);
        myDb.open();
        restaurantManager = RestaurantListManager.getInstance();
        lastModified = LastModified.getInstance(this);
        if(!read){
            fillInitialRestaurantList();
            read = true;
            getUpdatedFiles();
        }

        if(lastModified.readInitialStart(this)){
            fillInitialDatabase();
            lastModified.writeInitialStart(this);
        }

        if (lastModified.getAppStart() && past20Hours()) {
            lastModified.setAppStart();
            new GetDataTask().execute();
        }
        getLocationPermission();

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationUpdates();
        }
    }

    private void LocationUpdates() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(locationSettingsResponse -> {
            //Settings of device are satisfied and we can start location updates
            startLocationUpdates();
        });
        locationSettingsResponseTask.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                ResolvableApiException apiException = (ResolvableApiException) e;
                try {
                    apiException.startResolutionForResult(MapsActivity.this, 1001);
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initialMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionsGranted = false;
                        return;
                    }
                }
                mLocationPermissionsGranted = true;
                //initialize our map
                initialMap();
                LocationUpdates();
            }
        }
    }

    private void initialMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_map,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if (item.getItemId() == R.id.action_list) {
            startActivity(new Intent(MapsActivity.this,RestaurantListActivity.class));
            finish();
            return true;
        }
        else if(item.getItemId() ==  R.id.action_search){
            FragmentManager manager = getSupportFragmentManager();
            SearchDialogFragment dialog = new SearchDialogFragment(); // open search
            dialog.show(manager, "SearchDialog");
            return true;
        }
        return false;
    }


    public DBAdapter getDBAdapter_restaurants(){
        return myDb;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myDb.close();

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupMap();
        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener(){
            @Override
            public boolean onMyLocationButtonClick()
            {
                //startLocationUpdates();
                return true;
            }
        });
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Location currentLocation = (Location) task.getResult();
                        
                        if(currentLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(13f));
                        }

                        startLocationUpdates();
                        extractDataFromIntent();
                    }else{
                        Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }catch (SecurityException e){
            Log.e("user location", "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    public void setupMap(){

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mClusterManager = new ClusterManager<>(this, mMap);
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this));
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);

        popLatlong();
        addMarkers(mMap);
    }
    private void popLatlong() {
        restaurantlatlog = new ArrayList<>();
        for (Restaurant current : restaurantManager.getList()){
            LatLng temp = new LatLng(current.getGpsLat(),current.getGpsLong());
            restaurantlatlog.add(temp);
        }
    }

    private void addMarkers(GoogleMap googleMap) {
        if(googleMap != null) {

            if (mClusterManager == null) {
                mClusterManager = new ClusterManager<ClusterMarker>(this.getApplicationContext(), googleMap);
            }
            if (mClusterManagerRenderer == null) {
                mClusterManagerRenderer = new ClusterManagerRenderer(this, googleMap, mClusterManager );
                mClusterManager.setRenderer(mClusterManagerRenderer);
            }
            int pos = 0 ;
            // set the severity icons
            int low = R.drawable.green_hazard;
            int med = R.drawable.orange_hazard;
            int high = R.drawable.red_hazard;
            for (LatLng current : restaurantlatlog) {
                try {
                    if (restaurantManager.getRestaurant(pos).getInspections().getInspections().size() > 0) {
                        String snippet = "";
                        int severity_icon = R.drawable.green_hazard;
                        Inspection latestInspection = Collections.max(restaurantManager.getRestaurant(pos).getInspections().getInspections());
                        if (latestInspection.getLevel() == HazardLevel.LOW) {
                            snippet = ""+ pos;
                            severity_icon = low;
                        } else if (latestInspection.getLevel() == HazardLevel.MODERATE) {
                            snippet = ""+ pos;
                            severity_icon = med;
                        } else if (latestInspection.getLevel() == HazardLevel.HIGH) {
                            snippet = ""+ pos;
                            severity_icon = high;
                        }
                        ClusterMarker newClusterMarker = new ClusterMarker(current,restaurantManager.getRestaurant(pos).getName(), snippet, severity_icon, restaurantManager.getRestaurant(pos));
                        mClusterManager.addItem(newClusterMarker);
                        Markerlist.add(newClusterMarker);
                    }
                } catch(NullPointerException e){
                    Log.e("CMarker", "addMapMarkers: NullPointerException: " + e.getMessage());
                }
                pos++;
            }
            mClusterManager.cluster();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        //mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public boolean onClusterClick(Cluster<ClusterMarker> cluster) {

        // Create the builder to collect all essential cluster items for the bounds.
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();

        // Animate camera to the bounds
        try {
           mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopLocationUpdates();
        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<ClusterMarker> cluster) {
    }

    @Override
    public boolean onClusterItemClick(ClusterMarker item) {
        stopLocationUpdates();
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(ClusterMarker item) {
            int position = restaurantlatlog.indexOf(item.getPosition());
            Intent intent = RestaurantDetailsActivity.makeLaunchIntent(MapsActivity.this, position);
            startActivity(intent);
    }

    // Get the CSV links and timestamps
    private class GetDataTask extends AsyncTask<Void,Void,List<CsvInfo>> {
        @Override
        protected List<CsvInfo> doInBackground(Void... voids) {
            return new SurreyDataDownloader().getDataLink(MapsActivity.this);
        }

        @Override
        protected void onPostExecute(List<CsvInfo> data) {
            restaurantUpdate = data;
            if (restaurantUpdate.get(0).getChanged() // check if restaurant list changed
                    || restaurantUpdate.get(1).getChanged()) { // if inspection list changed
                // Want update? Execute function
                FragmentManager manager = getSupportFragmentManager();
                UpdateFragment dialog = new UpdateFragment(); // ask if user wants to update
                dialog.show(manager, "MessageDialog");
            } else {
                lastModified.setLastCheck(MapsActivity.this, LocalDateTime.now());
            }
        }
    }

    @Override
    public void sendInput(boolean input) {
        if(input) {
            listUpdateTask = (ListUpdateTask) new ListUpdateTask().execute();
        }
    }

    @Override
    public void sendCancel(boolean input) {
        if (input) {
            listUpdateTask.cancel(true);
            Toast.makeText(this, "CANCELLED DOWNLOAD", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void sendSearchInput(String input, String hazard_filter, int num_critical_filter) {
        Toast.makeText(this, "name: " + input+" hazard filter: "+hazard_filter+" critical filter: "+num_critical_filter, Toast.LENGTH_LONG).show();
        if(input.length() > 0) {
            Cursor relevantRowsCursor = myDb.searchRestaurants(DBAdapter.KEY_NAME, input, DBAdapter.MatchString.CONTAINS);
            Log.d(TAG, "NEW SEARCH: " + input);
            if (relevantRowsCursor != null) {
                addRelevantMarkers(relevantRowsCursor);
            }
            printCursor(relevantRowsCursor);
        }
    }

    private void printCursor(Cursor cursor){
        if(cursor.moveToFirst()){
            do{
                String name = cursor.getString(DBAdapter.COL_NAME);
                Log.d(TAG, "printing cursor: " + name);
            }while(cursor.moveToNext());
        }
        else{
            Log.d(TAG, "null cursor");
        }
    }

    private void addRelevantMarkers(Cursor cursor){
        mClusterManager.clearItems();
        mClusterManager.cluster();
        MarkerOptions markerOptions = new MarkerOptions();
        int pos = 0;

        if(cursor.moveToFirst()){
            do{
//                markerOptions.position(temp);
//                markerOptions.title(name);
//                mMap.addMarker(markerOptions);
                String tracking = cursor.getString(DBAdapter.COL_TRACKING);
                String address = cursor.getString(DBAdapter.COL_ADDRESS);
                String city = cursor.getString(DBAdapter.COL_CITY);
                String name = cursor.getString(DBAdapter.COL_NAME);
                float latitude = cursor.getFloat(DBAdapter.COL_LATITUDE);
                float longitude = cursor.getFloat(DBAdapter.COL_LONGITUDE);
                LatLng temp = new LatLng(latitude, longitude);
                Restaurant newRestaurant = new Restaurant(tracking, name, address, city, longitude, latitude);
                int low = R.drawable.green_hazard;
                int med = R.drawable.orange_hazard;
                int high = R.drawable.red_hazard;
                String snippet = "";
                int severity_icon = R.drawable.green_hazard;
                Inspection latestInspection = extractLatestInspection(cursor);
                if(latestInspection != null) {
                    if (latestInspection.getLevel() == HazardLevel.LOW) {
                        snippet = "" + pos;
                        severity_icon = low;
                    } else if (latestInspection.getLevel() == HazardLevel.MODERATE) {
                        snippet = "" + pos;
                        severity_icon = med;
                    } else if (latestInspection.getLevel() == HazardLevel.HIGH) {
                        snippet = "" + pos;
                        severity_icon = high;
                    }
                    Log.d(TAG, "new restaurant added to map: " + name);
                    name = "poo";
                    ClusterMarker newClusterMarker = new ClusterMarker(temp ,
                            name,
                            snippet,
                            severity_icon,
                            newRestaurant);
                    mClusterManager.addItem(newClusterMarker);
                    mClusterManager.cluster();
                    pos++;
                } else {
                    Log.d(TAG, "null inspection");
                }

            }while(cursor.moveToNext());
        }

    }

    private Inspection extractLatestInspection(Cursor cursor){
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Inspection>>() {}.getType();
        String outputString = cursor.getString(DBAdapter.COL_INSPECTION_LIST);
        ArrayList<Inspection> inspectionsArray = gson.fromJson(outputString, type);
        Inspection latestInpection = null;
        if(inspectionsArray.size() > 0){
            latestInpection = Collections.max(inspectionsArray);
        }
        return latestInpection;
    }

    // Download CSV files
    private class ListUpdateTask extends AsyncTask<Void,Void, Boolean> {
        @Override
        protected void onPreExecute() {
            FragmentManager manager = getSupportFragmentManager();
            loadingDialog = new LoadingDialogFragment(); // loading dialog
            loadingDialog.show(manager, "LoadingDialog");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (!isCancelled()) {
                new SurreyDataDownloader().getCSVData(restaurantUpdate, MapsActivity.this);
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean receivedUpdate) {
            boolean update = receivedUpdate;
            if (update) {
                fillDatabaseWithUpdated();
                lastModified.setLastCheck(MapsActivity.this, LocalDateTime.now());
                lastModified.setLast_mod_restaurants(MapsActivity.this, restaurantUpdate.get(0).getLast_modified());
                lastModified.setLast_mod_inspections(MapsActivity.this, restaurantUpdate.get(1).getLast_modified());
                getUpdatedFiles();
                setupMap();

                finish();
                startActivity(getIntent());
                loadingDialog.dismiss();
            }
        }
    }
    private String TAG1 = "Tag1";
    private void fillInitialRestaurantList() {
        InputStream inputStream = getResources().openRawResource(R.raw.restaurants_itr1);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        Log.d(TAG, "INITIAL FILL CALLED!!!");
        restaurantManager.fillRestaurantManager(reader,this);
        InputStream is = getResources().openRawResource(R.raw.inspectionreports_itr1);
        BufferedReader inspectionReader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
        );
        restaurantManager.fillInspectionManager(inspectionReader);
    }

    private void getUpdatedFiles() {
        FileInputStream inputStream_rest;
        FileInputStream inputStream_insp;
        try {
            inputStream_rest = MapsActivity.this.openFileInput(SurreyDataDownloader.DOWNLOAD_RESTAURANTS);
            inputStream_insp = MapsActivity.this.openFileInput(SurreyDataDownloader.DOWNLOAD_INSPECTIONS);
            InputStreamReader inputReader_rest = new InputStreamReader(inputStream_rest, StandardCharsets.UTF_8);
            InputStreamReader inputReader_insp = new InputStreamReader(inputStream_insp, StandardCharsets.UTF_8);
            restaurantManager.fillRestaurantManager(new BufferedReader(inputReader_rest),this);
            restaurantManager.fillInspectionManager(new BufferedReader(inputReader_insp));
        } catch (FileNotFoundException e) {
            // No update files downloaded
        }
    }

    private void fillInitialDatabase(){
        InputStream inputStream = getResources().openRawResource(R.raw.restaurants_itr1);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        fillRestaurantDatabase(reader);
        InputStream is = getResources().openRawResource(R.raw.inspectionreports_itr1);
        BufferedReader inspectionReader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
        );
        fillInspectionsDatabase(inspectionReader);

    }

    private void fillDatabaseWithUpdated(){
        FileInputStream inputStream_rest;
        FileInputStream inputStream_insp;
        try {
            inputStream_rest = MapsActivity.this.openFileInput(SurreyDataDownloader.DOWNLOAD_RESTAURANTS);
            inputStream_insp = MapsActivity.this.openFileInput(SurreyDataDownloader.DOWNLOAD_INSPECTIONS);
            InputStreamReader inputReader_rest = new InputStreamReader(inputStream_rest, StandardCharsets.UTF_8);
            InputStreamReader inputReader_insp = new InputStreamReader(inputStream_insp, StandardCharsets.UTF_8);
            fillRestaurantDatabase(new BufferedReader(inputReader_rest));
            long startTime = System.nanoTime();
            fillInspectionsDatabase(new BufferedReader(inputReader_insp));
            long stopTime = System.nanoTime();
            Log.d(TAG, "TIME TAKEN: " + (stopTime - startTime));
        } catch (FileNotFoundException e) {
            // No update files downloaded
        }
    }



    private void fillRestaurantDatabase(BufferedReader reader){
        String line = "";
        try {
            reader.readLine();
            myDb.deleteAll();
            myDb.beginTransaction();
            while ((line = reader.readLine()) != null) {
                line = line.replace("\"", "");

                String[] attributes = line.split(",");
                String tracking = attributes[0];
                tracking = tracking.replace(" ", "");
                String name = attributes[1];

                int addrIndex = attributes.length - 5;
                for (int i = 2; i < addrIndex; i++) {
                    name = name.concat(attributes[i]);
                }
                String addr = attributes[addrIndex];
                String city = attributes[addrIndex + 1];
                float gpsLat = Float.parseFloat(attributes[addrIndex + 3]);
                float gpsLong = Float.parseFloat(attributes[addrIndex + 4]);
                ArrayList<Inspection> inspectionsArray = new ArrayList<>();
                Gson gson = new Gson();
                String inspections = gson.toJson(inspectionsArray);
                int favourite = 0;
                //read data
                myDb.insertRowRestaurant(tracking,
                        name,
                        addr,
                        city,
                        gpsLat,
                        gpsLong,
                        inspections,
                        favourite);

            }
            myDb.endTransactionSuccessful();

        } catch(IOException e){
            Log.wtf("MapsActivity", "error reading data file on line " + line, e);
        }

    }

    private void fillInspectionsDatabase(BufferedReader reader){
        String line = "";
        try {
            reader.readLine();
            Gson gson = new Gson();
            myDb.beginTransaction();
            while ((line = reader.readLine()) != null) {
                line = line.replace("\"", "");
                String[] tokens = line.split(",");

                //read data
                if (tokens.length > 0) {
                    String inspectionTracking = tokens[0];
                    Cursor restaurantCursor = myDb.searchRestaurants(DBAdapter.KEY_TRACKING, inspectionTracking, DBAdapter.MatchString.EQUALS);
                    String stringDate = tokens[1];
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                    LocalDate date = LocalDate.parse(tokens[1], formatter);
                    String stringType = tokens[2];
                    InspectionType inspectionType;
                    if(stringType.equals("Routine")){
                        inspectionType = InspectionType.ROUTINE;
                    }
                    else{
                        inspectionType = InspectionType.FOLLOWUP;
                    }
                    int numCritical = Integer.parseInt(tokens[3]);
                    int numNonCritical = Integer.parseInt(tokens[4]);
                    String violationLump = "[empty]";
                    String stringHazard = tokens[tokens.length-1];
                    HazardLevel hazard = getHazardLevel(tokens[tokens.length-1]);
                    Inspection inspection;
                    if(tokens.length > 5 && tokens[5].length() > 0) {
                        violationLump = getVioLump(tokens);
                        inspection = new Inspection(date, inspectionType, numCritical, numNonCritical, hazard, violationLump);
                    } else {
                        inspection = new Inspection(date, inspectionType, numCritical, numNonCritical, hazard);
                    }
                    if(restaurantCursor.moveToFirst()) {
                        //Type arrayType = new TypeToken<ArrayList<Inspection>>() {}.getType();
                        //String outputarray = restaurantCursor.getString(DBAdapter.COL_INSPECTION_LIST);
                        //ArrayList<Inspection> inspectionsListDB = gson.fromJson(outputarray, arrayType);
                        ArrayList<Inspection> inspectionsListDB = new ArrayList<>();
                        inspectionsListDB.add(inspection);
                        String trackingID = restaurantCursor.getString(DBAdapter.COL_TRACKING);
                        String inputString = gson.toJson(inspectionsListDB);
                        //String trackingID = restaurantCursor.getString(DBAdapter.COL_TRACKING);
                        //String inputString = "gobbledy goop";
                        myDb.updateRowInspections(trackingID, inputString);
                    }
                    //myDb.insertRowInspection(inspectionTracking, stringDate, stringType, numCritical, numNonCritical, violationLump, stringHazard);
                }
            }
            myDb.endTransactionSuccessful();
        } catch(IOException e){
            Log.wtf("RestaurantListActivity", "error reading data file on line " + line, e);
        }
    }

    private HazardLevel getHazardLevel(String hazard) {
        if (hazard.equals("High")) {
            return HazardLevel.HIGH;
        } else if (hazard.equals("Moderate")) {
            return HazardLevel.MODERATE;
        } else {
            return HazardLevel.LOW;
        }
    }

    private String getVioLump(String[] inspectionRow){
        StringBuilder lump = new StringBuilder();
        for (int i = 5; i < inspectionRow.length - 1; i++) {
            lump.append(inspectionRow[i]).append(",");
        }
        return lump.toString();
    }


    private boolean past20Hours() {
        lastModified = LastModified.getInstance(MapsActivity.this);
        LocalDateTime previous = lastModified.getLastCheck();
        LocalDateTime current = LocalDateTime.now();
        LocalDateTime compare = current.minusHours(20);
        return previous.isBefore(compare) || compare.isEqual(previous);
    }

    public static Intent makeLaunchIntentMapsActivity(Context context, int restIdx){
        Intent intent = new Intent(context, MapsActivity.class);
        intent.putExtra(REST_DETAILS_INDEX, restIdx);
        return intent;
    }

    private void extractDataFromIntent() {
        Intent intent = getIntent();
        int restaurant_details_idx = intent.getIntExtra(REST_DETAILS_INDEX, -1);
        stopLocationUpdates();
        if(restaurant_details_idx > 0) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(restaurantlatlog.get(restaurant_details_idx)));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(20));
            Marker mark = mClusterManagerRenderer.getMarker(Markerlist.get(restaurant_details_idx));
            if(mark != null){
                mark.showInfoWindow();
            }
        }
    }
}
