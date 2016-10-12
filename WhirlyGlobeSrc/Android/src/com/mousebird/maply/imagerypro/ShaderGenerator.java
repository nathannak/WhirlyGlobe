package com.mousebird.maply.imagerypro;

/**
 * The shader generator builds OpenGL ES Programs from pieces.
 * <br>
 * The shader generator is used to construct shader programs from a variety of pre-defined
 * pieces.  It's not particularly intelligent, but it will paste strings together and do
 * some very basic checking.
 */
public class ShaderGenerator
{
    public enum ShaderType {VertexShader,FragmentShader};
    public enum ShaderVersion {ShaderVersion2,ShaderVersion3};
    public enum PrecisionType {PrecisionLow,PrecisionMedium,PrecisionHigh,PrecisionDefault};
    public enum StorageQualType {StorageQualAttribute,StorageQualUniform,StorageQualVarying,StorageQualOutput};
    public enum DataType {DataBool,DataInt,DataFloat,DataVec2,DataVec3,DataVec4,DataMat2,DataMat3,DataMat4,DataSampler2D,DataTypeMax};

    protected ShaderGenerator() {
    }

    /**
     * Set the OpenGL ES Version in the shader
     */
    public native void setVersion(int version);

    /**
     * Set the default precision for the program
     */
    public native void setGlobalPrecision(int vertexPrec,int fragmentPrec);

    /**
     * Throw in a globe block of declarations.  Constants, probably.
     */
    public native void addGlobalCodeBlock(int shaderType,String theBlock);

    /**
     * Add a global variable, probably an input to the program.
     */
    public native void addGlobalVariable(int precType,int storageType,int dataType,String varName,int layoutIndex);

    /**
     * Add a named function to the mix.  It will be pulled in if referenced by the body.
     * Add the main here as well.
     */
    public native void addFunction(int shaderType,String funcName,String funcBody);

    /**
     * Replace occurances of one string with another in all functions
     */
    public native void replaceStringInFunctions(int shaderType,String oldStr,String newStr);

    /**
     * Build the shaders we'll actually use.
     * @return An error string if the shaders failed to generate.
     */
    public native String generateShaders();

    /**
     * The vertex shader if generateShaders ran successfully.
     */
    public native String getVertexShader();

    /**
     * The fragment shader if generateShaders ran successfully.
     */
    public native String getFragmentShader();

    static
    {
        nativeInit();
    }
    private static native void nativeInit();
    private long nativeHandle;
}
