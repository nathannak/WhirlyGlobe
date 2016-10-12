/*
 *  RampStackShader_jni.cpp
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
#import "com_mousebird_maply_imagerypro_RampStackShader.h"
#import "WhirlyGlobe.h"

using namespace WhirlyKit;
using namespace MaplyIPro;

class RampStackShaderGeneratorWrapper : public ShaderGeneratorWrapper
{
public:
    RampStackShaderGeneratorWrapper(RampStackShaderGenerator *inShaderGen) : ShaderGeneratorWrapper(inShaderGen) { }
    virtual ~RampStackShaderGeneratorWrapper() { }
    
    RampStackShaderGenerator *rampShaderGen() { return (RampStackShaderGenerator *)shaderGen; }
};

typedef JavaClassInfo<RampStackShaderGeneratorWrapper> RampStackShaderGeneratorWrapperClassInfo;
template<> RampStackShaderGeneratorWrapperClassInfo *RampStackShaderGeneratorWrapperClassInfo::classInfoObj = NULL;

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_nativeInit
(JNIEnv *env, jclass cls)
{
    RampStackShaderGeneratorWrapperClassInfo::getClassInfo(env,cls);
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_initialise
(JNIEnv *env, jobject obj)
{
    try
    {
        RampStackShaderGenerator *shaderGen = new RampStackShaderGenerator();
        RampStackShaderGeneratorWrapper *wrapper = new RampStackShaderGeneratorWrapper(shaderGen);
        RampStackShaderGeneratorWrapperClassInfo::getClassInfo()->setHandle(env,obj,wrapper);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::initialise()");
    }
}

static std::mutex disposeMutex;

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_dispose
(JNIEnv *env, jobject obj)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        {
            std::lock_guard<std::mutex> lock(disposeMutex);
            RampStackShaderGeneratorWrapper *inst = classInfo->getObject(env,obj);
            if (!inst)
                return;
            delete inst;
            
            classInfo->clearHandle(env,obj);
        }
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::dispose()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_setTexelInputType
(JNIEnv *env, jobject obj, jint inputType)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        RampStackShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->rampShaderGen()->setTexelInputType((RampStackShaderGenerator::TexelInputType)inputType);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::setTexelInputType()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_setIndexType
(JNIEnv *env, jobject obj, jint indexType)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        RampStackShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->rampShaderGen()->setIndexType((RampStackShaderGenerator::IndexType)indexType);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::setIndexType()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_setPixelByteOrder
(JNIEnv *env, jobject obj, jint orderType)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        RampStackShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->rampShaderGen()->setPixelByteOrder((RampStackShaderGenerator::PixelByteOrderType)orderType);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::setPixelByteOrder()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_setStackType
(JNIEnv *env, jobject obj, jint stackType)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        RampStackShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->rampShaderGen()->setStackType((RampStackShaderGenerator::StackType)stackType);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::setStackType()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_setStackCount
(JNIEnv *env, jobject obj, jint stackCount)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        RampStackShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->rampShaderGen()->setStackCount(stackCount);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::setStackCount()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_setTemporalInterpolation
(JNIEnv *env, jobject obj, jint tempType)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        RampStackShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->rampShaderGen()->setTemporalInterpolation((RampStackShaderGenerator::TemporalInterpType)tempType);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::setTemporalInterpolation()");
    }
}

JNIEXPORT void JNICALL Java_com_mousebird_maply_imagerypro_RampStackShader_setSpatialInterpolation
(JNIEnv *env, jobject obj, jint spatialInterp)
{
    try
    {
        RampStackShaderGeneratorWrapperClassInfo *classInfo = RampStackShaderGeneratorWrapperClassInfo::getClassInfo();
        RampStackShaderGeneratorWrapper *shaderGen = classInfo->getObject(env,obj);
        if (!shaderGen)
            return;
        
        shaderGen->rampShaderGen()->setSpatialInterpolation((RampStackShaderGenerator::SpatialInterpType)spatialInterp);
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, "Maply", "Crash in RampStackShader::setSpatialInterpolation()");
    }
}
