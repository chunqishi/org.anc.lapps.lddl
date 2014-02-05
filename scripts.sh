#!/bin/bash

# Zips the example script files in the modules directory.

set -eu

if [ ! -e VERSION ] ; then
    mvn anc:version
fi

VERSION=`cat VERSION`
RES=src/test/resources
MODULES=$RES/modules
ZIPFILE=lddl-$VERSION-scripts.zip

if [ ! -e $MODULES ] ; then
	echo "Modules directory not found: $MODULES"
	exit
fi

cd $RES

if [ -e $ZIPFILE ] ; then
	echo "Removing existing file: $ZIPFILE"
	rm $ZIPFILE
fi

echo "Packaging modules."
zip -r $ZIPFILE modules/

if [ -e /Volumes/share ] ; then
	cp $ZIPFILE /Volumes/share
	echo "Copied zip file to share"
fi

set +u
if  [ "$1" = "-upload" ] ; then
    anc-put $ZIPFILE /home/www/anc/downloads
fi

