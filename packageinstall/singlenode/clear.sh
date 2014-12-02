#!/bin/bash

( cd ../admin && source clear.sh )
( cd ../rinfo && source clear.sh )
( cd ../service && source clear.sh )

rm -rf tmp rinfo-rdl-singlenode.tar.gz
