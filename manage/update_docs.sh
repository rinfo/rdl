#!/bin/sh
if [ -z "$PW_RINFO" ]; then
        echo "Enter sudo password: "
        read PW_RINFO
fi

fab -p $PW_RINFO target.prod -R doc app.docs.build app.docs.deploy