var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

captureScreen = function() {
   var file_name = casper.cli.get("output")+'test_quantity_limit_screen_error.png';
   this.capture(file_name);
   this.echo('Captured "'+file_name+'"');
}

casper.test.begin('Test correct VA feed', function(test) {
   casper.start(casper.cli.get("url"));

   casper.waitForSelector("body");

   var feedUrl = "http://testfeed.lagrummet.se/dov_exempel_utan_fel/index-uppdaterad.atom"

   casper.then(function() {
        this.test.assertTitle('RInfo Checker: insamlingskontroll');
        this.test.assertTextDoesntExist('Utförd insamling');
        this.sendKeys("#html-form input[name='feedUrl']", feedUrl);
        this.evaluate(function() {
                document.querySelector("#maxEntries").value = "2";
                return true;
            });
        this.click('#submitButton');
   });

   casper.waitForSelector("#target > div", function(){}, captureScreen, 5000);

   casper.then(function() {
        this.test.assertTextExists("Utförd insamling");
        this.test.assertSelectorHasText('#target > div > div.summary > div.summary_left_section > dl > dd > code:nth-child(1)', feedUrl);
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_left_section > dl > dd:nth-child(2)','tag:vagledandeavgoranden.dom.se,2011:rinfo:feed');
        this.test.assertSelectorHasText('#filtrera > span:nth-child(2)', 'Korrekta poster - 2st');
   });

   casper.run(function() {test.done();});
});

