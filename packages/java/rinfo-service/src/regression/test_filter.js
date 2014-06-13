var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
captureScreen = function() {
   var file_name = casper.cli.get("output")+'test_filter_screen_error.png';
   this.capture(file_name);
   this.echo('Captured "'+file_name+'"');
}
casper.test.begin('Test filter', function(test) {
   casper.start(casper.cli.get("url")+'/ui/');

   casper.waitForSelector("body");

   if (casper.cli.get("target") != "demo" && casper.cli.get("target") != "regression") {
       casper.then(function() {
            this.test.assertTitle('RInfo Service UI');
            this.test.assertTextDoesntExist('Sökresultat');
            this.sendKeys("#queryForm input[name='q']", "djur");
            this.evaluate(function() {
                    document.querySelector("#type").value = "VagledandeDomstolsavgorande";
                    document.querySelector("#publisher").value = "hoegsta_domstolen";
                    return true;
                });
            this.click('#queryForm button[type="submit"]');
       });

       casper.waitForSelector("#resultsView h2", function(){}, captureScreen, 5000);

       casper.then(function() {
            this.test.assertTextExists("Sökresultat");
            this.test.assertExists("a[href='#/publ/dom/hd/b2882-02/2003-12-22/data.json']");
            this.click("a[href='#/publ/dom/hd/b2882-02/2003-12-22/data.json']");
       });

       casper.waitForSelector("#documentView h2", function(){}, captureScreen, 20000);

       casper.then(function() {
            this.test.assertSelectorHasText("#documentView > div:nth-child(2) > dl > dd:nth-child(8)", "B2882-02");

       });
   }

   casper.run(function() {test.done();});
});



