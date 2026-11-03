cmake_minimum_required(VERSION 3.22.1)
project("decent_usb_audio" C CXX)

set(CMAKE_CXX_STANDARD 17)

# USB isochronous driver only. Tuned keeps its own Media3 FLAC decoder;
# the decent NativeAudioEngine / libFLAC path is not built.
add_library(decent_usb_audio SHARED
    usb-audio-output.cpp
)

find_library(log-lib log)
target_link_libraries(decent_usb_audio ${log-lib})

# Android 15+ devices with 16 KB memory pages refuse to load shared libraries
# whose ELF segments are aligned to 4 KB. The app module already links with
# this flag (app/src/main/cpp/CMakeLists.txt); without it here,
# System.loadLibrary("decent_usb_audio") throws UnsatisfiedLinkError on those
# devices and the whole exclusive USB output path is dead.
# NOTE: target_link_options, not add_link_options — the directory-level
# command only affects targets created AFTER it, and this library is
# created above (a first version of this fix used add_link_options at
# the end of the file, which silently never reached the target).
target_link_options(decent_usb_audio PRIVATE "-Wl,-z,max-page-size=16384")
