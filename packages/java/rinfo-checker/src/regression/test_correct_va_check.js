var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Test correct VA feed', function(test) {
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
           this.sendKeys("input[name='feedUrl']", "http://testfeed.lagrummet.se/dov_exempel_utan_fel/index-uppdaterad.atom");
       },
       function fail() {
           test.assertExists("input[name='feedUrl']");
   });
   casper.waitForSelector("form#html-form input[name='feedUrl']",
       function success() {
           test.assertExists("form#html-form input[name='feedUrl']");
           this.click("form#html-form input[name='feedUrl']");
       },
       function fail() {
           test.assertExists("form#html-form input[name='feedUrl']");
   });
   casper.waitForSelector("form#html-form input[name='feedUrl']",
       function success() {
           test.assertExists("form#html-form input[name='feedUrl']");
           this.click("form#html-form input[name='feedUrl']");
       },
       function fail() {
           test.assertExists("form#html-form input[name='feedUrl']");
   });
   casper.waitForSelector("form#html-form input[type=submit][value='Check']",
       function success() {
           test.assertExists("form#html-form input[type=submit][value='Check']");
           this.click("form#html-form input[type=submit][value='Check']");
       },
       function fail() {
           test.assertExists("form#html-form input[type=submit][value='Check']");
   });
   casper.waitForSelector("h2",
       function success() {
           test.assertExists("h2");
           this.click("h2");
       },
       function fail() {
           test.assertExists("h2");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Utförd insamling\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Utförd insamling\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Utförd insamling\')]"));
   });
   casper.waitForSelector("#filtrera",
       function success() {
           test.assertExists("#filtrera");
           this.click("#filtrera");
       },
       function fail() {
           test.assertExists("#filtrera");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Korrekta poster - 3st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Korrekta poster - 3st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Korrekta poster - 3st\')]"));
   });

   casper.run(function() {test.done();});
});