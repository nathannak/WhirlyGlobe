/*
 *  ShaderGenerator_jni.cpp
 *  WhirlyGlobeLib
 *
 *  Created by Steve Gifford on 10/11/16.
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

#import <jni.h>
#import "Maply_jni.h"
#import "com_mousebird_maply_imagerypro_ShaderGenerator.h"
#import "WhirlyGlobe.h"

using namespace WhirlyKit;
using namespace MaplyIPro;

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_nativeInit
(JNIEnv *env, jclass cls)
{
    ShaderGeneratorWrapperClassInfo::getClassInfo(env,cls);
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_setVersion
(JNIEnv *env, jobject obj, jint version)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->shaderGen->setVersion((ShaderGenerator::ShaderVersion)version);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::setVersion()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_setGlobalPrecision
(JNIEnv *env, jobject obj, jint vertexPrec, jint fragPrec)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->shaderGen->setGlobalPrecision((ShaderGenerator::PrecisionType)vertexPrec,(ShaderGenerator::PrecisionType)fragPrec);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::setGlobalPrecision()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_addGlobalCodeBlock
(JNIEnv *env, jobject obj, jint shaderType, jstring shaderStr)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        JavaString shaderString(env,shaderStr);
        shaderGen->shaderGen->addGlobalCodeBlock((ShaderGenerator::ShaderType)shaderType,shaderString.cStr);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::addGlobalCodeBlock()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_addGlobalVariable
(JNIEnv *env, jobject obj, jint precType, jint storageType, jint dataType, jstring varStr, jint layoutIndex)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        JavaString varString(env,varStr);
        shaderGen->shaderGen->addGlobalVariable((ShaderGenerator::PrecisionType)precType,
                                     (ShaderGenerator::StorageQualType)storageType,
                                     (ShaderGenerator::DataType)dataType,
                                     varString.cStr,layoutIndex);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::addGlobalVariable()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_addFunction
(JNIEnv *env, jobject obj, jint shaderType, jstring funcNameStr, jstring funcBodyStr)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        JavaString funcNameString(env,funcNameStr);
        JavaString funcBodyString(env,funcBodyStr);
        shaderGen->shaderGen->addFunction((ShaderGenerator::ShaderType)shaderType,
                               funcNameString.cStr,
                               funcBodyString.cStr);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::addFunction()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_replaceStringInFunctions
(JNIEnv *env, jobject obj, jint shaderType, jstring oldStr, jstring newStr)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        JavaString oldString(env,oldStr);
        JavaString newString(env,newStr);
        shaderGen->shaderGen->replaceStringInFunctions((ShaderGenerator::ShaderType)shaderType,
                                            oldString.cStr,newString.cStr);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::replaceStringInFunctions()");
    }
}

JNIEXPORT jstring JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_generateShaders
(JNIEnv *env, jobject obj)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return NULL;
        
        std::string errorStr;
        bool retVal = shaderGen->shaderGen->generateShaders(shaderGen->vertStr,shaderGen->fragStr,errorStr);
        
        if (!retVal)
            return env->NewStringUTF(errorStr.c_str());
        else
            return NULL;
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::generateShaders()");
    }
}

JNIEXPORT jstring JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_getVertexShader
(JNIEnv *env, jobject obj)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return NULL;

        return env->NewStringUTF(shaderGen->vertStr.c_str());
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::getVertexShader()");
    }
}

JNIEXPORT jstring JNICALL Java_com_mousebird_maply_imagerypro_ShaderGenerator_getFragmentShader
(JNIEnv *env, jobject obj)
{
    try
    {
        ShaderGeneratorWrapperClassInfo *classInfo = ShaderGeneratorWrapperClassInfo::getClassInfo();
        ShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return NULL;
        
        return env->NewStringUTF(shaderGen->fragStr.c_str());
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in ShaderGenerator::getFragmentShader()");
    }
}
