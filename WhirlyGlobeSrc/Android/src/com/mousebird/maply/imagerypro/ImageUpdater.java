package com.mousebird.maply.imagerypro;

import android.util.Log;

import com.mousebird.maply.ActiveObject;
import com.mousebird.maply.ChangeSet;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.Point4d;
import com.mousebird.maply.Shader;

/** The Image Updater is called every frame to decide what image is being shown.
 * This object controls what part of the image stack is being displayed.
 * If you want to get fancy, you can override this and update it yourself.
 */
public class ImageUpdater implements ActiveObject
{
    /**
     * The shader we'll be tweaking
     */
    public Shader program;

    /**
     * The last valid value for currentImage
     */
    public float maxCurrentImage;

    /**
     * brief The period over which we'll switch them all
     */
    public double period = 0.0;

    /**
     * Start time, for offset purposes
     */
    public double startTime;

    /**
     * Set to 0 if there are no slices, otherwise the number of slices in a single texel
     */
    public int slicesPerImage = 0;

    /**
     * If set we're doing cubic interpolation over the slices (time)
     */
    public boolean cubicTemporal = false;

    /**
     * If set we'll print way too much information
     */
    public boolean debugMode = false;

    int imageDepth = 0;

    MaplyBaseController maplyControl = null;
    IProQuadImageTileLayer imageLayer = null;

    double lastUpdate;
    boolean doUpdate = true;

    ImageUpdater(IProQuadImageTileLayer inImageLayer)
    {
        imageLayer = inImageLayer;
        maplyControl = imageLayer.maplyControl;
        imageDepth = imageLayer.getImageDepth();
        startTime = System.currentTimeMillis()/1000.0;
        if (imageLayer.getImageDepth() > 1)
            startTime = startTime-imageLayer.getCurrentImage()/imageLayer.getImageDepth() * period;
    }

    /**
     * Force the updater to explicitly do an update on the next frame
     */
    public void forceUpdate()
    {
        doUpdate = true;
    }

    @Override
    public boolean hasChanges()
    {
        return doUpdate || imageLayer.getAnimationPeriod() != 0.0;
    }

    // Change the current image based on the time
    public void activeUpdate()
    {
        if (imageLayer == null || !imageLayer.getEnable() || !hasChanges())
            return;

        double now = System.currentTimeMillis()/1000.0;

        double where = 0.0;
        final double minTime = 0.0;
        final double maxTime = maxCurrentImage;

        if (imageLayer.getAnimationPeriod() != 0.0)
            where = ((now-startTime) % period)/period * maxCurrentImage;
        else
            where = imageLayer.getCurrentImage();

        // Might override the automatic value
        if (imageLayer.positionSettingDelegate != null)
            where = imageLayer.positionSettingDelegate.calculatePositionForImagesLayer(imageLayer,minTime,maxTime);

        // Change which textures are active if necessary
        int image0 = (int)Math.floor(where);
        int image1 = (int)Math.ceil(where);
        int imageDepth = imageLayer.getImageDepth();

        if (imageLayer.getAnimationWrap())
        {
            if (image1 >= imageDepth)
                image1 = 0;
        }
        if (image0 >= imageDepth-2 && imageDepth > 2)
        {
            image0 = imageDepth-2;
            image1 = image0+1;
        }
        if (image1 >= imageDepth)
        {
            image1 = imageDepth-1;
            image0 = image1-1;
            image0 = Math.max(image0,0);
        }

        //    NSLog(@"currentImage = %d->%d -> %f",image0,image1,t);

        // Change the images to give us start and finish
        ChangeSet changes = new ChangeSet();
        imageLayer.setCurrentImageSimple((float)where);
        imageLayer.setCurrentImages(image0,image1,changes);
        imageLayer.getLayerThread().scene.addChanges(changes);

        if (slicesPerImage > 0)
        {
            double withinImage = slicesPerImage * (where - image0);
            int stackIndex0 = (int)Math.floor(withinImage);
            double withinFrac = withinImage - stackIndex0;
            int stackIndex1 = (int)Math.ceil(withinImage);
            double interpSlice = withinImage - stackIndex0;

            if (program != null)
            {
                if (cubicTemporal)
                {
                    double cub[] = CubicInterpolationVec(withinFrac);
                    int stackIndexMinus1 = stackIndex0-1;
                    // Note: Should adjust the where we're handing setCurrentImage:
                    stackIndexMinus1 = Math.max(stackIndexMinus1,0);
                    int stackIndex2 = stackIndex1+1;
                    stackIndexMinus1 = Math.min(stackIndexMinus1,7);
                    stackIndex0 = Math.min(stackIndex0,7);
                    stackIndex1 = Math.min(stackIndex1,7);
                    stackIndex2 = Math.min(stackIndex2,7);

                    if (debugMode)
                        Log.d("Maply","updateForFrame: stack-1 = " + stackIndexMinus1 +
                                ", stack0 = " + stackIndex0 + ", stack1 = " + stackIndex1 + ", stack2 = " + stackIndex2
                                + ",interp = "+ withinFrac);

                    program.setUniform("u_stackIndex0",stackIndexMinus1);
                    program.setUniform("u_stackIndex1",stackIndex0);
                    program.setUniform("u_stackIndex2",stackIndex1);
                    program.setUniform("u_stackIndex3",stackIndex1);
                    program.setUniform("u_interpCubic",new Point4d(cub[0],cub[1],cub[2],cub[3]));
                } else {
                    // Linear interpolation in time
                    program.setUniform("u_stackIndex0",stackIndex0);
                    program.setUniform("u_stackIndex1",stackIndex1);
                    program.setUniform("u_interpSlice",interpSlice);

                    if (debugMode)
                        Log.d("Maply","UpdateForFrame:  stackIndex0 = " + stackIndex0 + ", stackIndex1 = " + stackIndex1 +
                                ", interpSlice = " + interpSlice);
                }
            }
        }  else {
            double t = where-image0;
            program.setUniform("u_interp",t);

            if (debugMode)
                Log.d("Maply","UpdateForFrame: image0 = " + image0 + " image1 = " + image1 + " currentImage = " + where);
        }

        // Update the developer with the time
        if (imageLayer.positionFeedbackDelegate != null)
        {
            switch (imageLayer.positionFeedbackType)
            {
                case MaplyIProPosFeedbackContinuous:
                    imageLayer.positionFeedbackDelegate.positionForImagesLayer(imageLayer,minTime,maxTime,where);
                    break;
                case MaplyIProPosFeedbackPeriodic:
                {
                    double curTime = now;
                    if (curTime - lastUpdate > imageLayer.positionUpdatePeriod)
                    {
                        lastUpdate = curTime;
                        final double finalWhere = where;
                        maplyControl.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageLayer.positionFeedbackDelegate.positionForImagesLayer(imageLayer,minTime,maxTime,finalWhere);
                            }
                        });
                    }
                }
                break;
            }
        }

        doUpdate = false;
    }

    /** Generate the parameters for cubic interpolation
     * <br>
     * We use cubic and bicubic interpolation in a variety of places.
     * This version runs on the CPU and uses the exact interpolation we use in shaders.
     */
    static public double[] CubicInterpolationVec(double t)
    {
        double t2 = t * t;
        double t3 = t2 * t;
        double[] rets = new double[4];
        rets[0] = (  -t3 + 3*t2 - 3*t + 1);
        rets[1] = ( 3*t3 - 6*t2       + 4);
        rets[2] = (-3*t3 + 3*t2 + 3*t + 1);
        rets[3] = ( t3);

        return rets;
    }
}