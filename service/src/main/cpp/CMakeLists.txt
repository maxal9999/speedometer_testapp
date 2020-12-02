cmake_minimum_required(VERSION 3.4.1)

set(LIBRARY_NAME generateService-lib)

add_library(${LIBRARY_NAME}
            SHARED
            generateService-lib.cpp)

target_include_directories(${LIBRARY_NAME} 
  PRIVATE
     include
     cpp
)

target_sources(${LIBRARY_NAME}
  PRIVATE
    include/NeedlePositionGenerator.h
    src/NeedlePositionGenerator.cpp
)
