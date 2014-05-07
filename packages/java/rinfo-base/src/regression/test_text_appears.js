var x = require('casper').selectXPath;
casper.options.viewportSize = {width: 1855, height: 968};
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Positive test base', function(test) {
   casper.start('http://admin.regression.lagrummet.se/');
   casper.waitForSelector("title",
       function success() {
           test.assertExists("title");
           this.click("title");
       },
       function fail() {
           test.assertExists("title");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Index\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Index\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Index\')]"));
   });

   casper.run(function() {test.done();});
});