var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Test no protocol in url', function(test) {
   casper.start(casper.cli.get("url"));
   casper.waitForSelector("form#html-form input[name='feedUrl']",
       function success() {
           test.assertExists("form#html-form input[name='feedUrl']");
           this.click("form#html-form input[name='feedUrl']");
       },
       function fail() {
           test.assertExists("form#html-form input[name='feedUrl']");
   });
   casper.waitForSelector("input[name='feedUrl']",
       function success() {
           this.sendKeys("input[name='feedUrl']", "URL_HERE\r");
       },
       function fail() {
           test.assertExists("input[name='feedUrl']");
   });
   casper.waitForSelector("form#html-form input[type=submit][value='Check']",
       function success() {
           test.assertExists("form#html-form input[type=submit][value='Check']");
           this.click("form#html-form input[type=submit][value='Check']");
       },
       function fail() {
           test.assertExists("form#html-form input[type=submit][value='Check']");
   });
   casper.waitForSelector(x("//*[contains(text(), \'java.net.MalformedURLException: no protocol: URL_HERE\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'java.net.MalformedURLException: no protocol: URL_HERE\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'java.net.MalformedURLException: no protocol: URL_HERE\')]"));
   });

   casper.run(function() {test.done();});
});