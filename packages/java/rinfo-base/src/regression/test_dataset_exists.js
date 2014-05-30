var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

casper.test.begin('Check admin feed exists', function(test) {
    casper.start().then(function() {
        this.open(casper.cli.get("url")+'sys/dataset/rdf.rdf', {
            method: 'get',
            headers: {
                'Accept': 'application/rdf+xml'
            }
        });
    });

   /*casper.waitForSelector("RDF",
       function success() {
           test.assertExists("RDF");
           this.click("RDF");
       },
       function fail() {
           test.assertExists("RDF");
   });

   casper.waitForSelector(x("//*[contains(text(), \'tag:lagrummet.se,2009:rinfo\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'tag:lagrummet.se,2009:rinfo\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'tag:lagrummet.se,2009:rinfo\')]"));
   });*/


   casper.run(function() {test.done();});
});