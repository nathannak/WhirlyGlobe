/*
 *  MaplyIProQuadImagesLayer.mm
 *  WGMaply-ImageryPro
 *
 *  Created by Steve Gifford on 8/18/16.
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

#import <regex>
#import "MaplyIProShaderGenerator.h"

namespace MaplyIPro
{
    
std::string ShaderGenerator::ShaderFunction::getFormattedBody() const
{
    std::string formatStr;
    
    return std::regex_replace(body, std::regex(";"), ";\n");
}

ShaderGenerator::ShaderGenerator()
    : vertexPrec(PrecisionDefault), fragmentPrec(PrecisionDefault), shaderVersion(ShaderVersion2)
{
}

ShaderGenerator::~ShaderGenerator()
{
}
    
void ShaderGenerator::setVersion(ShaderVersion version)
{
    shaderVersion = version;
}
    
void ShaderGenerator::setGlobalPrecision(PrecisionType inVertexPrec,PrecisionType inFragmentPrec)
{
    vertexPrec = inVertexPrec;
    fragmentPrec = inFragmentPrec;
}

// Throw in a globe block of declarations.  Constants, probably.
void ShaderGenerator::addGlobalCodeBlock(ShaderType shaderType,const std::string &theBlock)
{
    switch (shaderType)
    {
        case VertexShader:
            vertexCodeBlocks.push_back(theBlock);
            break;
        case FragmentShader:
            fragmentCodeBlocks.push_back(theBlock);
            break;
    }
}

// Add a global variable, probably an input to the program.
void ShaderGenerator::addGlobalVariable(PrecisionType precType,StorageQualType storageType,DataType dataType,const std::string &varName,int layoutIndex)
{
    GlobalVariable newVar(varName);
    newVar.precType = precType;
    newVar.storageType = storageType;
    newVar.dataType = dataType;
    newVar.layoutIndex = layoutIndex;
    
    // Replace it if it's there
    auto it = globalVars.find(newVar);
    if (it != globalVars.end())
        globalVars.erase(it);
    
    globalVars.insert(newVar);
}

// Add a named function to the mix.  It will be pulled in if referenced by the body.
// Add the main here as well
void ShaderGenerator::addFunction(ShaderType shaderType,const std::string &funcName,const std::string &funcBody)
{
    ShaderFunction func(funcName);
    func.body = funcBody;
    
    switch (shaderType)
    {
        case VertexShader:
        {
            auto it = vertexFunctions.find(func);
            if (it != vertexFunctions.end())
                vertexFunctions.erase(it);
            vertexFunctions.insert(func);
        }
            break;
        case FragmentShader:
        {
            auto it = fragmentFunctions.find(func);
            if (it != fragmentFunctions.end())
                fragmentFunctions.erase(it);
            fragmentFunctions.insert(func);
        }
            break;
    }
}
    
void ShaderGenerator::replaceStringInFunctions(ShaderType shaderType,const std::string &oldStr,const std::string &newStr)
{
    switch (shaderType)
    {
        case VertexShader:
        {
            std::set<ShaderFunction> newVertFuncs;
            for (auto it : vertexFunctions)
            {
                it.body = std::regex_replace(it.body, std::regex(oldStr), newStr);
                newVertFuncs.insert(it);
            }
            vertexFunctions = newVertFuncs;
        }
            break;
        case FragmentShader:
        {
            std::set<ShaderFunction> newFragFuncs;
            for (auto it : fragmentFunctions)
            {
                it.body = std::regex_replace(it.body, std::regex(oldStr), newStr);
                newFragFuncs.insert(it);
            }
            fragmentFunctions = newFragFuncs;
        }
            break;
    }
}
    
std::string ShaderGenerator::versionString(ShaderVersion version)
{
    switch (version)
    {
        case ShaderVersion2:
            return "";
            break;
        case ShaderVersion3:
            return "#version 300 es\n";
            break;
    }
}
    
std::string ShaderGenerator::precisionString(PrecisionType precType)
{
    switch(precType)
    {
        case PrecisionLow:
            return "precision lowp float;";
            break;
        case PrecisionMedium:
            return "precision mediump float;";
            break;
        case PrecisionHigh:
            return "precision highp float;";
            break;
        case PrecisionDefault:
            return "";
            break;
    }
}
    
static const char *dataTypeName[ShaderGenerator::DataTypeMax] = {"bool","int","float","vec2","vec3","vec4","mat2","mat3","mat4","sampler2D"};
    
std::string ShaderGenerator::GlobalVariable::toString(ShaderVersion shaderVersion,ShaderType shaderType) const
{
    std::string line = "";
    
    switch (storageType)
    {
        case StorageQualAttribute:
            if (shaderVersion < ShaderVersion3)
                line += "attribute ";
            else
                line += "in ";
            break;
        case StorageQualUniform:
            line += "uniform ";
            break;
        case StorageQualVarying:
            if (shaderVersion < ShaderVersion3)
                line += "varying ";
            else {
                switch (shaderType)
                {
                    case VertexShader:
                        line += "out ";
                        break;
                    case FragmentShader:
                        line += "in ";
                        break;
                }
            }
            break;
        case StorageQualOutput:
            line += "out ";
            break;
    }

    switch (precType)
    {
        case PrecisionLow:
            line += "lowp ";
            break;
        case PrecisionMedium:
            line += "mediump ";
            break;
        case PrecisionHigh:
            line += "highp ";
            break;
        case PrecisionDefault:
            break;
    }

    line += (std::string)dataTypeName[dataType] + " ";
    line += name + ";";
    
    return line;
}

// Generate the shaders for both vertex and fragment if we can
// We'll pull in all the global variables and functions we see used
bool ShaderGenerator::generateShaders(std::string &vertShader,std::string &fragShader,std::string &error)
{
    vertShader += versionString(shaderVersion);
    fragShader += versionString(shaderVersion);

    // Optional precision strings
    vertShader += precisionString(vertexPrec);
    if (vertShader.length() > 0)
        vertShader += "\n\n";
    fragShader += precisionString(fragmentPrec);
    if (fragShader.length() > 0)
        fragShader += "\n\n";

    // First the uniforms
    bool hasUniform = false;
    for (const auto &thisVar : globalVars)
    {
        if (thisVar.storageType == StorageQualUniform)
        {
            std::string line = thisVar.toString(shaderVersion,FragmentShader) + "\n";
            // Note: Check that the variable is used
            if (thisVar.dataType != DataSampler2D)
                vertShader += line;
            fragShader += line;
            hasUniform = true;
        }
    }
    if (hasUniform)
    {
        vertShader += "\n";
        fragShader += "\n";
    }
    
    // The attributes, just in the vertex shader
    bool hasAttribute = false;
    for (const auto &thisVar : globalVars)
    {
        if (thisVar.storageType == StorageQualAttribute)
        {
            // Note: Check that the variable is used
            std::string line = thisVar.toString(shaderVersion,VertexShader) + "\n";
            vertShader += line;
            hasAttribute = true;
        }
    }
    if (hasAttribute)
    {
        vertShader += "\n";
    }
    
    // Varying, shared between both shaders
    bool hasVarying = false;
    for (const auto &thisVar : globalVars)
    {
        if (thisVar.storageType == StorageQualVarying)
        {
            std::string line = thisVar.toString(shaderVersion,VertexShader) + "\n";
            // Note: Check that the variable is used
            vertShader += line;
            hasVarying = true;
        }
        if (thisVar.storageType == StorageQualVarying)
        {
            std::string line = thisVar.toString(shaderVersion,FragmentShader) + "\n";
            // Note: Check that the variable is used
            fragShader += line;
            hasVarying = true;
        }
    }
    if (hasVarying)
    {
        vertShader += "\n";
        fragShader += "\n";
    }
    
    // Output, fragment shader only
    bool hasOutput = false;
    for (const auto &thisVar : globalVars)
    {
        if (thisVar.storageType == StorageQualOutput)
        {
            std::string line = thisVar.toString(shaderVersion,FragmentShader) + "\n";
            // Note: Check that the variable is used
            fragShader += line;
            hasOutput = true;
        }
    }
    if (hasOutput)
    {
        fragShader += "\n";
    }

    // Vertex functions (except for main)
    std::string mainBody;
    for (const auto &thisFunc : vertexFunctions)
    {
        if (thisFunc.name != "main")
        {
            vertShader += thisFunc.getFormattedBody() + "\n";
        } else {
            mainBody = thisFunc.getFormattedBody();
        }
    }
    // Toss in main at the end
    vertShader += mainBody + "\n";
    
    // Fragment functions (except for main)
    mainBody = "";
    for (const auto &thisFunc : fragmentFunctions)
    {
        if (thisFunc.name != "main")
        {
            fragShader += thisFunc.getFormattedBody() + "\n";
        } else {
            mainBody = thisFunc.getFormattedBody();
        }
    }
    // Toss in main at the end
    fragShader += mainBody + "\n";
    
    return true;
}
    
}
