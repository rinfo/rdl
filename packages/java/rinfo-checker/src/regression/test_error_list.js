var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Test common errors in bvfs feed', function(test) {
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
           this.sendKeys("input[name='feedUrl']", "http://testfeed.lagrummet.se/fel_1/index.atom");
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
   casper.waitForSelector(x("//*[contains(text(), \'Utförd insamling\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Utförd insamling\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Utförd insamling\')]"));
   });
   casper.waitForSelector("#filtrera:nth-child(2)",
       function success() {
           test.assertExists("#filtrera:nth-child(2)");
           this.click("#filtrera:nth-child(2)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(2)");
   });
   /*casper.waitForSelector(x("//*[contains(text(), \'Saknar obligatoriskt värde för egenskap: bemyndigande - 7st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Saknar obligatoriskt värde för egenskap: bemyndigande - 7st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Saknar obligatoriskt värde för egenskap: bemyndigande - 7st\')]"));
   });*/
   casper.waitForSelector("#filtrera:nth-child(3)",
       function success() {
           test.assertExists("#filtrera:nth-child(3)");
           this.click("#filtrera:nth-child(3)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(3)");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Värdet matchar inte datatyp för egenskap: utkomFranTryck - 1st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Värdet matchar inte datatyp för egenskap: utkomFranTryck - 1st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Värdet matchar inte datatyp för egenskap: utkomFranTryck - 1st\')]"));
   });
   casper.waitForSelector("#filtrera:nth-child(4)",
       function success() {
           test.assertExists("#filtrera:nth-child(4)");
           this.click("#filtrera:nth-child(4)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(4)");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Värdet matchar inte datatyp för egenskap: ikrafttradandedatum - 1st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Värdet matchar inte datatyp för egenskap: ikrafttradandedatum - 1st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Värdet matchar inte datatyp för egenskap: ikrafttradandedatum - 1st\')]"));
   });
   casper.waitForSelector("#filtrera:nth-child(5)",
       function success() {
           test.assertExists("#filtrera:nth-child(5)");
           this.click("#filtrera:nth-child(5)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(5)");
   });
   /*casper.waitForSelector(x("//*[contains(text(), \'Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 2st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 2st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 2st\')]"));
   });*/
   casper.waitForSelector("#filtrera:nth-child(6)",
       function success() {
           test.assertExists("#filtrera:nth-child(6)");
           this.click("#filtrera:nth-child(6)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(6)");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Övriga - 1st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Övriga - 1st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Övriga - 1st\')]"));
   });

   casper.run(function() {test.done();});
});