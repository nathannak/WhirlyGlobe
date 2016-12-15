package com.mousebirdconsulting.autotester.TestCases;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.mousebird.maply.AttrDictionary;
import com.mousebird.maply.ComponentObject;
import com.mousebird.maply.GlobeController;
import com.mousebird.maply.MapController;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.MarkerInfo;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.ScreenMarker;
import com.mousebird.maply.VectorObject;
import com.mousebirdconsulting.autotester.ChatHeadService;
import com.mousebirdconsulting.autotester.Framework.MaplyTestCase;
import com.mousebirdconsulting.autotester.R;
import com.mousebirdconsulting.autotester.Utils;

import java.util.ArrayList;
import java.util.Random;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by jmnavarro on 30/12/15.
 **/

public class AndroidLocationTestCase extends MaplyTestCase implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {

    //for chat head permisions
    private int OVERLAY_PERMISSION_REQ_CODE_CHATHEAD = 1234;
    private static int OVERLAY_PERMISSION_REQ_CODE_CHATHEAD_MSG = 5678;
    //
    ComponentObject object=null;
    Point2d centroid = null;
    MapController mapVC_copy = null;
    //
    public static GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    //
    private Double lat;
    private Double lng;
    //
    Context ctx;
    //
    Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.testtarget);
    private ArrayList<ComponentObject> componentObjects = new ArrayList<>();
    //
    private float currentDegree = 0f;
    private float degree = 0f;
    private Boolean firstTime = true;
    //

    //unregister at some point to prevent leaking ToDO fix
    private SensorManager mSensorManager;
    //
    Double valueX;
    Double valueY;

    // If these values change to true, we use the fake set of location instead of real location
    // ChatHeadService also changes these values
    public static Boolean northUp = false;
    public static Boolean headingUp = false;
    public static Boolean engageFakeLocation = false;


    public AndroidLocationTestCase(Activity activity) {

        //
        super(activity);
        setTestName("AndroidLocation TestCase");
        setDelay(1000);
        this.implementation = TestExecutionImplementation.Both;
        ctx = activity.getBaseContext();
        //


        //connect location
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        //

        //resigster sensor listener and initiate
        mSensorManager = (SensorManager) ctx.getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
        //


    }

    //
    public ArrayList<ComponentObject> getComponentObjects() {
        return componentObjects;
    }

    @Override
    public boolean setUpWithMap(MapController mapVC) throws Exception {

        //show chat head
        if(Utils.canDrawOverlays(ctx))
        {
            startChatHead();
        }else{
            requestPermission(OVERLAY_PERMISSION_REQ_CODE_CHATHEAD);
        }


//        // not wise to call the location connect command and sensor registerer here since it is a recursive function!
//        if(!mGoogleApiClient.isConnected())
//        {
//            buildGoogleApiClient();
//            mGoogleApiClient.connect();
//        }
//
//        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
//                SensorManager.SENSOR_DELAY_UI);

        VectorsTestCase baseView = new VectorsTestCase(getActivity());
        baseView.setUpWithMap(mapVC);
        //

        //keep a copy to be used later
        mapVC_copy = mapVC;
        insertMarker(mapVC);

        //chnage the view point with the new location
        Point2d loc = Point2d.FromDegrees(-3.6704803, 40.5023056);
        mapVC.setPositionGeo(loc.getX(), loc.getX(), 2);
        return true;
        //
    }

    @Override
    public boolean setUpWithGlobe(GlobeController globeVC) throws Exception {
        VectorsTestCase baseView = new VectorsTestCase(getActivity());
        baseView.setUpWithGlobe(globeVC);

        //apply same changes here
        //insertMarkers(baseView.getVectors(), globeVC);

        Point2d loc = Point2d.FromDegrees(-3.6704803, 40.5023056);
        globeVC.animatePositionGeo(loc.getX(), loc.getX(), 0.9, 1);
        return true;
    }
    //

    //
    private void insertMarker(MaplyBaseController baseVC) {
        MarkerInfo markerInfo = new MarkerInfo();

        markerInfo.setClusterGroup(0);
        markerInfo.setLayoutImportance(1.f);

        //
        ArrayList<ScreenMarker> markers = new ArrayList<ScreenMarker>();
        //

        //
        //for (VectorObject vector : vectors){}
        //

        ScreenMarker marker = new ScreenMarker();
        marker.image = icon;

        //Point2d centroid = vector.centroid();

        //Point2d centroid = Point2d.FromDegrees(lng,lat);
        centroid = Point2d.FromDegrees(lng,lat);

        //
        if (centroid != null) {

            //
            Log.v("AndroidLocationTestCase","centroid.getX() :" + centroid.getX());
            Log.v("AndroidLocationTestCase","centroid.getY() :" + centroid.getY());
            //

            marker.loc = centroid;
            marker.size = new Point2d(128, 128);

            //
            valueX = centroid.getX();
            valueY = centroid.getY();
            //

            //moving camera to new location
            if(valueX != null && valueY != null)
            {
                mapVC_copy.animatePositionGeo(valueX+0.001,valueY+0.001,0.05,0.25);
            }
            //

            //previous method of getting rotation
            //marker.rotation = Math.random() * 2.f * Math.PI;

            //new method of getting rotstion from sensor listener
            // ToDO check for formula accuracy
            marker.rotation = currentDegree * Math.PI/180;

            //
            marker.selectable = true;

            // not sure wht this is but may come handy later
            // marker.offset = new Point2d(-64,-64);

            //AttrDictionary attrs = vector.getAttributes();
            //if (attrs != null) {
            //    marker.userObject = attrs.getString("ADMIN");
            markers.add(marker);
            //}

        }

        //Changed the following command so we can keep ComponentObject instance around
        //ComponentObject object = baseVC.addScreenMarkers(markers, markerInfo, MaplyBaseController.ThreadMode.ThreadAny);
        object = baseVC.addScreenMarkers(markers, markerInfo, MaplyBaseController.ThreadMode.ThreadAny);

        //
        if (object != null)
        {
            componentObjects.add(object);
        }
        //

        //does not work for zooming Todo fix
        //baseVC.currentMapZoom(centroid);
        //

    }

    //implementing location related methods ...

    @Override
    public void onLocationChanged(Location location) {

        // this function gets called every 0 seconds
        if (location != null)
        {

            // get real location if we are not using fake locations
            if(!engageFakeLocation) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }
            else if (engageFakeLocation)
            {
                //use fak locations
                //37.3118453 -121.9763707
                Random rn = new Random();
                lat = 37 + ((rn.nextInt(100 - 1 + 1) + 1)/100.00);

                rn = new Random();
                lng = -122 + ((rn.nextInt(100 - 1 + 1) + 1)/100.00);
                //
            }
            //
            if(mapVC_copy != null)
            {
                mapVC_copy.removeObject(object, MaplyBaseController.ThreadMode.ThreadAny);
                insertMarker(mapVC_copy);

                //consider locks
                //if no lock mode is on just get the phone tilt by degrees
                if(!headingUp && !northUp)
                {

                    //ToDO compelte no lock formula

                    //northUp is on
                }else if(northUp && !headingUp){

                    //this is how it is implemented in iOS project
                    //ToDO check for formula accuracy
                    mapVC_copy.setHeading(0);

                    //headingUp is on
                }else if(!northUp && headingUp){

                    //this is how it is implemented in iOS project
                    // currentDegree * Math.PI/180 is the way it is calculated in insertMarker
                    // ToDO check for formula accuracy
                    mapVC_copy.setHeading( currentDegree * Math.PI/180 + 2*Math.PI );

                }

                /**
                 Right, the location is changing.
                 The next part is to make the viewpoint move around as the location changes.
                 You can use setPosition for that initially, but you’ll want to try out the animate calls in the GlobeController or MapController.

                 -moving camera to new location after inserting a new marker, eventually looks like putting it in
                 -insertMarker function works way better ...

                 //                if(valueX != null && valueY != null)
                 //                {
                 //                    mapVC_copy.animatePositionGeo(valueX+0.001,valueY+0.001,0.05,0.5);
                 //                }
                 **/

                Log.v("AndroidLocationTestCase", " re-inserting marker because of location change");
            }
            Log.v("AndroidLocationTestCase", "lat and lng on from onLocationChanged listener have been updated - assisted location from main - are:" + Double.toString(location.getLatitude()) + " " + Double.toString(location.getLongitude()));
        }
    }

    //
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0); // Update location every 0 second
        //
        if(mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

        if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mLastLocation != null) {
            lat = mLastLocation.getLatitude();
            lng = mLastLocation.getLongitude();
        }
    }
    //
    @Override
    public void onConnectionSuspended(int i) {}
    //
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        buildGoogleApiClient();
        Log.v("AndroidLocationTestCase", "search new - in onConnected FAILED, creating googleapi client again");
    }
    //
    synchronized void buildGoogleApiClient() {
        //mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())

        mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }
    //
    //implementing sensor listener
    //
    @Override
    public void onSensorChanged(SensorEvent event) {

        degree = Math.round(event.values[0]);

        //insert marker if rotation is greater than 3 degrees
        //inserting new marler comes with a delay, because of animation transition time
        if(!firstTime && Math.abs(degree - currentDegree) > 3)
        {

//            // these won't work - removeObject should be called on mapController to effectively remove the marker
//            if(object != null)
//            {
//                componentObjects.remove(object);
//                componentObjects.clear();
//                object = null;
//            }
//
            //
            if(mapVC_copy != null)
            {
                mapVC_copy.removeObject(object, MaplyBaseController.ThreadMode.ThreadAny);
                insertMarker(mapVC_copy);
                Log.v("AndroidLocationTestCase", " re-inserting marker because of heading change, new heading: " + Float.toString(degree) + " degrees");
            }
        }

        currentDegree = degree;
        firstTime = false;
        Log.v("AndroidLocationTestCase", " Heading: " + Float.toString(degree) + " degrees");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // not in use
    }

    //chat head stuff ....
    private void startChatHead(){
        ctx.startService(new Intent(ctx, ChatHeadService.class));
    }

    private void needPermissionDialog(final int requestCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setMessage("You need to allow permission");
        builder.setPositiveButton("OK",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        requestPermission(requestCode);
                    }
                });
        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void requestPermission(int requestCode){
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + ctx.getPackageName()));

        //ctx.startActivityForResult(intent, requestCode);
        showPermission(requestCode);
    }


    protected void showPermission(int requestCode) {

        if (requestCode == OVERLAY_PERMISSION_REQ_CODE_CHATHEAD) {
            if (!Utils.canDrawOverlays(ctx)) {
                needPermissionDialog(requestCode);
            }else{
                startChatHead();
            }

        }else if(requestCode == OVERLAY_PERMISSION_REQ_CODE_CHATHEAD_MSG){
            if (!Utils.canDrawOverlays(ctx)) {
                needPermissionDialog(requestCode);
            }else{
                //showChatHeadMsg();
            }
        }
    }
}
