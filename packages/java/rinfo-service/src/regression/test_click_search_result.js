var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
captureScreen = function() {
   this.capture('test_click_search_result_screen_error.png');
   this.echo('Captured "test_click_search_result_screen_error.png"');

}
casper.test.begin('Test click of search result', function(test) {
   casper.start(casper.cli.get("url")+'/ui/');

   casper.waitForSelector("body");

   casper.then(function() {
        this.test.assertTitle('RInfo Service UI');
        this.test.assertTextDoesntExist('Sökresultat');
        this.sendKeys("#queryForm input[name='q']", "1999");
        this.click('#queryForm button[type="submit"]');
   });

   casper.waitForSelector("#resultsView h2", function(){}, captureScreen, 5000);

   casper.then(function() {
        this.test.assertTextExists("Sökresultat");
        this.test.assertExists("a[href='#/publ/sfs/1999:766/data.json']");
        this.click("a[href='#/publ/sfs/1999:766/data.json']");
   });

   casper.waitForSelector("#documentView h2", function(){}, captureScreen, 20000);

   casper.then(function() {
        this.test.assertSelectorHasText('#documentView h2', 'SFS 1999:766');
        //this.capture('test_click_search_result_screen.png');
   })

   casper.run(function() {test.done();});
});

