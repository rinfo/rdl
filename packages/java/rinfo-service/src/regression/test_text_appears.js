var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Failing test for correct title', function(test) {
   casper.start('http://service.regression.lagrummet.se/ui/');
   casper.waitForSelector(x("//*[contains(text(), \'RInfo Service UI\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'RInfo Service UI\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'RInfo Service UI\')]"));
   });

   casper.run(function() {test.done();});
});