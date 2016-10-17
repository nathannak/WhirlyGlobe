package com.mousebirdconsulting.autotester.TestCases;

import android.app.Activity;

import com.mousebird.maply.GlobeController;
import com.mousebird.maply.MapController;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.QuadImageTileLayer;
import com.mousebird.maply.SphericalMercatorCoordSystem;
import com.mousebirdconsulting.autotester.ConfigOptions;
import com.mousebirdconsulting.autotester.Framework.MaplyTestCase;
import com.mousebirdconsulting.autotester.IndexTestTileSource;

/**
 * Test case for Imagery Pro Quad Images layer.
 * This one just does whole pixel indexing.  No slices.
 */
public class IndexWholeTestCase extends MaplyTestCase
{
    public IndexWholeTestCase(Activity activity) {
        super(activity);

        setTestName("Indexed Image, No slices");
        setDelay(20);
        this.implementation = TestExecutionImplementation.Both;
    }

    private QuadImageTileLayer setupImageLayer(ConfigOptions.TestType testType, MaplyBaseController baseController) throws Exception {

        IndexTestTileSource tileSource = new IndexTestTileSource(0,10,true,0);
        SphericalMercatorCoordSystem coordSystem = new SphericalMercatorCoordSystem();
        QuadImageTileLayer baseLayer = new QuadImageTileLayer(baseController, coordSystem, tileSource);
        baseLayer.setImageDepth(4);
        if (testType == ConfigOptions.TestType.MapTest)
        {
//			baseLayer.setSingleLevelLoading(true);
//			baseLayer.setUseTargetZoomLevel(true);
//			baseLayer.setMultiLevelLoads(new int[]{-3});
            baseLayer.setCoverPoles(false);
            baseLayer.setHandleEdges(false);
        } else {
            baseLayer.setCoverPoles(true);
            baseLayer.setHandleEdges(true);
        }

        baseLayer.setDrawPriority(MaplyBaseController.ImageLayerDrawPriorityDefault+100);
        return baseLayer;
    }

    @Override
    public boolean setUpWithGlobe(GlobeController globeVC) throws Exception {
        CartoDBMapTestCase baseCase = new CartoDBMapTestCase(getActivity());
        baseCase.setUpWithGlobe(globeVC);

        globeVC.addLayer(this.setupImageLayer(ConfigOptions.TestType.GlobeTest, globeVC));
        Point2d loc = Point2d.FromDegrees(-3.6704803, 40.5023056);
        globeVC.animatePositionGeo(loc.getX(), loc.getY(), 2.0, 1.0);
        return true;
    }

    @Override
    public boolean setUpWithMap(MapController mapVC) throws Exception {
        CartoDBMapTestCase baseCase = new CartoDBMapTestCase(getActivity());
        baseCase.setUpWithMap(mapVC);

        mapVC.addLayer(this.setupImageLayer(ConfigOptions.TestType.MapTest, mapVC));
        Point2d loc = Point2d.FromDegrees(-3.6704803, 40.5023056);
        mapVC.setPositionGeo(loc.getX(), loc.getY(), 2.0);
        return true;
    }}
