@echo off
"D:\\Software\\SDK\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\Project\\IOT\\YourAble\\opencv\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=24" ^
  "-DANDROID_PLATFORM=android-24" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=D:\\Software\\SDK\\ndk\\26.1.10909125" ^
  "-DCMAKE_ANDROID_NDK=D:\\Software\\SDK\\ndk\\26.1.10909125" ^
  "-DCMAKE_TOOLCHAIN_FILE=D:\\Software\\SDK\\ndk\\26.1.10909125\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=D:\\Software\\SDK\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\Project\\IOT\\YourAble\\opencv\\build\\intermediates\\cxx\\Debug\\193e3v6c\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\Project\\IOT\\YourAble\\opencv\\build\\intermediates\\cxx\\Debug\\193e3v6c\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BD:\\Project\\IOT\\YourAble\\opencv\\.cxx\\Debug\\193e3v6c\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
