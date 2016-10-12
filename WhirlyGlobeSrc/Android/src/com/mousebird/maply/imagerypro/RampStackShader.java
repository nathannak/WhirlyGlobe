package com.mousebird.maply.imagerypro;

/**
 * The Ramp Stack Shader Generator assembles the shader from its component pieces.
 * <br>
 * The Ramp Stack Shader Generator puts the pieces together for a
 * shader that can interpret indexed values (or not) uses stacked images (or not)
 * and has a variety of interpolation.
 */
public class RampStackShader extends ShaderGenerator
{
    public enum IndexType {IndexOn,IndexOff};
    public enum StackType {Stack4Bits,Stack8Bits,StackWhole};
    public enum TexelInputType {TexelByte,TexelWhole};
    public enum PixelByteOrderType {OrderRGBA,OrderABGR};
    public enum TemporalInterpType {CubicInterpTemporal,LinearInterpTemporal,NearestInterpTemporal};
    public enum SpatialInterpType {BicubicInterpSpatial,BilinearInterpSpatial};

    RampStackShader()
    {
        initialise();
    }

    /// @brief Texel input type (8 bits or whole)
    public native void setTexelInputType(int texelType);

    /// @brief Indexing or not
    public native void setIndexType(int indexType);

    /// @brief The order of bytes in the pixel
    public native void setPixelByteOrder(int orderType);

    /// @brief 8 bit or 4 bit index values in the stacked image
    public native void setStackType(int stackType);

    /// @brief Number of slices stacked into each image
    public native void setStackCount(int stackCount);

    /// @brief Use bicubic or bilinear interpolation between frames
    public native void setTemporalInterpolation(int tempType);

    /// @brief Use bicubic or blinear interpolation for the texels
    public native void setSpatialInterpolation(int spatialType);

    public void finalize()
    {
        dispose();
    }

    static
    {
        nativeInit();
    }
    private static native void nativeInit();
    native void initialise();
    native void dispose();
}
