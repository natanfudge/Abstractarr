git add .
git commit -m %1
git push
cd ../MetaUtils-Releases
gradlew bintrayupload
git add .
git commit -m %1
git push
cd ../Abstractarr
