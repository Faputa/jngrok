PID=$(ps -ef | grep ngrokd.jar | grep -v grep | awk '{ print $2 }')
if [ ! -z "$PID" ]; then
echo kill -9 $PID
kill -9 $PID
fi