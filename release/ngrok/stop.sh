PID=$(ps -ef | grep ngrok.jar | grep -v grep | awk '{ print $2 }')
if [ ! -z "$PID" ]; then
echo kill -9 $PID
kill -9 $PID
fi