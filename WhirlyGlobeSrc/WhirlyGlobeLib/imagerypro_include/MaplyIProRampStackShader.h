/*
 *  MaplyIProRampStackShader.h
 *  WGMaply-ImageryPro
 *
 *  Created by Steve Gifford on 8/19/16.
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

#import <string>
#import "MaplyIProShaderGenerator.h"

namespace MaplyIPro
{

/** @brief The Ramp Stack Shader Generator assembles the shader from its component pieces.
    @details The Ramp Stack Shader Generator puts the pieces together for a
    shader that can interpret indexed values (or not) uses stacked images (or not)
    and has a variety of interpolation.
  */
class RampStackShaderGenerator : public ShaderGenerator
{
public:
    typedef enum {IndexOn,IndexOff} IndexType;
    typedef enum {Stack4Bits,Stack8Bits,StackWhole} StackType;
    typedef enum {TexelByte,TexelWhole} TexelInputType;
    typedef enum {OrderRGBA,OrderABGR} PixelByteOrderType;
    typedef enum {CubicInterpTemporal,LinearInterpTemporal,NearestInterpTemporal} TemporalInterpType;
    typedef enum {BicubicInterpSpatial,BilinearInterpSpatial} SpatialInterpType;
    
    RampStackShaderGenerator();
    virtual ~RampStackShaderGenerator();

    /// @brief Texel input type (8 bits or whole)
    void setTexelInputType(TexelInputType texelType);
    
    /// @brief Indexing or not
    void setIndexType(IndexType indexType);

    /// @brief The order of bytes in the pixel
    void setPixelByteOrder(PixelByteOrderType orderType);

    /// @brief 8 bit or 4 bit index values in the stacked image
    void setStackType(StackType stackType);
    
    /// @brief Number of slices stacked into each image
    void setStackCount(int stackCount);
    
    /// @brief Use bicubic or bilinear interpolation between frames
    void setTemporalInterpolation(TemporalInterpType tempType);

    /// @brief Use bicubic or blinear interpolation for the texels
    void setSpatialInterpolation(SpatialInterpType spatialType);

    /// @brief Build the shaders we'll actually use
    bool generateShaders(std::string &vertShader,std::string &fragShader,std::string &error);
    
    /// @brief Set up the various functions and parts.  Called by generateShaders() if it hasn't been already.
    bool setupParts();

protected:
    TexelInputType texelType;
    IndexType indexType;
    StackType stackType;
    PixelByteOrderType pixelOrderType;
    int stackCount;
    TemporalInterpType timeInterpType;
    SpatialInterpType spatialInterpType;
    
    std::string partsError;
    bool partsInit;
};
    
/** @brief Generate the parameters for cubic interpolation
    @details We use cubic and bicubic interpolation in a variety of places.  This version runs on the CPU and uses the exact interpolation we use in shaders.
  */
extern void CubicInterpolationVec(double t,double &x,double &y,double &z,double &w);
    
}
