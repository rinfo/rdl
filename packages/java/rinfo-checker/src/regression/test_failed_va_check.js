var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

captureScreen = function() {
   var file_name = casper.cli.get("output")+'test_failed_va_check_screen_error.png';
   this.capture(file_name);
   this.echo('Captured "'+file_name+'"');
}

casper.test.begin('Test test VA feed that checker finds errors in', function(test) {
   casper.start(casper.cli.get("url"));

   casper.waitForSelector("body");

   var feedUrl = "http://testfeed.lagrummet.se/dov_exempel_med_fel/index.atom"

   casper.then(function() {
        this.test.assertTitle('RInfo Checker: insamlingskontroll');
        this.test.assertTextDoesntExist('Utförd insamling');
        this.sendKeys("#html-form input[name='feedUrl']", feedUrl);
        this.click('#submitButton');
   });

   casper.waitForSelector("#target > div", function(){}, captureScreen, 5000);

   casper.then(function() {
        this.test.assertTextExists("Utförd insamling");
        this.test.assertSelectorHasText('#target > div > div.summary > div.summary_left_section > dl > dd > code:nth-child(1)', feedUrl);
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_left_section > dl > dd:nth-child(2)','tag:vagledandeavgoranden.dom.se,2011:rinfo:feed');
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_right_section > dl > dd:nth-child(2)', '3');
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_right_section > dl > dd.success', '0');
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_right_section > dl > dd.error', '3');

        //this.test.assertTextExists('Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 1st'); //todo this value is surrounded with Quotation marks in the html text. Not correct
        this.test.assertTextExists('Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet');
        this.test.assertTextExists('Saknar svenskt språkattribut (xml:lang) för egenskap: description - 1st');
        this.test.assertTextExists('Kan inte tolka URI:n:  - 1st'); //todo double spaces within text

        this.test.assertTextExists('Saknar svenskt språkattribut (xml:lang) för egenskap:');
        this.test.assertTextExists('Kan inte tolka URI:n <http://rinfo.lagrummet.se/publ/dom/hfd/2486-11/2012-11-07>. Detta kan bero på att URI:n innehåller delar som inte är konfigurerade i RDL eller att delarna är felstavade eller på annat sätt felaktiga.');
        this.test.assertTextExists('Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet');
   });

   casper.run(function() {test.done();});
});
