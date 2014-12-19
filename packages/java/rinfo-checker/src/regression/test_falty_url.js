var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

captureScreen = function() {
   var file_name = casper.cli.get("output")+'test_fawlty_url_screen_error.png';
   this.capture(file_name);
   this.echo('Captured "'+file_name+'"');
}

casper.test.begin('Test response of nonexisting url', function(test) {
   casper.start(casper.cli.get("url"));

   casper.waitForSelector("body");

   var feedUrl = "http://this.utlrl.goes.nowhere.lagrbeta.lagruymmet.se";

   casper.then(function() {
        this.test.assertTitle('RInfo Checker: insamlingskontroll');
        this.test.assertTextDoesntExist('Ofullständig insamling');
        this.sendKeys("#html-form input[name='feedUrl']", feedUrl);
        this.click('#submitButton');
   });

   casper.waitForSelector("#target > div", function(){}, captureScreen, 5000);

   casper.then(function() {
        this.test.assertTextExists("Ofullständig insamling");
        this.test.assertTextExists("Sidfel");
        this.test.assertTextExists("Systemfel vid sidinläsning. Var vänlig att validera sidan. Teknisk orsak");
   });

   casper.run(function() {test.done();});
});