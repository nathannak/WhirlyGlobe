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

#import "MaplyIProRampStackShader.h"
#import <Eigen/Eigen>

namespace MaplyIPro
{

extern void CubicInterpolationVec(double t,double &x,double &y,double &z,double &w)
{
    float t2 = t * t;
    float t3 = t2 * t;
    x = (  -t3 + 3*t2 - 3*t + 1);
    y = ( 3*t3 - 6*t2       + 4);
    z = (-3*t3 + 3*t2 + 3*t + 1);
    w = ( t3);
}

RampStackShaderGenerator::RampStackShaderGenerator()
    : indexType(IndexOff), texelType(TexelWhole), stackType(StackWhole), pixelOrderType(OrderRGBA), stackCount(0), timeInterpType(LinearInterpTemporal), spatialInterpType(BilinearInterpSpatial),
    partsInit(false)
{
}

RampStackShaderGenerator::~RampStackShaderGenerator()
{
    
}
    
void RampStackShaderGenerator::setTexelInputType(TexelInputType inTexelType)
{
    texelType = inTexelType;
}
    
void RampStackShaderGenerator::setIndexType(IndexType inIndexType)
{
    indexType = inIndexType;
}
    
void RampStackShaderGenerator::setPixelByteOrder(PixelByteOrderType inOrderType)
{
    pixelOrderType = inOrderType;
}

// 8 bit or 4 bit index values in the stacked image
void RampStackShaderGenerator::setStackType(StackType inStackType)
{
    stackType = inStackType;
}

// Number of slices stacked into each image
void RampStackShaderGenerator::setStackCount(int inStackCount)
{
    stackCount = inStackCount;
}

// Use bicubic or bilinear interpolation between frames
void RampStackShaderGenerator::setTemporalInterpolation(TemporalInterpType inTempType)
{
    timeInterpType = inTempType;
}

// Use bicubic or blinear interpolation for the texels
void RampStackShaderGenerator::setSpatialInterpolation(SpatialInterpType inSpatialType)
{
    spatialInterpType = inSpatialType;
}

// Build the shaders we'll actually use
bool RampStackShaderGenerator::generateShaders(std::string &vertShader,std::string &fragShader,std::string &error)
{
    if (!partsInit)
        if (!setupParts())
        {
            error = partsError;
            return false;
        }
    
    return ShaderGenerator::generateShaders(vertShader,fragShader,error);
}

// For the cubic interpolation examples,
// credit to: http://stackoverflow.com/questions/13501081/efficient-bicubic-filtering-code-in-glsl


// Set up the various functions and parts.  Called by generateShaders() if it hasn't been already.
bool RampStackShaderGenerator::setupParts()
{
    partsInit = true;

    setGlobalPrecision(PrecisionDefault,PrecisionMedium);
    setVersion(ShaderVersion2);
    
    // Uniforms
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataMat4, "u_mvpMatrix");
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataFloat, "u_fade");
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataFloat, "u_textureAtlasSize");
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataFloat, "u_textureAtlasSizeInv");

    // Interpolation for the linear case
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataFloat, "u_interp");
    // Selection for stack images
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataInt, "u_stackIndex0");
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataInt, "u_stackIndex1");
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataInt, "u_stackIndex2");
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataInt, "u_stackIndex3");
    
    // Interpolation for two slices
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataFloat, "u_interpSlice");
    
    // Cubic interpolation for four slices
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataVec4, "u_interpCubic");
    
    // Attributes
    addGlobalVariable(PrecisionDefault, StorageQualAttribute, DataVec3, "a_position");
    addGlobalVariable(PrecisionDefault, StorageQualAttribute, DataVec2, "a_texCoord0");
    addGlobalVariable(PrecisionDefault, StorageQualAttribute, DataVec4, "a_color");
    addGlobalVariable(PrecisionDefault, StorageQualAttribute, DataVec3, "a_normal");
    
    // Varying (passed between shaders)
    addGlobalVariable(PrecisionDefault, StorageQualVarying, DataVec2, "v_texCoord0");
    addGlobalVariable(PrecisionDefault, StorageQualVarying, DataVec4, "v_color");
    
    // Samplers for fragment shader
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataSampler2D, "s_baseMap0");
    addGlobalVariable(PrecisionDefault, StorageQualUniform, DataSampler2D, "s_baseMap1");
    
    // Simple main for the vertex shader
    addFunction(VertexShader,"main",
                "void main()\n"
                "{\n"
                "   v_texCoord0 = a_texCoord0;"
                "   v_color = a_color * u_fade;"
                "\n"
                "   gl_Position = u_mvpMatrix * vec4(a_position,1.0);"
                "}\n"
                );
    
    // General use function for cubic interpolation in fragment shader
    // Note: Fix the ordering of functions in the output so we don't have to sort them manually
    addFunction(FragmentShader,"A_cubicInterpVec",
                "vec4 A_cubicInterpVec(float v)\n"
                "{\n"
                "    vec4 n = vec4(1.0, 2.0, 3.0, 4.0) - v;"
                "    vec4 s = n * n * n;"
                "    float x = s.x;"
                "    float y = s.y - 4.0 * s.x;"
                "    float z = s.z - 4.0 * s.y + 6.0 * s.x;"
                "    float w = 6.0 - x - y - z;"
                "\n"
                "    return vec4(x, y, z, w);"
                "}\n");

    bool colorIsSingle = false;
    
    switch (indexType)
    {
        case IndexOn:
        {
            addGlobalVariable(PrecisionDefault, StorageQualUniform, DataSampler2D, "s_colorRamp");
            
            switch (spatialInterpType)
            {
                case BilinearInterpSpatial:
                    addFunction(FragmentShader,"C_colorLookup",
                                "vec4 C_colorLookup(sampler2D basemap0, sampler2D basemap1, vec2 texCoord)\n"
                                "{\n"
                                "  vec4 tex0 = ##TEXTURE(basemap0, texCoord);"
                                "  vec4 tex1 = ##TEXTURE(basemap1, texCoord);"
                                "\n"
                                "  float idx = B_mixIndices(tex0,tex1);"
                                "\n"
                                "  return ##TEXTURE(s_colorRamp, vec2(idx,0.5));"
                                "}\n"
                                );
                    break;
                case BicubicInterpSpatial:
                    setGlobalPrecision(PrecisionDefault,PrecisionHigh);
                    addFunction(FragmentShader,"C_colorLookup",
                                "vec4 C_colorLookup(sampler2D basemap0, sampler2D basemap1, vec2 texCoord)\n"
                                "{\n"
                                "    texCoord *= u_textureAtlasSize;"
                                "\n"
                                "    vec2 frac = vec2(fract(texCoord.x),fract(texCoord.y));"
                                "    texCoord -= frac;"
                                "\n"
                                "    vec4 xcub = A_cubicInterpVec(frac.x);"
                                "    vec4 ycub = A_cubicInterpVec(frac.y);"
                                "\n"
                                "    vec4 c = vec4(texCoord.x - 0.5, texCoord.x + 1.5, texCoord.y - 0.5, texCoord.y + 1.5);"
                                "    vec4 s = vec4(xcub.x + xcub.y, xcub.z + xcub.w, ycub.x + ycub.y, ycub.z + ycub.w);"
                                "    vec4 offset = c + vec4(xcub.y, xcub.w, ycub.y, ycub.w) / s;"
                                "\n"
                                "    vec4 sample0[2],sample1[2],sample2[2],sample3[2];"
                                "    vec2 texScale = vec2(u_textureAtlasSizeInv,u_textureAtlasSizeInv);"
                                "    sample0[0] = ##TEXTURE(basemap0, vec2(offset.x, offset.z) * texScale);"
                                "    sample1[0] = ##TEXTURE(basemap0, vec2(offset.y, offset.z) * texScale);"
                                "    sample2[0] = ##TEXTURE(basemap0, vec2(offset.x, offset.w) * texScale);"
                                "    sample3[0] = ##TEXTURE(basemap0, vec2(offset.y, offset.w) * texScale);"
                                "    sample0[1] = ##TEXTURE(basemap1, vec2(offset.x, offset.z) * texScale);"
                                "    sample1[1] = ##TEXTURE(basemap1, vec2(offset.y, offset.z) * texScale);"
                                "    sample2[1] = ##TEXTURE(basemap1, vec2(offset.x, offset.w) * texScale);"
                                "    sample3[1] = ##TEXTURE(basemap1, vec2(offset.y, offset.w) * texScale);"
                                "\n"
                                "    float sx = s.x / (s.x + s.y);"
                                "    float sy = s.z / (s.z + s.w);"
                                "\n"
                                "    vec4 tex0 = mix(mix(sample3[0],sample2[0],sx),mix(sample1[0],sample0[0],sx),sy);"
                                "    vec4 tex1 = mix(mix(sample3[1],sample2[1],sx),mix(sample1[1],sample0[1],sx),sy);"
                                "\n"
                                "    float idx = B_mixIndices(tex0,tex1);"
                                "\n"
                                "    return ##TEXTURE(s_colorRamp, vec2(idx,0.5));"
                                "}\n"
                                );
                    break;
            }

            switch (stackType)
            {
                case Stack4Bits:
                    // Need OpenGL ES 3.0 for 4 bit versions
                    setVersion(ShaderVersion3);

                    switch (timeInterpType)
                    {
                        case NearestInterpTemporal:
                            addFunction(FragmentShader,"B_mixIndices",
                                        "float B_mixIndices(vec4 tex0,vec4 tex1)\n"
                                        "{\n"
                                        "\n"
                                        "  float pix[8];"
//                                        "  float val = tex0.##VAL0;"
//                                        "  pix[0] = (val & 0xF)/16.0;"
                                        "  pix[0] = tex0.##VAL0 * .0625;"
                                        "  pix[1] = pix[0];"
//                                        "  pix[1] = tex0.##VAL0 - pix[0] * 16.0;"
                                        "\n"
                                        "  pix[2] = tex0.##VAL1 * .0625;"
                                        "  pix[3] = pix[2];"
//                                        "  pix[3] = tex0.##VAL1 - pix[2] * 16.0;"
                                        "\n"
                                        "  pix[4] = tex0.##VAL2 * .0625;"
                                        "  pix[5] = pix[4];"
//                                        "  pix[5] = tex0.##VAL2 - pix[4] * 16.0;"
                                        "\n"
                                        "  pix[6] = tex0.##VAL3 * .0625;"
                                        "  pix[7] = pix[6];"
//                                        "  pix[7] = tex0.##VAL3 - pix[6] * 16.0;"
                                        "\n"
                                        "  float val0 = pix[u_stackIndex0];"
                                        "\n"
                                        "  return val0;"
                                        "}\n"
                                        );
                            break;
                        case LinearInterpTemporal:
                            return false;
                            break;
                        case CubicInterpTemporal:
                            return false;
                            break;
                    }
                    break;
                case Stack8Bits:
                    switch (timeInterpType)
                    {
                        case NearestInterpTemporal:
                            addFunction(FragmentShader,"B_mixIndices",
                                        "float B_mixIndices(vec4 tex0,vec4 tex1)\n"
                                        "{\n"
                                        "\n"
                                        "  float pix[4];"
                                        "  pix[0] = tex0.##VAL0;"
                                        "  pix[1] = tex0.##VAL1;"
                                        "  pix[2] = tex0.##VAL2;"
                                        "  pix[3] = tex0.##VAL3;"
                                        "\n"
                                        "  float val0 = pix[u_stackIndex0];"
                                        "\n"
                                        "  return val0;"
                                        "}\n"
                                        );
                            break;
                        case LinearInterpTemporal:
                            addFunction(FragmentShader,"B_mixIndices",
                                        "float B_mixIndices(vec4 tex0,vec4 tex1)\n"
                                        "{\n"
                                        "\n"
                                        "  float pix[8];"
                                        "  pix[0] = tex0.##VAL0;"
                                        "  pix[1] = tex0.##VAL1;"
                                        "  pix[2] = tex0.##VAL2;"
                                        "  pix[3] = tex0.##VAL3;"
                                        "  pix[4] = tex1.##VAL0;"
                                        "  pix[5] = tex1.##VAL1;"
                                        "  pix[6] = tex1.##VAL2;"
                                        "  pix[7] = tex1.##VAL3;"
                                        "\n"
                                        "  float val0 = pix[u_stackIndex0];"
                                        "  float val1 = pix[u_stackIndex1];"
                                        "\n"
                                        "  return mix(val0,val1,u_interpSlice);"
                                        "}\n"
                                        );
                            break;
                        case CubicInterpTemporal:
                            addFunction(FragmentShader,"B_mixIndices",
                                        "float B_mixIndices(vec4 tex0,vec4 tex1)\n"
                                        "{\n"
                                        "\n"
                                        "  float pix[8];"
                                        "  pix[0] = tex0.##VAL0;"
                                        "  pix[1] = tex0.##VAL1;"
                                        "  pix[2] = tex0.##VAL2;"
                                        "  pix[3] = tex0.##VAL3;"
                                        "  pix[4] = tex1.##VAL0;"
                                        "  pix[5] = tex1.##VAL1;"
                                        "  pix[6] = tex1.##VAL2;"
                                        "  pix[7] = tex1.##VAL3;"
                                        "\n"
                                        "  return (u_interpCubic.x * pix[u_stackIndex0] + u_interpCubic.y * pix[u_stackIndex1] + u_interpCubic.z * pix[u_stackIndex2] + u_interpCubic.w * pix[u_stackIndex3])/6.0;\n"
                                        "}\n"
                                        );
                            break;
                    }
                    
                    colorIsSingle = false;
                    break;
                case StackWhole:
                    addFunction(FragmentShader,"C_colorLookup",
                                "vec4 C_colorLookup(sampler2D basemap0, sampler2D basemap1, vec2 texCoord)\n"
                                "{\n"
                                "  float val0 = ##TEXTURE(basemap0, texCoord).a;"
                                "  float val1 = ##TEXTURE(basemap1, texCoord).a;"
                                "  float idx = mix(val0,val1,u_interp);"
                                "  return ##TEXTURE(s_colorRamp, vec2(idx,0.5));"
                                "}\n"
                                );
                    
                    colorIsSingle = false;
                    break;
            }
        }
            break;
        case IndexOff:
            switch (timeInterpType)
            {
                case NearestInterpTemporal:
                case LinearInterpTemporal:
                    // Fine for no index case
                    break;
                case CubicInterpTemporal:
                    partsError = "Cubic temporal interpolation not currently supported for non-indexed data sets.";
                    return false;
                    break;
            }

            switch (spatialInterpType)
            {
                case BilinearInterpSpatial:
                    // Fine for no index case
                    break;
                case BicubicInterpSpatial:
                    partsError = "Bicubic interpolation not currently supported for non-indexed data sets.";
                    break;
            }
            
            switch (texelType)
            {
                case TexelByte:
                    addFunction(FragmentShader,"C_colorLookup",
                                "float C_colorLookup(sampler2D basemap0, sampler2D basemap1, vec2 texCoord)\n"
                                "{\n"
                                "  float val0 = ##TEXTURE(basemap0, texCoord).a;"
                                "  float val1 = ##TEXTURE(basemap1, texCoord).a;"
                                "  return mix(val0,val1,u_interp);"
                                "}\n"
                                );
                    colorIsSingle = true;
                    break;
                case TexelWhole:
                    addFunction(FragmentShader,"C_colorLookup",
                                "vec4 C_colorLookup(sampler2D basemap0, sampler2D basemap1, vec2 texCoord)\n"
                                "{\n"
                                "  vec4 val0 = ##TEXTURE(basemap0, texCoord);"
                                "  vec4 val1 = ##TEXTURE(basemap1, texCoord);"
                                "  return mix(val0,val1,u_interp);"
                                "}\n"
                                );
                    colorIsSingle = false;
                    break;
            }
            break;
    }
    
    switch (stackType)
    {
        case Stack4Bits:
            break;
        case Stack8Bits:
            break;
        case StackWhole:
            break;
    }

    if (colorIsSingle)
    {
        // Fragment shader main is also pretty simple
        addFunction(FragmentShader,"main",
                    "void main()\n"
                    "{\n"
                    "  float color = C_colorLookup(s_baseMap0, s_baseMap1, v_texCoord0);"
                    "\n"
                    "  ##FRAGCOLOR = v_color * color;"
                    "}\n"
                    );
    } else {
        // Fragment shader main is also pretty simple
        addFunction(FragmentShader,"main",
                    "void main()\n"
                    "{\n"
                    "  vec4 color = C_colorLookup(s_baseMap0, s_baseMap1, v_texCoord0);"
                    "\n"
                    "  ##FRAGCOLOR = v_color * color;"
                    "}\n"
                    );
    }
    
    switch (pixelOrderType)
    {
        case OrderRGBA:
            replaceStringInFunctions(FragmentShader,"##VAL0","r");
            replaceStringInFunctions(FragmentShader,"##VAL1","g");
            replaceStringInFunctions(FragmentShader,"##VAL2","b");
            replaceStringInFunctions(FragmentShader,"##VAL3","a");
            break;
        case OrderABGR:
            replaceStringInFunctions(FragmentShader,"##VAL0","a");
            replaceStringInFunctions(FragmentShader,"##VAL1","b");
            replaceStringInFunctions(FragmentShader,"##VAL2","g");
            replaceStringInFunctions(FragmentShader,"##VAL3","r");
            break;
    }
    
    switch (shaderVersion)
    {
        case ShaderGenerator::ShaderVersion2:
            replaceStringInFunctions(FragmentShader, "##TEXTURE", "texture2D");
            replaceStringInFunctions(FragmentShader, "##FRAGCOLOR", "gl_FragColor");
            break;
        case ShaderGenerator::ShaderVersion3:
            addGlobalVariable(PrecisionDefault, StorageQualOutput, DataVec4, "fragColor");
            replaceStringInFunctions(FragmentShader, "##TEXTURE", "texture");
            replaceStringInFunctions(FragmentShader, "##FRAGCOLOR", "fragColor");
            break;
    }
    
    return true;
}


}
