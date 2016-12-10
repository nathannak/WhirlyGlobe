/*
 *  QuadImageTileLayer.java
 *  ImageryPro
 *
 *  Created by Steve Gifford on 10/12/16.
 *  Copyright 2011-2016 mousebird consulting
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.mousebird.maply.imagerypro;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mousebird.maply.ChangeSet;
import com.mousebird.maply.CoordSystem;
import com.mousebird.maply.Layer;
import com.mousebird.maply.LayerThread;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.MaplyImageTile;
import com.mousebird.maply.MaplyRenderer;
import com.mousebird.maply.MaplyTexture;
import com.mousebird.maply.MaplyTileID;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.QuadImageTileLayer;
import com.mousebird.maply.QuadImageTileLayerInterface;
import com.mousebird.maply.Scene;
import com.mousebird.maply.Shader;
import com.mousebird.maply.ViewState;

/**
 * The quad image tiling layer manages a self contained basemap.  Basemaps are
 * local or remote collections of tiny images, often 256 pixels on a side.  This
 * object pulls the ones it needs from a remote or local source and displays them
 * as a coherent whole to the user
 * <p>
 * You'll need to create one of these and add it to the layerThread.  You'll
 * also need to pass it a TileSource.  That will do the actual data fetching
 * on demand.
 *
 * @author sjg
 *
 */
public class IProQuadImageTileLayer extends Layer implements LayerThread.ViewWatcherInterface, QuadImageTileLayerInterface
{
    /// Image format used internally (e.g. stored by OpenGL)
    public enum MaplyIProInternalImageFormat {
        MaplyIProImage4Layer8Bit,
        MaplyIProImageUShort565,
        MaplyIProImageUShort4444,
        MaplyIProImageUShort5551,
        MaplyIProImageByte
    };

    public enum MaplyIProTemporalInterpolation {
        MaplyIProTemporalNearest,
        MaplyIProTemporalLinear,
        MaplyIProTemporalCubic
    };

    public enum MaplyIProSpatialInterpolation {
        MaplyIProSpatialBilinear,
        MaplyIProSpatialBicubic
    };

    // Set when the layer is active.
    boolean valid = false;

    private IProQuadImageTileLayer()
    {
    }

    public MaplyBaseController maplyControl = null;
    public CoordSystem coordSys = null;
    com.mousebird.maply.QuadImageTileLayer.TileSource tileSource = null;
    boolean flipY = true;

    /**
     * Construct a quad image tile layer.  You'll pass it over to the MaplyController to
     * handle.
     *
     * @param inMaplyControl The maply controller this will be part of.
     * @param inCoordSys Coordinate system the layer will work in, probably Spherical Mercator.
     * @param inTileSource Tile source for images.
     */
    public IProQuadImageTileLayer(MaplyBaseController inMaplyControl,CoordSystem inCoordSys,com.mousebird.maply.QuadImageTileLayer.TileSource inTileSource)
    {
        maplyControl = inMaplyControl;
        coordSys = inCoordSys;
        tileSource = inTileSource;
        ChangeSet changes = new ChangeSet();
        initialise(coordSys,changes);
        LayerThread layerThread = maplyControl.getLayerThread();
        if (layerThread != null)
            layerThread.addChanges(changes);
        setSimultaneousFetches(8);
        setDrawPriority(MaplyBaseController.ImageLayerDrawPriorityDefault);
    }

    public void finalize()
    {
        dispose();
    }

    @Override
    public float getMinTime()
    {
        // Update every 1/10s
        return 0.1f;
    }

    @Override
    public float getMaxLagTime()
    {
        // Want an update no less often than this
        // Note: What?
        return 4.0f;
    }

    boolean currentImageSetBeforeStart = false;
    float currentImageValue;
    int[] currentPriorities = null;


    /**
     * Called by the layer thread.  Don't call this directly.
     */
    public void startLayer(LayerThread layerThread)
    {
        super.startLayer(layerThread);

        imageUpdater = new ImageUpdater(this);

        // Build the Shader if it isn't set already
        if (shader == null)
            shader = generateShader();
        updateUpdater();
        setAnimationPeriod(animationPeriod);
        setAnimationWrap(animationWrap);

        setMaxCurrentImage(calcNumberOfFrames()*widthPerFrame());

        layerThread.addWatcher(this);
        Point2d ll = new Point2d(coordSys.ll.getX(),coordSys.ll.getY());
        Point2d ur = new Point2d(coordSys.ur.getX(),coordSys.ur.getY());
        nativeStartLayer(layerThread.scene,layerThread.renderer,ll,ur,tileSource.minZoom(),tileSource.maxZoom(),tileSource.pixelsPerSide());

        if (currentImageSetBeforeStart)
        {
            ChangeSet changes = new ChangeSet();
            setCurrentImages((int)currentImageValue,(int)(currentImageValue+1), changes);

            setFrameLoadingPriority(currentPriorities, changes);

            if (layerThread.scene != null)
                changes.process(layerThread.scene);
        }

        scheduleEvalStep();

        valid = true;

        if (startEnable != true)
            setEnable(startEnable);
    }

    /**
     * Builds a Shader based on local parameters.
     * @return
     */
    Shader generateShader()
    {
        RampStackShader shaderGen = new RampStackShader();
        shaderGen.setTexelInputType(RampStackShader.TexelInputType.TexelWhole.ordinal());
        if (sourceLayout.indexed)
            shaderGen.setIndexType(RampStackShader.IndexType.IndexOn.ordinal());
        else
            shaderGen.setIndexType(RampStackShader.IndexType.IndexOff.ordinal());
        switch (sourceLayout.sourceWidth)
        {
            case MaplyIProWidth4Bits:
                shaderGen.setStackType(RampStackShader.StackType.Stack4Bits.ordinal());
                break;
            case MaplyIProWidth8Bits:
                shaderGen.setStackType(RampStackShader.StackType.Stack8Bits.ordinal());
                break;
            case MaplyIProWidthWhole:
                shaderGen.setStackType(RampStackShader.StackType.StackWhole.ordinal());
                break;
            default:
                Log.d("IPro","QuadImageLayer: Unsupported source width for shader generator");
                break;
        }
        switch (sourceLayout.pixelOrder)
        {
            case MaplyIProOrderRGBA:
                shaderGen.setPixelByteOrder(RampStackShader.PixelByteOrderType.OrderRGBA.ordinal());
                break;
            case MaplyIProWidthABGR:
                shaderGen.setPixelByteOrder(RampStackShader.PixelByteOrderType.OrderABGR.ordinal());
                break;
        }
        shaderGen.setStackCount(0);
        switch (temporalInterpolate)
        {
            case MaplyIProTemporalNearest:
                shaderGen.setTemporalInterpolation(RampStackShader.TemporalInterpType.NearestInterpTemporal.ordinal());
                break;
            case MaplyIProTemporalLinear:
                shaderGen.setTemporalInterpolation(RampStackShader.TemporalInterpType.LinearInterpTemporal.ordinal());
                break;
            case MaplyIProTemporalCubic:
                shaderGen.setTemporalInterpolation(RampStackShader.TemporalInterpType.CubicInterpTemporal.ordinal());
                break;
        }
        switch (spatialInterpolate)
        {
            case MaplyIProSpatialBilinear:
                shaderGen.setSpatialInterpolation(RampStackShader.SpatialInterpType.BilinearInterpSpatial.ordinal());
                break;
            case MaplyIProSpatialBicubic:
                shaderGen.setSpatialInterpolation(RampStackShader.SpatialInterpType.BicubicInterpSpatial.ordinal());
                break;
        }

        String vertShader,fragShader,errStr;
        String errString = shaderGen.generateShaders();
        if (errString != null)
        {
            Log.e("IPro","Failed to generate shader in MaplyIProQuadImagesLayer.  Using default shader.  Error:\n" + errString);
        }
        vertShader = shaderGen.getVertexShader();
        fragShader = shaderGen.getFragmentShader();

        shader = new Shader("Generated ImageryPro Shader",vertShader,fragShader,maplyControl);
        if (!shader.valid())
        {
            Log.e("IPro","Failed to set up shader " + shader.getName() + " Here's the code:\nVertex Shader:\n" + vertShader + "\nFragment Shader:\n" + fragShader);
        } else {
            maplyControl.addShaderProgram(shader,shader.getName());
            if (rampImage != null)
                setRampImage(rampImage);
            shader.setUniform("u_textureAtlasSize",textureAtlasSize);
            shader.setUniform("u_textureAtlasSizeInv",1.0/textureAtlasSize);

            imageUpdater.program = shader;
            setShaderNative(shader.getID());
        }

        return shader;
    }

    // Update the active object based on current parameters
    void updateUpdater()
    {
        imageUpdater.program = shader;
        imageUpdater.maxCurrentImage = getMaxCurrentImage();
        imageUpdater.period = getAnimationPeriod();
        switch (sourceLayout.sourceWidth)
        {
            case MaplyIProWidth4Bits:
                imageUpdater.slicesPerImage = 8;
                break;
            case MaplyIProWidth8Bits:
                imageUpdater.slicesPerImage = 4;
                break;
            case MaplyIProWidthWhole:
                imageUpdater.slicesPerImage = 0;
                break;
            default:
                Log.d("IPro","QuadImageLayer: Unsupported source width for Shader.");
                break;
        }
        switch (temporalInterpolate)
        {
            case MaplyIProTemporalNearest:
                imageUpdater.cubicTemporal = false;
                break;
            case MaplyIProTemporalLinear:
                imageUpdater.cubicTemporal = false;
                break;
            case MaplyIProTemporalCubic:
                imageUpdater.cubicTemporal = true;
                break;
        }
        imageUpdater.debugMode = false;
        imageUpdater.imageDepth = getImageDepth();

        maplyControl.addActiveObject(imageUpdater);
        imageUpdater.debugMode = debugMode;
    }

    /**
     * Called by the layer thread.  Don't call this directly.
     */
    public void shutdown()
    {
        valid = false;
        if (layerThread != null)
            layerThread.removeWatcher(this);
        cancelEvalStep();
        ChangeSet changes = new ChangeSet();
        nativeShutdown(changes);
        if (layerThread != null && layerThread.scene != null)
            changes.process(layerThread.scene);
        super.shutdown();

        dispose();
    }

    /**
     * The view updated.  Called by the layer thread.  Don't call this directly.
     */
    @Override
    public void viewUpdated(ViewState viewState)
    {
        if (!valid)
            return;

        nativeViewUpdate(viewState);

        scheduleEvalStep();
    }

    Handler evalStepHandle = null;
    Runnable evalStepRun = null;

    // Cancel the current evalStep
    void cancelEvalStep()
    {
        if (!valid)
            return;

        synchronized(this)
        {
            if (evalStepHandle != null)
            {
                evalStepHandle.removeCallbacks(evalStepRun);
                evalStepHandle = null;
                evalStepRun = null;
            }
        }
    }

    // Post an evalStep if there isn't one scheduled
    void scheduleEvalStep()
    {
        if (!valid)
            return;
//		cancelEvalStep();

        synchronized(this)
        {
            if (evalStepHandle == null)
            {
                evalStepRun = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (valid)
                            evalStep();
                    }
                };
                evalStepHandle = layerThread.addTask(evalStepRun,true);
            }
        }
    }

    // Do something small and then return
    void evalStep()
    {
        if (!valid)
            return;

        synchronized(this)
        {
            evalStepHandle = null;
            evalStepRun = null;
        }

        // Note: There's a scene timing problem
        // Sometimes this hasn't been fully set up
        if (!rampTexSetup && rampImage != null) {
            setRampImage(rampImage);
        }

        // Note: Check that the renderer is set up and such.
        ChangeSet changes = new ChangeSet();
        boolean didSomething = nativeEvalStep(changes);
        layerThread.addChanges(changes);
        if (didSomething)
            scheduleEvalStep();
    }

    /**
     * If you call this, the layer will clear out all current geometry
     * and refetch everything.
     */
    public void refresh()
    {
        if (!valid)
            return;

        // Make sure this runs on the layer thread
        if (Looper.myLooper() != layerThread.getLooper())
        {
            Handler handle = new Handler();
            handle.post(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            refresh();
                        }
                    });
            return;
        }

        ChangeSet changes = new ChangeSet();
        boolean doEvalStep = nativeRefresh(changes);
        layerThread.addChanges(changes);
        if (doEvalStep)
            scheduleEvalStep();
    }

    /* Called by the JNI side.  We need to start fetching
     * the given tile.
     */
    void startFetch(int level,int x,int y,final int frame)
    {
        if (!valid)
            return;

        // If we're not going OSM style addressing, we need to flip the Y back to TMS
        if (!flipY)
            y = (1<<level)-y-1;

        final MaplyTileID tileID = new MaplyTileID(x,y,level);

        // Fake loading for tiles less than the minZoom
        if (level < tileSource.minZoom())
        {
            layerThread.addTask(new Runnable()
            {
                @Override
                public void run()
                {
                    loadedTile(tileID,frame,null);
                }
            },true);
        } else
            tileSource.startFetchForTile(this, tileID, frame);
    }

	/*
	 * Called by the JNI side.  We're being woken up
	 */

    /**
     * When a tile source finishes loading a given image tile,
     * it calls this method to let the quad image tile layer know
     * about it.  You can call this on any thread.
     *
     * @param imageTile The image tile we've just loaded.  Pass in null on failure.
     */
    public void loadedTile(final MaplyTileID tileID,final int frame,final MaplyImageTile imageTile)
    {
        if (!valid)
            return;

        int y = tileID.y;
        if (!flipY)
            y =  (1<<tileID.level)-tileID.y-1;

        if (Looper.myLooper() != layerThread.getLooper())
        {
            layerThread.addTask(new Runnable()
            {
                @Override
                public void run()
                {
                    loadedTile(tileID,frame,imageTile);
//					Log.d("Maply","Responding to load for tile: " + tileID.level + ": (" + tileID.x + "," + tileID.y);
                }
            });
            return;
        }

        ChangeSet changes = new ChangeSet();
        if (imageTile != null) {
            if (imageTile.bitmaps != null)
                nativeTileDidLoad(tileID.x, y, tileID.level, -1, imageTile.bitmaps, changes);
            else
                nativeTileDidLoad(tileID.x, y, tileID.level, frame, imageTile.bitmap, changes);
        } else
            nativeTileDidNotLoad(tileID.x,y,tileID.level,frame,changes);
        layerThread.addChanges(changes);
    }

    public LayerThread getLayerThread()
    {
        return layerThread;
    }

    boolean startEnable = true;

    /** Enable/Disable the whole layer.
     *	By default this is on.  If you turn it off, there may be a slight delay before the whole layer disappears.  The layer will keep working, but any geometry will be invisible until you turn it back on.
     */
    public void setEnable(boolean enable)
    {
        if (layerThread == null)
            return;

        ChangeSet changes = new ChangeSet();
        setEnable(enable,changes);
        layerThread.addChanges(changes);
        startEnable = enable;
    }

    native void setEnable(boolean enable,ChangeSet changes);

    /**
     * Is the layer enabled.
     */
    public boolean getEnable()
    {
        return startEnable;
    }

    /**
     * Set the draw priority for the whole quad image layer.
     */
    public native void setDrawPriority(int drawPriority);

//	public native void setFade(float fade);

    /** The number of images we're expecting to get per tile.
     * This is the number of images the layer will ask for per tile.  The default is 1, which is the normal case.  If this is greater than one that typically means we're going to animate between them.
     * the MaplyTileSource delegate is always expected to provide this many imates.
     */
    public native void setImageDepth(int imageDepth);

    /**
     * The number of images expected per tile. These are often used as animation frames.
     */
    public native int getImageDepth();

    /**
     * Get the current image being displayed.  Only really makes sense for animated layers.
     * @return The current image index (or between) being displayed.
     */
    public native float getCurrentImage();

    /**
     * Set the current image while short circuiting all the various other parts of the system.
     * Only used internally.
     */
    public native void setCurrentImageSimple(float where);

    int lastPriority = -1;

    /** Set the current image we're displaying.
     * This sets the current image being displayed, and interpolates between it and the next image.  If set to an integer value, you'll get just that image.  If set to a value between integers, you'll get a blend of the two.
     * This is incompatible with setting an animationPeriod.  Do just one or the other.
     */
    public void setCurrentImage(final float current,boolean updatePriorities)
    {
        int prior[] = null;
        if (updatePriorities)
        {
            int curPriority = (int)current;
            if (curPriority != lastPriority) {
                prior = new int[this.getImageDepth()];

                int start = curPriority;
                prior[0] = start;
                int where = 1;
                for (int ii = 1; ii < prior.length; ii++) {
                    int up = start + ii;
                    int down = start - ii;
                    if (up < prior.length)
                        prior[where++] = up;
                    if (down >= 0)
                        prior[where++] = down;
                }

                lastPriority = curPriority;
            }
        }

        if (layerThread != null) {

            if (prior != null) {
                final int priorFinal[] = prior;
                layerThread.addTask(new Runnable() {
                    @Override
                    public void run() {
                        ChangeSet changes = new ChangeSet();

                        setFrameLoadingPriority(priorFinal, changes);

                        layerThread.addChanges(changes);
                    }
                });
            }
            ChangeSet changes = new ChangeSet();

            setCurrentImages((int)current,(int)(current+1), changes);

            if (layerThread.scene != null)
                changes.process(layerThread.scene);
        } else {
            currentImageSetBeforeStart = true;
            currentImageValue = current;
            currentPriorities = prior;
        }

        maplyControl.requestRender();
    }

    native public void setCurrentImages(int image0,int image1,ChangeSet changes);

    /**
     * If set, we'll use this as the maximum current image value when animating.
     */
    public native void setMaxCurrentImage(float maxCurrent);

    /**
     * Return the maximum current image value possible when animing.
     */
    public native float getMaxCurrentImage();

    ImageUpdater imageUpdater = null;

    double animationPeriod = 0.0;

    /** The length of time we'll take to switch through all available images (per tile).
     * If set to non-zero right after layer creation we'll run through all the available images (in each tile) over the given period.  This only makes sense if you've got more than one image per tile.
     * If you want tighter control use the currentImage property and set your own timer.
     */
    public void setAnimationPeriod(double period) {
        animationPeriod = period;

        if (imageUpdater != null)
            imageUpdater.period = animationPeriod;
    }

    /**
     * Get the current animation period.
     */
    public double getAnimationPeriod()
    {
        return animationPeriod;
    }

    boolean animationWrap = false;

    /** If set to true, we'll consider the list of images for each tile to be circular when we animate.
     * When set we'll loop back to the first image when we go past the last.  This is the default.
     * When not set, we'll run from 0 to maxCurrentImage and then restart.
     */
    public void setAnimationWrap(boolean wrap)
    {
        animationWrap = wrap;
    }

    /**
     * If set, the animation will wrap around when displaying.
     */
    boolean getAnimationWrap() { return animationWrap; }

    /** If set, we'll try to fetch frames individually.
     * When fetching from a data source that has multiple frames we'll fetch each frame individually and allow them to display as we go.
     * If this is false, we'll force all the frames to load for a given tile before we move on to the next tile.
     */
    public native void setAllowFrameLoading(boolean frameLoading);

    /**
     * Information about the frame status
     */
    static public class FrameStatus
    {
        FrameStatus(int depth)
        {
            complete = new boolean[depth];
            tilesLoaded = new int[depth];
        }

        @Override public boolean equals(Object thatObj)
        {
            if (thatObj == null)
                return false;

            FrameStatus that = (FrameStatus)thatObj;
            if (currentFrame != that.currentFrame)
                return false;
            if (complete.length != that.complete.length)
                return false;
            for (int ii=0;ii<complete.length;ii++)
                if (complete[ii] != that.complete[ii])
                    return false;
            if (tilesLoaded.length != that.tilesLoaded.length)
                return false;
            for (int ii=0;ii<tilesLoaded.length;ii++)
                if (tilesLoaded[ii] != that.tilesLoaded[ii])
                    return false;

            return true;
        }

        public int currentFrame;
        public boolean complete[];
        public int tilesLoaded[];
    };

    /**
     * Query the status for active frames.  This asks the quad image layer what the state of
     * frame loading is at this instant.  All arrays are imageDepth in size.
     * @param complete For each frame, whether or not it's completely loaded.
     * @param tilesLoaded For each frame, how many tiles are loaded.
     * @return The frame currently beng loaded.  Returns -1 if the call was invalid.
     */
    public FrameStatus getFrameStatus()
    {
        if (getImageDepth() <= 1)
            return null;

        FrameStatus status = new FrameStatus(getImageDepth());
        status.currentFrame = getFrameStatusNative(status.complete,status.tilesLoaded);
        if (status.currentFrame == -1)
            return null;

        return status;
    }

    private native int getFrameStatusNative(boolean complete[],int tilesLoaded[]);

    /** For the case where we're loading individual frames, this sets the order to load them in.
     * When doing animation and loading frames, we have the option of loading them one by one.  Normally we start from 0 and work our way up, but you can control that order here.
     */
    public void setFrameLoadingPriority(int[] priorites)
    {
        ChangeSet changes = new ChangeSet();
        setFrameLoadingPriority(priorites,changes);
        layerThread.addChanges(changes);

        scheduleEvalStep();
    }

    native void setFrameLoadingPriority(int[] priorites,ChangeSet changes);

    /**
     * Set the Color for the tile geometry from a standard Android Color value.
     * @param color Color value, including alpha.
     */
    public void setColor(int color)
    {
        setColor(Color.red(color)/255.f,Color.green(color)/255.f,Color.blue(color)/255.f,Color.alpha(color)/255.f);
    }

    /** Color for the tile geometry.
     * The geometry we create for tiles has an RGBA color.  It's white/full alpha by default, but you can set it here.  You might want to do this if you'd like a semi-transparent layer, sort of a shader of course, where you can do whatever you like.
     */
    public void setColor(float r,float g,float b,float a)
    {
        ChangeSet changes = new ChangeSet();
        setColor(r,g,b,a,changes);
        if (layerThread != null)
            layerThread.addChanges(changes);
    }

    native void setColor(float r,float g,float b,float a,ChangeSet changes);

    /** Maximum number of tiles to load in at once.
     * This is the maximum number of tiles the pager will have loaded into memory at once.  The default is 128 and that's generally good enough.  However, if your tile size is small, you may want to load in more.
     * Tile loading can get out of control when using elevation data.  The toolkit calculates potential screen coverage for each tile so elevation data makes all tiles more important.  As a result the system will happily page in way more data than you may want.  The limit becomes important in elevation mode, so leave it at 128 unless you need to change it.
     */
    public native void setMaxTiles(int maxTiles);

    /** Tinker with the importance for tiles.  This will cause more or fewer tiles to load
     * The system calculates an importance for each tile based on its size and location on the screen.  You can mess with those values here.
     * Any value less than 1.0 will make the tiles less important.  Any value greater than 1.0 will make tiles more important.
     */
    public native void setImportanceScale(float scale);

    int textureAtlasSize = 2048;
    /** Set the (power of two) size of texture atlases the layer will create.
     * The system makes extensive use of texture atlases for rendering tiles.  Typically we'll only have one or two gigantic textures will all our imagery and a handfull of drawables.  This is what makes the system fast.  Very fast.
     * This option controls the size of those texture atlases.  It's set to 2048 by default (2048x2048 texels).  If you're going to change it, set it to 1024, but don't go any lower unless you know something we don't.  It must always be a power of 2.
     */
    public void setTextureAtlasSize(int size)
    {
        textureAtlasSize = size;
        setTextureAtlasSizeNative(size);
    }

    native void setTextureAtlasSizeNative(int size);

    /**
     * Enumerated values for image types.
     */
    public enum ImageFormat {
        MaplyIProImage4Layer8Bit,
        MaplyIProImageUShort565,
        MaplyIProImageUShort4444,
        MaplyIProImageUShort5551,
        MaplyIProImageByte
        };

    /**
     * Returns the number of border texels used around images.
     */
    public native int getBorderTexel();

    /** Number of border texels to set up around image tiles.
     For matching image tiles along borders in 3D (probably the globe) we resample the image slightly smaller than we get and make up a boundary around the outside.  This number controls that border size.
     By default this is 1.  It's safe to set it to 0 for 2D maps and some overalys.
     */
    public native void setBorderTexel(int borderTexel);

    /** Control how tiles are indexed, either from the lower left or the upper left.
     * If set, we'll use the OSM approach (also Google Maps) to y indexing.  That's that default and it's normally what you're run into.
     * Strictly speaking, TMS addressing (the standard) is flipped the other way.  So if you're tile source looks odd, try setting this to false.
     * Default value is true.
     */
    public void setFlipY(boolean inFlipY)
    {
        flipY = inFlipY;
    }

    /** Detail the levels you want loaded in target level mode.
     * The image display can work in one of two modes, quad tree where it loads everything starting from the min level or a target level mode where it just tries to load one or more target levels.  This is the array that controls which levels it will try to load.
     * We do this so that the user doesn't have to wait for the target level to load.  This can be distracting on large displays with small tiles.  If you use this mode, the layer will load lower levels first, filling in quicker and then load the target level.  This looks much better, but doesn't take as long as the full quad tree based loading.
     * The layer calculates the optimal target level (for 2D maps, if you're in that mode).  The entries in this array are relative to that level or absolute.  For example [0,-4,-2] means the layer will always try to load levels 0, targetLevel-4 and targetLevel-2, but only the latter two if they make sense.
     */
    public native void setMultiLevelLoads(int[] levels);

    /**
     * Calculate the current target zoom level and return it.
     */
    public native int getTargetZoomLevel();

    /** Force a full reload of all tiles.
     * This will notify the system to flush out all the existing tiles and start reloading from the top.  If everything is cached locally (and the MaplyTileSource objects say so) then this should appear instantly.  If something needs to be fetched or it's taking too long, you'll see these page in from the low to the high level.
     * This is good for tile sources, like weather, that need to be refreshed every so often.
     */
    public void reload()
    {
        ChangeSet changes = new ChangeSet();
        reload(changes);
        layerThread.addChanges(changes);
    }

    native void reload(ChangeSet changes);

    /**
     * We can only have a certain number of fetches going at once.
     * We'll create this number of threads (in some cases) based
     * on this number.
     */
    public native void setSimultaneousFetches(int numFetches);

    /**
     * If set we'll calculate a single target zoom level for the whole
     * viewport, rather than evaluating tiles individually.  This works
     * for 2D maps, but not for 3D maps or globes.
     */
    public native void setUseTargetZoomLevel(boolean newVal);

    /**
     * If set we'll skip the lower levels of the pyramid and load only
     * the current target zoom level.
     */
    public native void setSingleLevelLoading(boolean newVal);

    /**
     * If set, we'll generate edges between the map tiles to deal with
     * diverged tile zoom levels loaded at once.
     */
    public native void setHandleEdges(boolean newVal);

    /**
     * If set, we'll provide geometry for the north and south poles.
     */
    public native void setCoverPoles(boolean newVal);

    /**
     * By default the quad layer is always visible.  If you set these
     * then the layer will only be visible in the given range.
     * @param minVis The close zoom range at which to drop out the layer.  0.0 by default.
     * @param maxVis The far zoom range at which to drop out the layer.
     * Something implausibly large by default.
     */
    public native void setVisibility(double minVis,double maxVis);

    // Note: New functions

    /**
     * Set up the internal image format of the quad images layer.
     * <br>
     * The internal image format is how OpenGL ES is storing the raw data we're using for rendering.
     * If you're doing indexed or stacked data this is probably MaplyIProImage4Layer8Bit.
     * If you're doing grayscale then MaplyIProImageByte.
     * The other modes are optimized color options.  565 is good for color, 4444 if you don't really know, and 5551 if you have very simple alpha.
     */
    public void setInternalImageFormat(MaplyIProInternalImageFormat internalImageFormat)
    {
        setInternalImageFormatNative(internalImageFormat.ordinal());
    }
    private native void setInternalImageFormatNative(int iVal);

    ImageSourceLayout sourceLayout = null;

    /** The source layout controls how we treat the input data.
     * <br>
     * The source layout object tells the system how to treat the input data and decode it.  Consult MaplyIProImageSourceLayout for details.
     */
    public void setSourceLayout(ImageSourceLayout inLayout)
    {
        sourceLayout = inLayout;
    }

    /**
     * Return image layout information.
     */
    public ImageSourceLayout getSourceLayout() { return sourceLayout; }

    /**
     * Fill in the interface for the position feedback and you can set the position manually every frame.
     */
    public interface ImagePositionFeedback
    {
        /** The callback for position (or time) update.
         * <br>
         * Fill this in to get notified when the quad images layer changes the position with the stack of images.
         * @param layer The quad images layer in question.
         * @param minPos Minimum overall position value for comparison (usually 0.0)
         * @param maxPos Maximum overall position for comparison (usually imageDepth).
         * @param currentPos Current position within the image stack.
         */
        void positionForImagesLayer(IProQuadImageTileLayer layer, double minPos, double maxPos, double currentPos);
    }

    /**
     * Fill in the interface for the position feedback and you can get the position every so
     */
    public ImagePositionFeedback positionFeedbackDelegate = null;

    public enum MaplyIProPosFeedback {
        MaplyIProPosFeedbackContinuous,
        MaplyIProPosFeedbackPeriodic
    };

    public MaplyIProPosFeedback positionFeedbackType;
    public double positionUpdatePeriod = 1.0;

    /**
     * Fill this in to get periodic or continuous position updates.
     * @param feedbackType Continuous or periodic position updates.
     * @param updatePeriod How often to update.  Only used if the feedback is periodic.
     * @param newDelegate Object to call on position update.
     */
    public void setImagePositionFeedback(MaplyIProPosFeedback feedbackType,double updatePeriod,ImagePositionFeedback newDelegate)
    {
        positionFeedbackType = feedbackType;
        positionUpdatePeriod = updatePeriod;
        positionFeedbackDelegate = newDelegate;
    }

    /**  Used to modify the position within the image stack.
     * <br>
     * You can set currentImage directly or you can let the quad images layer animation on its own.  But if you really want to, you can just mess with the position directly each frame.
     * If you want to do that you fill in this delegate and return a position between minPos and maxPos.
     */
    public interface ImagePositionSetter
    {
        /** Return a position to display within the image stack.
         * @param layer The quad images layer in question.
         * @param minPos Minimum overall position value for comparison (usually 0.0)
         * @param maxPos Maximum overall position for comparison (usually imageDepth).
         */
        double calculatePositionForImagesLayer(IProQuadImageTileLayer layer,double minPos,double maxPos);
    }

    /**
     * Fill this in to set the position every frame (if desired).
     */
    public ImagePositionSetter positionSettingDelegate = null;

    /** The OpenGL ES program used to draw the quad images layer.
     * <br>
     * This will be generated internally by the quad images layer unless you override it.
     * If you do override it, be sure to also look at the MaplyIProQuadImagesUpdater which sets the various uniforms.
     */
    public void setShader(Shader shader)
    {
        if (rampImage != null)
            setRampImage(rampImage);
        setShaderNative(shader.getID());
    }
    private native void setShaderNative(long shaderID);

    // Shader is either generated or passed in
    protected Shader shader = null;
    protected Bitmap rampImage;
    MaplyTexture rampTex = null;
    boolean rampTexSetup = false;

    /** For indexed data sets the image we'll index into.
     * <br>
     * The ramp image is used by indexed data sets to generate colors.
     * For 8 bit lookup you'll need a ramp image of at least 256x1.
     * Put your various colors in each of those pixels or use the MaplyColorRampGenerator to generate an image.
     */
    public void setRampImage(Bitmap inRampImage)
    {
        rampImage = inRampImage;

        // Set up a MaplyTexture
        if (shader != null)
        {
            if (rampTex == null)
                rampTex = maplyControl.addTexture(rampImage,new MaplyBaseController.TextureSettings(), MaplyBaseController.ThreadMode.ThreadCurrent);
            rampTexSetup = shader.addTexture("s_colorRamp",rampTex);
        }
    }

    MaplyIProTemporalInterpolation temporalInterpolate = MaplyIProTemporalInterpolation.MaplyIProTemporalLinear;
    MaplyIProSpatialInterpolation spatialInterpolate = MaplyIProSpatialInterpolation.MaplyIProSpatialBilinear;

    /** Controls how the layer displays things in time (position).
     * <br>
     * There are a variety of interpolation modes among the image stack (think time).  None are terribly expensive on the device since they're just interpolation among layers.
     */
    public void setTemporalInterpolate(MaplyIProTemporalInterpolation inVal)
    {
        temporalInterpolate = inVal;
    }

    /** Controls how the layer displays things in space.
     * The quad images layer can let the normal bilinear interpolation that OpenGL ES provides happen or it can override that with a more expensive bicubic interpolation.
     */
    public void setSpatialInterpolate(MaplyIProSpatialInterpolation inVal)
    {
        spatialInterpolate = inVal;
    }

    /**
     * Figure out the total number of frames, taking the input images, slices per image, and slices in the last image into account.
     */
    public int calcNumberOfFrames()
    {
        if (getImageDepth() < 1)
            return 0;

        return sourceLayout.slicesPerImage * (getImageDepth() - 1) + sourceLayout.slicesInLastImage;
    }

    /**
     * Returns the width of a single frame (slice) in units that currentImage uses.
     */
    public float widthPerFrame()
    {
        if (sourceLayout.slicesPerImage <= 1)
            return 1.f;

        return 1.f/sourceLayout.slicesPerImage;
    }


    boolean debugMode = true;
    /**
     * Turn on a lot of output.
     */
    public void setDebugMode(boolean inDebugMode)
    {
        debugMode = inDebugMode;
        if (imageUpdater != null)
            imageUpdater.debugMode = debugMode;
    }

    native void nativeShutdown(ChangeSet changes);

    static
    {
        nativeInit();
    }
    private static native void nativeInit();
    native void initialise(CoordSystem coordSys,ChangeSet changes);
    native void dispose();
    private long nativeHandle;

    native void nativeStartLayer(Scene scene,MaplyRenderer renderer,Point2d ll,Point2d ur,int minZoom,int maxZoom,int pixelsPerSide);
    native void nativeViewUpdate(ViewState viewState);
    native boolean nativeEvalStep(ChangeSet changes);
    native boolean nativeRefresh(ChangeSet changes);
    native void nativeTileDidLoad(int x,int y,int level,int frame,Bitmap bitmap,ChangeSet changes);
    native void nativeTileDidLoad(int x,int y,int level,int frame,Bitmap[] bitmaps,ChangeSet changes);
    native void nativeTileDidNotLoad(int x,int y,int level,int frame,ChangeSet changes);
}
