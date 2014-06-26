var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

captureScreen = function() {
   var file_name = casper.cli.get("output")+'test_error_list_screen_error.png';
   this.capture(file_name);
   this.echo('Captured "'+file_name+'"');
}

casper.test.begin('Test common errors in bvfs feed', function(test) {
   casper.start(casper.cli.get("url"));

   casper.waitForSelector("body");

   var feedUrl = "http://testfeed.lagrummet.se/fel_1/index.atom";

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
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_left_section > dl > dd:nth-child(2)','tag:boverket.se,2009:rinfo:feed');
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_right_section > dl > dd:nth-child(2)', '10');
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_right_section > dl > dd.success', '0');
        this.test.assertSelectorHasText('#target > div > div.source > div.summary > div.summary_right_section > dl > dd.error', '10');

        //todo the id "filtrera" exists serveral times within the document
        this.test.assertTextExists('Saknar obligatoriskt värde för egenskap: bemyndigande - 7st');
        this.test.assertTextExists('Värdet matchar inte datatyp för egenskap: utkomFranTryck - 1st');
        this.test.assertTextExists('Värdet matchar inte datatyp för egenskap: ikrafttradandedatum - 1st');
        //this.test.assertTextExists('Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 2st');
        this.test.assertTextExists('Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet'); //todo this value is surrounded with Quotation marks in the html text. Not correct
        this.test.assertTextExists('Övriga - 1st');

        this.test.assertTextExists('Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet');
        this.test.assertTextExists('Saknar obligatoriskt värde för egenskap: bemyndigande');
        this.test.assertTextExists('http://testfeed.lagrummet.se/fel_1/1988-13tob8.xml');
        this.test.assertTextExists('Värdet \'1988-03-00\' matchar inte datatyp för egenskap: utkomFranTryck');
        this.test.assertTextExists('Värdet \'9999-99-99\' matchar inte datatyp för egenskap: ikrafttradandedatum');
   });

   casper.run(function() {test.done();});
});
