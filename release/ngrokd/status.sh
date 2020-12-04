app=ngrokd.jar
pid=$(ps -ef | grep $app | grep -v grep | awk '{ print $2 }')
if [ -z "$pid" ]; then
  echo "stoped: $app"
else
  echo "running: $app"
fi
