app=ngrok.jar
dir=$(dirname $0)
nohup java -Xbootclasspath/a:$dir -jar $dir/$app >/dev/null 2>&1 &
