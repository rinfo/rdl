var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});

captureScreen = function() {
   var file_name = casper.cli.get("output")+'test_no_protocol_in_url_screen_error.png';
   this.capture(file_name);
   this.echo('Captured "'+file_name+'"');
}


casper.test.begin('Test no protocol in url', function(test) {
   casper.start(casper.cli.get("url"));

   casper.waitForSelector("body");

   var feedUrl = "URL_HERE";

   casper.then(function() {
        this.test.assertTitle('RInfo Checker: insamlingskontroll');
        this.test.assertTextDoesntExist('Internal Server Error');
        this.sendKeys("#html-form input[name='feedUrl']", feedUrl);
        this.click('#submitButton');
   });

   casper.waitForSelector("#target > p", function(){}, captureScreen, 5000);

   casper.then(function() {
        this.test.assertTextExists("Internal Server Error");
        this.test.assertTextExists("java.net.MalformedURLException: no protocol: URL_HERE");
        this.test.assertTextExists("You can get technical details");
   });

   casper.run(function() {test.done();});
});