var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

casper.test.begin('Check main feed exists', function(test) {
    casper.start().then(function() {
        this.open(casper.cli.get("url")+'feed/current', {
            method: 'get',
            headers: {
                'Accept': 'application/atom+xml'
            }
        });
    });

   casper.waitForSelector("author",
       function success() {
           test.assertExists("author");
           this.click("author");
       },
       function fail() {
           test.assertExists("author");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Rättsinformationssystemet\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Rättsinformationssystemet\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Rättsinformationssystemet\')]"));
   });

   casper.run(function() {test.done();});
});