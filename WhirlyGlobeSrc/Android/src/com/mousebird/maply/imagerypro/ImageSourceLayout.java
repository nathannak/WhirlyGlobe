package com.mousebird.maply.imagerypro;

/**
 * A description of the layout of the input images.
 * <br>
 * Fill this out to describe how your input data is structured.
 */
public class ImageSourceLayout
{
    /// Ordering of the bytes within the image
    public enum MaplyIProSourcePixelOrder {
        MaplyIProOrderRGBA,
        MaplyIProWidthABGR
    };

    /// The number of bits for indexed data
    public enum MaplyIProSourceWidth {
        MaplyIProWidth4Bits,
        MaplyIProWidth8Bits,
        MaplyIProWidthWhole
    };

    /**
     * The number of index slices we can expect in a single image.
     * <br>
     * This is the number of slices we'll find in a single image.  Should be 3 or 4.
     */
    public int slicesPerImage;

    /**
     * Set if we're dealing with indexed data.
     * <br>
     * Set this if the data sources is indexed lookup data rather in color.
     */
    public boolean indexed;

    /**
     * The size of a single slices in the image.
     * <br>
     * Slices within an image can be 8 bits or the whole image.
     */
    public MaplyIProSourceWidth sourceWidth;

    /**
     * Layout of the bytes within the pixel.
     * <br>
     * The image pixels can be a laid out in a variety of ways.
     */
    public MaplyIProSourcePixelOrder pixelOrder;

    /**
     * Number of slices in the last image source in the list.
     * <br>
     * The number of time slices doesn't always neatly divide into 4 or 8.  You may be left with empty ones.  Set this to deal with that case.
     */
    public int slicesInLastImage;
}
