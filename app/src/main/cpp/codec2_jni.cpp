#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <cstdlib>

extern "C" {
#include "codec2/codec2.h"
}

#define LOG_TAG "Codec2JNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jbyteArray JNICALL
Java_org_meshtastic_core_audio_Codec2Wrapper_encode(
        JNIEnv *env, jobject /*obj*/, jshortArray pcm, jint mode) {

    struct CODEC2 *c2 = codec2_create(mode);
    if (!c2) {
        LOGE("codec2_create failed for mode %d", mode);
        return nullptr;
    }

    int samplesPerFrame = codec2_samples_per_frame(c2);
    int bitsPerFrame = codec2_bits_per_frame(c2);
    int bytesPerFrame = (bitsPerFrame + 7) / 8;

    jsize pcmLen = env->GetArrayLength(pcm);
    int numFrames = pcmLen / samplesPerFrame;

    jshort *pcmData = env->GetShortArrayElements(pcm, nullptr);
    if (!pcmData) {
        codec2_destroy(c2);
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(numFrames * bytesPerFrame);
    if (!result) {
        env->ReleaseShortArrayElements(pcm, pcmData, JNI_ABORT);
        codec2_destroy(c2);
        return nullptr;
    }

    jbyte *outData = env->GetByteArrayElements(result, nullptr);

    for (int i = 0; i < numFrames; i++) {
        codec2_encode(c2,
                      reinterpret_cast<unsigned char *>(outData + i * bytesPerFrame),
                      reinterpret_cast<short *>(pcmData + i * samplesPerFrame));
    }

    env->ReleaseByteArrayElements(result, outData, 0);
    env->ReleaseShortArrayElements(pcm, pcmData, JNI_ABORT);
    codec2_destroy(c2);
    return result;
}

JNIEXPORT jshortArray JNICALL
Java_org_meshtastic_core_audio_Codec2Wrapper_decode(
        JNIEnv *env, jobject /*obj*/, jbyteArray codec2Data, jint mode) {

    struct CODEC2 *c2 = codec2_create(mode);
    if (!c2) {
        LOGE("codec2_create failed for mode %d", mode);
        return nullptr;
    }

    int samplesPerFrame = codec2_samples_per_frame(c2);
    int bitsPerFrame = codec2_bits_per_frame(c2);
    int bytesPerFrame = (bitsPerFrame + 7) / 8;

    jsize inLen = env->GetArrayLength(codec2Data);
    int numFrames = inLen / bytesPerFrame;

    jbyte *inData = env->GetByteArrayElements(codec2Data, nullptr);
    if (!inData) {
        codec2_destroy(c2);
        return nullptr;
    }

    jshortArray result = env->NewShortArray(numFrames * samplesPerFrame);
    if (!result) {
        env->ReleaseByteArrayElements(codec2Data, inData, JNI_ABORT);
        codec2_destroy(c2);
        return nullptr;
    }

    jshort *outData = env->GetShortArrayElements(result, nullptr);

    for (int i = 0; i < numFrames; i++) {
        codec2_decode(c2,
                      reinterpret_cast<short *>(outData + i * samplesPerFrame),
                      reinterpret_cast<unsigned char *>(inData + i * bytesPerFrame));
    }

    env->ReleaseShortArrayElements(result, outData, 0);
    env->ReleaseByteArrayElements(codec2Data, inData, JNI_ABORT);
    codec2_destroy(c2);
    return result;
}

JNIEXPORT jint JNICALL
Java_org_meshtastic_core_audio_Codec2Wrapper_getSamplesPerFrame(
        JNIEnv * /*env*/, jobject /*obj*/, jint mode) {
    struct CODEC2 *c2 = codec2_create(mode);
    if (!c2) return -1;
    int n = codec2_samples_per_frame(c2);
    codec2_destroy(c2);
    return n;
}

JNIEXPORT jint JNICALL
Java_org_meshtastic_core_audio_Codec2Wrapper_getBytesPerFrame(
        JNIEnv * /*env*/, jobject /*obj*/, jint mode) {
    struct CODEC2 *c2 = codec2_create(mode);
    if (!c2) return -1;
    int bits = codec2_bits_per_frame(c2);
    codec2_destroy(c2);
    return (bits + 7) / 8;
}

}
