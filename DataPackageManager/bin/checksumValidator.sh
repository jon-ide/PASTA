CHECKSUM_DIR=/home/pasta/local/checksum
mkdir -p $CHECKSUM_DIR
DATE=$(date +"%Y-%m-%d_%H:%M:%S")
java -cp $DATAPACKAGEMANAGER/WebRoot/WEB-INF/classes:$DATAPACKAGEMANAGER/WebRoot/WEB-INF/lib/*:$TOMCAT/lib/servlet-api.jar edu.lternet.pasta.datapackagemanager.checksum.ChecksumValidator >& $CHECKSUM_DIR/$DATE
