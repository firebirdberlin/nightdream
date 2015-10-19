#! /bin/bash

image=$1

mkdir -p ../res/drawable-ldpi
mkdir -p ../res/drawable-mdpi
mkdir -p ../res/drawable-hdpi
mkdir -p ../res/drawable-xhdpi
mkdir -p ../res/drawable-xxhdpi
mkdir -p ../res/drawable-xxxhdpi

# launcher symbols
# msize=48
msize=36
lsize=`calc -p 0.7*$msize`
hsize=`calc -p 1.5*$msize`
xhsize=`calc -p 2.0*$msize`
xxhsize=`calc -p 3.0*$msize`
xxxhsize=`calc -p 4.0*$msize`


convert $image -resize $msizex$msize       ../res/drawable-mdpi/$image
convert $image -resize $lsizex$lsize       ../res/drawable-ldpi/$image
convert $image -resize $xhsizex$xhsize     ../res/drawable-hdpi/$image
convert $image -resize $xhsizex$xhsize     ../res/drawable-xhdpi/$image
convert $image -resize $xxhsizex$xxhsize   ../res/drawable-xxhdpi/$image
convert $image -resize $xxxhsizex$xxxhsize ../res/drawable-xxxhdpi/$image
