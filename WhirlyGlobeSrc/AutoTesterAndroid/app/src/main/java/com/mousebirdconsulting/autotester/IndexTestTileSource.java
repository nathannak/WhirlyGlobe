package com.mousebirdconsulting.autotester;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.mousebird.maply.LayerThread;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.MaplyImageTile;
import com.mousebird.maply.MaplyTileID;
import com.mousebird.maply.QuadImageTileLayerInterface;
import com.mousebird.maply.imagerypro.QuadImageTileLayer;
import com.mousebird.maply.imagerypro.RampStackShader;

/**
 * Created by sjg on 10/15/16.
 */
public class IndexTestTileSource implements QuadImageTileLayer.TileSource
{
    MaplyBaseController maplyControl = null;
    int minZoom,maxZoom;
    RampStackShader.IndexType indexType;
    RampStackShader.StackType stackType;
    int pixelsPerSide = 128;

    public IndexTestTileSource(MaplyBaseController inMaplyControl,int minLevel, int maxLevel, boolean inIndexed, int stackDepth)
    {
        maplyControl = inMaplyControl;
        minZoom = minLevel;
        maxZoom = maxLevel;
        indexType = inIndexed ? RampStackShader.IndexType.IndexOn : RampStackShader.IndexType.IndexOff;
        switch (stackDepth)
        {
            case 0:
                stackType = RampStackShader.StackType.StackWhole;
                break;
            case 4:
                stackType = RampStackShader.StackType.Stack4Bits;
                break;
            case 8:
                stackType = RampStackShader.StackType.Stack8Bits;
                break;
            default:
                Log.e("IPro","Unsupported stack depth of " + stackDepth + " for TestTileSource");
                break;
        }
    }


    public int minZoom()
    {
        return minZoom;
    }

    public int maxZoom()
    {
        return maxZoom;
    }

    public int pixelsPerSide()
    {
        return pixelsPerSide;
    }

    public void startFetchForTile(final QuadImageTileLayerInterface layer, final MaplyTileID tileID, final int frame)
    {
        LayerThread ourLayerThead = maplyControl.getWorkingThread();

        ourLayerThead.addTask(new Runnable() {
            @Override
            public void run() {
                MaplyImageTile imageTile = null;

                switch (stackType)
                {
                    case Stack8Bits: {
                        Bitmap allSlices[] = new Bitmap[4];
                        for (int ii = 0; ii < 4; ii++)
                            allSlices[ii] = generateImageSlice(tileID, 4 * frame + ii);
                        Bitmap stackedSlices = formStackedImage(allSlices);
                        imageTile = new MaplyImageTile(stackedSlices);
                    }
                    break;
                    case Stack4Bits: {
                        Bitmap allSlices[] = new Bitmap[8];
                        for (int ii = 0; ii < 8; ii++)
                            allSlices[ii] = generateImageSlice(tileID, 8 * frame + ii);
                        Bitmap stackedSlices = formStackedImage(allSlices);
                        imageTile = new MaplyImageTile(stackedSlices);
                    }
                    break;
                    default:
                        // Stack the same image multiple times
                        Bitmap allSlices[] = new Bitmap[4];
                        allSlices[0] = generateImageSlice(tileID, frame);
                        for (int ii=1;ii<4;ii++)
                            allSlices[ii] = allSlices[0];
                        Bitmap stackedSlices = formStackedImage(allSlices);
                        imageTile = new MaplyImageTile(stackedSlices);
                        break;
                }

                Log.d("IPro","IndexTestTileSource loaded image for tile " + tileID.level + ": (" + tileID.x + "," + tileID.y + ") frame = " + frame);
                layer.loadedTile(tileID, frame, imageTile);
            }
        });
    }

    public Bitmap formStackedImage(Bitmap[] slices)
    {
        int numSlices = slices.length;

        if (numSlices != 4 && numSlices != 8)
            return null;

        Bitmap outData = Bitmap.createBitmap(pixelsPerSide, pixelsPerSide, Bitmap.Config.ARGB_8888);
        // Work through the pixels, forming as we go
        for (int y=0;y<pixelsPerSide;y++) {
            for (int x = 0; x < pixelsPerSide; x++) {
                int pixel = 0;
                switch (numSlices) {
                    case 4: {
                        int pixA = slices[0].getPixel(x, y);
                        int pixB = slices[1].getPixel(x, y);
                        int pixC = slices[2].getPixel(x, y);
                        int pixD = slices[3].getPixel(x, y);
                        pixel = pixD << 24 | pixC << 16 | pixB << 8 | pixA;
                    }
                    break;
                    case 8: {
                        int pixA = slices[0].getPixel(x, y);
                        int pixB = slices[1].getPixel(x, y);
                        int pixC = slices[2].getPixel(x, y);
                        int pixD = slices[3].getPixel(x, y);
                        int pixE = slices[4].getPixel(x, y);
                        int pixF = slices[5].getPixel(x, y);
                        int pixG = slices[6].getPixel(x, y);
                        int pixH = slices[7].getPixel(x, y);
                        pixel = pixH << 28 | pixG << 24 | pixF << 20 | pixE << 16 | pixD << 12 | pixC << 8 | pixB << 4 | pixA;
                    }
                    break;
                }

                outData.setPixel(x, y, pixel);
            }
        }

        return outData;
    }

    public Bitmap generateImageSlice(final MaplyTileID tileID, final int frame)
    {
        // Render the tile ID into a bitmap
        String text = null;
        if (frame == -1)
            text = tileID.toString();
        else
            text = tileID.toString() + " " + frame;
        Paint p = new Paint();
        p.setTextSize(24.f);
        p.setColor(Color.WHITE);
        Rect bounds = new Rect();
        p.getTextBounds(text, 0, text.length(), bounds);
        int textLen = bounds.right;
//			int textHeight = -bounds.top;

        // Draw into a bitmap
        int sizeX = pixelsPerSide,sizeY = pixelsPerSide;
        Bitmap bitmap = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        Paint p2 = new Paint();
        p2.setStyle(Paint.Style.FILL);
        p2.setARGB(0, 0, 0, 0);
        c.drawRect(0, 0, sizeX, sizeY, p2);
        Paint p3 = new Paint();
        p3.setStyle(Paint.Style.STROKE);
        p3.setColor(Color.WHITE);
        c.drawRect(2,2,sizeX-2,sizeY-2,p3);
        c.drawText(text, (sizeX - textLen) / 2.f, sizeY / 2.f, p);

        return bitmap;
    }
}
