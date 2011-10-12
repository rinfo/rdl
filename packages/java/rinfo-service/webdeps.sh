#!/bin/bash
pushd "$(dirname $0)/src/main/webapp/ui/"

  curl -# http://code.jquery.com/jquery-1.6.4.min.js -O
  curl -# http://ajax.microsoft.com/ajax/jquery.templates/beta1/jquery.tmpl.min.js -O

  curl -# http://twitter.github.com/bootstrap/1.3.0/bootstrap.min.css -O
  curl -# http://twitter.github.com/bootstrap/1.3.0/bootstrap-modal.js -O

  curl -# http://html5shiv.googlecode.com/svn/trunk/html5.js -o html5shiv.js

popd
