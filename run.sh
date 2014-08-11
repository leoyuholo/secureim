
DIR=./

CLASSPATH=${DIR}:${DIR}classes:${DIR}mysql.jar

export CLASSPATH=CLASSPATH:${CLASSPATH}

javac -sourcepath src -d classes src/MessageServer.java

java -cp ${CLASSPATH} MessageServer
