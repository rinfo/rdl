var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Failing test for correct title', function(test) {
   casper.start(casper.cli.get("url")+'/ui/');

   casper.waitForSelector("body");

   casper.then(function() {
        this.test.assertTitle('RInfo Service UI');
   })

   casper.run(function() {test.done();});
});