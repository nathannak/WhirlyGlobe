package com.mousebirdconsulting.autotester.TestCases;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.mousebird.maply.AttrDictionary;
import com.mousebird.maply.ComponentObject;
import com.mousebird.maply.GlobeController;
import com.mousebird.maply.MapController;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.MarkerInfo;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.ScreenMarker;
import com.mousebird.maply.SelectedObject;
import com.mousebird.maply.VectorObject;
import com.mousebirdconsulting.autotester.Framework.MaplyTestCase;
import com.mousebirdconsulting.autotester.R;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jmnavarro on 30/12/15.
 */
public class ScreenMarkersTestCase extends MaplyTestCase
{
	private ArrayList<ComponentObject> componentObjects = new ArrayList<>();

	public ScreenMarkersTestCase(Activity activity) {
		super(activity);
		setTestName("Screen Markers Test");
		setDelay(1000);
		this.implementation = TestExecutionImplementation.Both;
	}

	public ArrayList<ComponentObject> getComponentObjects() {
		return componentObjects;
	}

	@Override
	public boolean setUpWithMap(MapController mapVC) throws Exception {
		VectorsTestCase baseView = new VectorsTestCase(getActivity());
		baseView.setUpWithMap(mapVC);
		insertMarkers(baseView.getVectors(), mapVC);
		Point2d loc = Point2d.FromDegrees(-3.6704803, 40.5023056);
		mapVC.setPositionGeo(loc.getX(), loc.getX(), 2);
		return true;
	}

	@Override
	public boolean setUpWithGlobe(GlobeController globeVC) throws Exception {
		VectorsTestCase baseView = new VectorsTestCase(getActivity());
		baseView.setUpWithGlobe(globeVC);
		insertMarkers(baseView.getVectors(), globeVC);
		Point2d loc = Point2d.FromDegrees(-3.6704803, 40.5023056);
		globeVC.animatePositionGeo(loc.getX(), loc.getX(), 0.9, 1);
		return true;
	}

	public class MarkerProperties {
		public String city;
		public String subject;
	}

	private void insertMarkers(ArrayList<VectorObject> vectors, MaplyBaseController baseVC) {
		MarkerInfo markerInfo = new MarkerInfo();
		Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.testtarget);
//		markerInfo.setMinVis(0.f);
//		markerInfo.setMaxVis(2.5f);
		markerInfo.setClusterGroup(0);
		markerInfo.setLayoutImportance(1.f);
		if (baseVC instanceof GlobeController){
			((GlobeController)baseVC).gestureDelegate = this;
		}
		if (baseVC instanceof MapController){
			((MapController) baseVC).gestureDelegate = this;
		}

		ArrayList<ScreenMarker> markers = new ArrayList<ScreenMarker>();

		MarkerProperties properties = new MarkerProperties();
		properties.city = "Moskow";
		properties.subject = "Moskow";
		ScreenMarker moskow = new ScreenMarker();
		moskow.loc = Point2d.FromDegrees(37.616667, 55.75); // Longitude, Latitude
		moskow.image = icon;
		moskow.size = new Point2d(128, 128);
		moskow.selectable = true;
		moskow.userObject = properties;
		markers.add(moskow);

		for (VectorObject vector : vectors) {
			ScreenMarker marker = new ScreenMarker();
			marker.image = icon;
			Point2d centroid = vector.centroid();
			if (centroid != null) {
				marker.loc = centroid;
				marker.size = new Point2d(128, 128);
				marker.rotation = Math.random() * 2.f * Math.PI;
				marker.selectable = true;
//				marker.offset = new Point2d(-64,-64);
				AttrDictionary attrs = vector.getAttributes();
				if (attrs != null) {
					MarkerProperties properties1 = new MarkerProperties();
					properties1.city = attrs.getString("ADMIN");
					properties1.subject = "AUTOTESTER";
					marker.userObject = properties1;
					markers.add(marker);
				}
			}
		}

		ComponentObject object = baseVC.addScreenMarkers(markers, markerInfo, MaplyBaseController.ThreadMode.ThreadAny);
		if (object != null) {
			componentObjects.add(object);
		}
	}

	@Override
	public void userDidSelect(GlobeController globeControl, SelectedObject[] selObjs, Point2d loc, Point2d screenLoc) {
		String msg = "Selected feature count: " + selObjs.length;
		for (SelectedObject obj : selObjs) {
			if (obj.selObj instanceof ScreenMarker) {
				ScreenMarker screenMarker = (ScreenMarker) obj.selObj;
				MarkerProperties properties = (MarkerProperties) screenMarker.userObject;
				msg += "\nScreen Marker: " + properties.city + ", " + properties.subject;
			}
		}
		Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
	}

	@Override
	public void userDidSelect(MapController mapControl, SelectedObject[] selObjs, Point2d loc, Point2d screenLoc) {
		String msg = "Selected feature count: " + selObjs.length;
		for (SelectedObject obj : selObjs) {
			if (obj.selObj instanceof ScreenMarker) {
				ScreenMarker screenMarker = (ScreenMarker) obj.selObj;
				MarkerProperties properties = (MarkerProperties) screenMarker.userObject;
				msg += "\nScreen Marker: " + properties.city + ", " + properties.subject;
			}
		}
		Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
	}
}
