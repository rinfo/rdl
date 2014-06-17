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
   casper.waitForSelector(x("//*[contains(text(), \'RInfo Checker\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'RInfo Checker\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'RInfo Checker\')]"));
   });

   casper.run(function() {test.done();});
});