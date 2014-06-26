var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Check for Checker page title', function(test) {
   casper.start(casper.cli.get("url"));

   casper.start(casper.cli.get("url"));

   casper.waitForSelector("body");

   casper.then(function() {
        this.test.assertTitle('RInfo Checker: insamlingskontroll');
   });

   casper.run(function() {test.done();});
});