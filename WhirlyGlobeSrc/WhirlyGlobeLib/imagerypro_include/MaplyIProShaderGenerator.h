/*
 *  MaplyIProShaderGenerator.h
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

#import <string>
#import <vector>
#import <set>

namespace MaplyIPro
{
    
/** @brief The shader generator builds OpenGL ES Programs from pieces.
    @details The shader generator is used to construct shader programs from a variety of pre-defined
    pieces.  It's not particularly intelligent, but it will paste strings together and do
    some very basic checking.
  */
class ShaderGenerator
{
public:
    ShaderGenerator();
    virtual ~ShaderGenerator();
    
    typedef enum {VertexShader,FragmentShader} ShaderType;
    typedef enum {ShaderVersion2,ShaderVersion3} ShaderVersion;
    typedef enum {PrecisionLow,PrecisionMedium,PrecisionHigh,PrecisionDefault} PrecisionType;
    typedef enum {StorageQualAttribute,StorageQualUniform,StorageQualVarying,StorageQualOutput} StorageQualType;
    typedef enum {DataBool,DataInt,DataFloat,DataVec2,DataVec3,DataVec4,DataMat2,DataMat3,DataMat4,DataSampler2D,DataTypeMax} DataType;
    
    /// @brief Set the OpenGL ES Version in the shader
    virtual void setVersion(ShaderVersion version);
    
    /// @brief Set the default precision for the program
    virtual void setGlobalPrecision(PrecisionType vertexPrec,PrecisionType fragmentPrec);
        
    /// @brief Throw in a globe block of declarations.  Constants, probably.
    virtual void addGlobalCodeBlock(ShaderType shaderType,const std::string &theBlock);
    
    /// @brief Add a global variable, probably an input to the program.
    virtual void addGlobalVariable(PrecisionType precType,StorageQualType storageType,DataType dataType,const std::string &varName,int layoutIndex=-1);

    /** @brief Add a named function.
        @details Add a named function to the mix.  It will be pulled in if referenced by the body.
        Add the main here as well.
     */
    virtual void addFunction(ShaderType shaderType,const std::string &funcName,const std::string &funcBody);
    
    /// @brief Replace occurances of one string with another in all functions
    virtual void replaceStringInFunctions(ShaderType shaderType,const std::string &oldStr,const std::string &newStr);
    
    /** @brief Generate all the shaders.
        @details Generate the shaders for both vertex and fragment if we can
        We'll pull in all the global variables and functions we see used.
     */
    virtual bool generateShaders(std::string &vertShader,std::string &fragShader,std::string &error);
    
protected:
    std::string precisionString(PrecisionType precType);
    std::string versionString(ShaderVersion version);
    
    class GlobalVariable
    {
    public:
        GlobalVariable(const std::string &name) : name(name) { }
        
        // Comparison operator
        bool operator < (const GlobalVariable &that) const { return name < that.name; }
        
        // Printable version.  Been writing too much Java.
        std::string toString(ShaderVersion shaderVersion,ShaderType shaderType) const;
        
        PrecisionType precType;
        StorageQualType storageType;
        DataType dataType;
        std::string name;
        int layoutIndex;
    };
    
    class ShaderFunction
    {
    public:
        ShaderFunction(const std::string &name) : name(name) { }
        
        // Comparison operator
        bool operator < (const ShaderFunction &that) const { return name < that.name; }
        
        // Return formatted (mess with the semi-colons)
        std::string getFormattedBody() const;

        std::string name;
        std::string body;
    };
    
    ShaderVersion shaderVersion;
    PrecisionType vertexPrec,fragmentPrec;
    
    std::vector<std::string> vertexCodeBlocks;
    std::vector<std::string> fragmentCodeBlocks;
    std::set<GlobalVariable> globalVars;
    std::set<ShaderFunction> vertexFunctions;
    std::set<ShaderFunction> fragmentFunctions;
    
};

}
