DIR=$(dirname $0)
nohup java -Xbootclasspath/a:$DIR -jar $DIR/ngrokd.jar >/dev/null 2>&1 &