var x = require('casper').selectXPath;
casper.options.viewportSize = {width: 1855, height: 968};
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Positive test service', function(test) {
   casper.start('http://demo.lagrummet.se/');
   casper.waitForSelector("article h1",
       function success() {
           test.assertExists("article h1");
           this.click("article h1");
       },
       function fail() {
           test.assertExists("article h1");
   });
   casper.waitForSelector(x("//*[contains(text(), \'TESTVERSION\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'TESTVERSION\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'TESTVERSION\')]"));
   });

   casper.run(function() {test.done();});
});