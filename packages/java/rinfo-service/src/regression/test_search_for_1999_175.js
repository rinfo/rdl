var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

captureScreen = function() {
   var file_name = casper.cli.get("output")+'test_click_search_for_1999_175_screen_error.png';
   this.capture(file_name);
   this.echo('Captured "'+file_name+'"');
}

casper.test.begin('Test search for 1999:175', function(test) {
   casper.start(casper.cli.get("url")+'/ui/');

   casper.then(function() {
        this.test.assertTitle('RInfo Service UI');
        this.test.assertTextDoesntExist('Sökresultat');
        this.sendKeys("#queryForm input[name='q']", "1999:175");
        this.click('#queryForm button[type="submit"]');
   });

   casper.waitForSelector("#resultsView h2", function(){}, captureScreen, 5000);

   casper.then(function() {
        this.test.assertTextExists("Sökresultat");
        this.test.assertExists("a[href='#/publ/sfs/1999:175/data.json']");
        this.click("a[href='#/publ/sfs/1999:175/data.json']");
   });

   casper.waitForSelector("#documentView h2", function(){}, captureScreen, 20000);

   casper.then(function() {
        this.test.assertSelectorHasText('#documentView h2', 'SFS 1999:175');
   })

   casper.run(function() {test.done();});
});