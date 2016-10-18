package com.mousebirdconsulting.autotester.TestCases;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.mousebird.maply.GlobeController;
import com.mousebird.maply.MapController;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.RemoteTileInfo;
import com.mousebird.maply.SphericalMercatorCoordSystem;
import com.mousebird.maply.imagerypro.ImageSourceLayout;
import com.mousebird.maply.imagerypro.MultiplexTileSource;
import com.mousebird.maply.imagerypro.QuadImageTileLayer;
import com.mousebirdconsulting.autotester.ConfigOptions;
import com.mousebirdconsulting.autotester.Framework.MaplyTestCase;
import com.mousebirdconsulting.autotester.R;

import java.util.ArrayList;

/**
 * Test case pointing directly at Met Office data feeds.
 */
public class MetOffice8BitTestCase extends MaplyTestCase
{
    public MetOffice8BitTestCase(Activity activity) {
        super(activity);

        setTestName("Met Office 8 bit test case");
        setDelay(20);
        this.implementation = TestExecutionImplementation.Both;
    }

    protected String baseURL = "%@";

    private QuadImageTileLayer setupImageLayer(ConfigOptions.TestType testType, MaplyBaseController baseController) throws Exception {

        ArrayList<String> tileURLs = new ArrayList<String>();
        tileURLs.add("1476720000");
        tileURLs.add("1476748800");
        tileURLs.add("1476792000");
        tileURLs.add("1476824400");
        tileURLs.add("1476835200");

        RemoteTileInfo tileInfos[] = new RemoteTileInfo[tileURLs.size()];
        int where = 0;
        for (String url : tileURLs) {
            tileInfos[where++] = new RemoteTileInfo(
                    "http://visual-weather-elb-896992404.eu-west-1.elb.amazonaws.com/mapimage/whirlyimg8/Rain/" + url + "/{z}/{x}x{y}.png", null, 5, 7);
        }

        SphericalMercatorCoordSystem coordSystem = new SphericalMercatorCoordSystem();
        MultiplexTileSource tileSource = new MultiplexTileSource(baseController,tileInfos,coordSystem);
        tileSource.debugOutput = true;

        // Describe the input data sources
        ImageSourceLayout srcLayout = new ImageSourceLayout();
        srcLayout.slicesInLastImage = 1;
        srcLayout.indexed = true;
        srcLayout.sourceWidth = ImageSourceLayout.MaplyIProSourceWidth.MaplyIProWidthWhole;
        srcLayout.slicesInLastImage = 1;

        Bitmap colorramp = BitmapFactory.decodeResource(getActivity().getResources(),
                R.drawable.colorramp);

        QuadImageTileLayer baseLayer = new QuadImageTileLayer(baseController, coordSystem, tileSource);
        baseLayer.setImageDepth(4);
        baseLayer.setSourceLayout(srcLayout);
        baseLayer.setInternalImageFormat(QuadImageTileLayer.MaplyIProInternalImageFormat.MaplyIProImage4Layer8Bit);
        baseLayer.setAnimationPeriod(6.0);
        baseLayer.setAnimationWrap(false);
        baseLayer.setRampImage(colorramp);
        baseLayer.setHandleEdges(false);
        baseLayer.setCoverPoles(false);
        baseLayer.setSingleLevelLoading(true);

        baseLayer.setDebugMode(false);

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
    }
}
