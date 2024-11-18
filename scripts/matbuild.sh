#!/usr/bin/env sh

for FILE in ../app/src/main/materials/*.mat; do
  filename="${FILE##*/}"
  output_file="../app/src/main/assets/materials/${filename%.*}.filamat"
  echo "compiling $filename"
#  dbg_flags=" -t"
#  dbg_file="app/src/debug/shaders/${FILE%%.*}.glsl"
  matc --optimize-size -a opengl --platform=mobile -o "$output_file" "$FILE";
done
