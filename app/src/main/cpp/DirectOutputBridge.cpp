// JNI surface for com.tuned.app.playback.InternalDirectStream.

#include "DirectOutputStream.h"

#include <jni.h>

#include <cstdint>
#include <new>

namespace {

using tuned::audio::DirectOutputStream;
using tuned::audio::DirectProbe;

DirectOutputStream* fromHandle(jlong handle) noexcept {
    return reinterpret_cast<DirectOutputStream*>(static_cast<std::uintptr_t>(handle));
}

jintArray toIntArray(JNIEnv* env, const jint* values, jsize count) {
    jintArray array = env->NewIntArray(count);
    if (array != nullptr) env->SetIntArrayRegion(array, 0, count, values);
    return array;
}

}  // namespace

extern "C" JNIEXPORT jintArray JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeProbe(
    JNIEnv* env,
    jclass,
    jint channelCount) {
    const DirectProbe probe = DirectOutputStream::probe(channelCount);
    const jint values[] = {
        probe.status,
        probe.exclusive ? 1 : 0,
        probe.mmap ? 1 : 0,
        probe.sampleRate,
        probe.channelCount,
        probe.encoding,
        probe.deviceId,
        probe.framesPerBurst,
        probe.bufferCapacity,
    };
    return toIntArray(env, values, static_cast<jsize>(sizeof(values) / sizeof(values[0])));
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeCreate(JNIEnv*, jclass) {
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(new (std::nothrow) DirectOutputStream()));
}

extern "C" JNIEXPORT void JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeDestroy(JNIEnv*, jclass, jlong handle) {
    delete fromHandle(handle);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeOpen(
    JNIEnv*,
    jclass,
    jlong handle,
    jint sampleRate,
    jint channelCount,
    jint encoding) {
    auto* stream = fromHandle(handle);
    if (stream == nullptr) return tuned::audio::kOpenBadArgs;
    return stream->open(sampleRate, channelCount, encoding);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeClose(JNIEnv*, jclass, jlong handle) {
    if (auto* stream = fromHandle(handle)) stream->close();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeStart(JNIEnv*, jclass, jlong handle) {
    auto* stream = fromHandle(handle);
    return stream != nullptr && stream->start() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativePause(JNIEnv*, jclass, jlong handle) {
    auto* stream = fromHandle(handle);
    return stream != nullptr && stream->pause() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeFlush(JNIEnv*, jclass, jlong handle) {
    if (auto* stream = fromHandle(handle)) stream->flush();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeWrite(
    JNIEnv* env,
    jclass,
    jlong handle,
    jobject directBuffer,
    jint offsetBytes,
    jint frames,
    jint inputEncoding,
    jint inputChannels) {
    auto* stream = fromHandle(handle);
    if (stream == nullptr || directBuffer == nullptr || offsetBytes < 0 || frames <= 0) return 0;
    auto* base = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(directBuffer));
    if (base == nullptr) return 0;
    // Bounds-check against the direct buffer's real capacity BEFORE any
    // pointer math (mirrors NativeBridge's write validation). Without it,
    // a bad offset/frame count from the Kotlin side made the conversion
    // loop read past the end of the buffer.
    const jlong capacity = env->GetDirectBufferCapacity(directBuffer);
    if (capacity < 0) return 0;
    const std::size_t bytesPerSample = [] (std::int32_t encoding) -> std::size_t {
        switch (encoding) {
            case tuned::audio::kDirectI16: return 2;
            case tuned::audio::kDirectI24Packed: return 3;
            case tuned::audio::kDirectI32: return 4;
            case tuned::audio::kDirectFloat: return 4;
            default: return 0;
        }
    }(inputEncoding);
    if (bytesPerSample == 0 || inputChannels < 1 || inputChannels > 8) return 0;
    const std::size_t bytesPerFrame = bytesPerSample * static_cast<std::size_t>(inputChannels);
    const std::size_t offset = static_cast<std::size_t>(offsetBytes);
    const std::size_t required = static_cast<std::size_t>(frames) * bytesPerFrame;
    if (offset > static_cast<std::size_t>(capacity) ||
        required > static_cast<std::size_t>(capacity) - offset) {
        return 0;
    }
    return stream->write(base + offsetBytes, frames, inputEncoding, inputChannels);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeCounters(JNIEnv* env, jclass, jlong handle) {
    auto* stream = fromHandle(handle);
    const jlong values[] = {
        stream != nullptr ? stream->consumedFrames() : 0,
        stream != nullptr ? stream->bufferedFrames() : 0,
        stream != nullptr ? stream->underruns() : 0,
        stream != nullptr && stream->isAlive() ? 1 : 0,
        stream != nullptr && stream->isStarted() ? 1 : 0,
    };
    jlongArray array = env->NewLongArray(5);
    if (array != nullptr) env->SetLongArrayRegion(array, 0, 5, values);
    return array;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_tuned_app_playback_InternalDirectStream_nativeInfo(JNIEnv* env, jclass, jlong handle) {
    auto* stream = fromHandle(handle);
    const jint values[] = {
        stream != nullptr ? stream->sampleRate() : 0,
        stream != nullptr ? stream->channelCount() : 0,
        stream != nullptr ? stream->encoding() : -1,
        stream != nullptr ? stream->deviceId() : 0,
        stream != nullptr ? stream->deviceBufferFrames() : 0,
        stream != nullptr && stream->mmapUsed() ? 1 : 0,
    };
    return toIntArray(env, values, static_cast<jsize>(sizeof(values) / sizeof(values[0])));
}
