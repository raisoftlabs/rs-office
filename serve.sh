#!/data/data/com.termux/files/usr/bin/bash
PORT=8484
OLD=$(pgrep -f "python -m http[.]server $PORT" | head -5)
if [ -n "$OLD" ]; then kill $OLD 2>/dev/null; sleep 1; fi
cd "$HOME/vellum" || exit 1
setsid python -m http.server $PORT --bind 127.0.0.1 >$PREFIX/tmp/srv.log 2>&1 </dev/null &
disown
exit 0
