var x = require('casper').selectXPath;
casper.options.viewportSize = {width: 1670, height: 585};
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Test test VA feed that checker finds errors in', function(test) {
   casper.start(casper.cli.get("url"));
   casper.waitForSelector("input[name='feedUrl']",
       function success() {
           this.sendKeys("input[name='feedUrl']", "http://testfeed.lagrummet.se/dov_exempel_med_fel/index.atom");
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
   casper.waitForSelector("#filtrera:nth-child(2)",
       function success() {
           test.assertExists("#filtrera:nth-child(2)");
           this.click("#filtrera:nth-child(2)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(2)");
   });
   casper.waitForSelector("#filtrera:nth-child(2) span:nth-child(2)",
       function success() {
           test.assertExists("#filtrera:nth-child(2) span:nth-child(2)");
           this.click("#filtrera:nth-child(2) span:nth-child(2)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(2) span:nth-child(2)");
   });
   casper.waitForSelector("#filtrera:nth-child(2) span:nth-child(2)",
       function success() {
           test.assertExists("#filtrera:nth-child(2) span:nth-child(2)");
           this.click("#filtrera:nth-child(2) span:nth-child(2)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(2) span:nth-child(2)");
   });
   casper.waitForSelector("#filtrera:nth-child(2)",
       function success() {
           test.assertExists("#filtrera:nth-child(2)");
           this.click("#filtrera:nth-child(2)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(2)");
   });
   /*casper.waitForSelector(x("//*[contains(text(), \'Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 1st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 1st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet: - 1st\')]"));
   });*/
   casper.waitForSelector("#filtrera:nth-child(3) span:nth-child(2)",
       function success() {
           test.assertExists("#filtrera:nth-child(3) span:nth-child(2)");
           this.click("#filtrera:nth-child(3) span:nth-child(2)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(3) span:nth-child(2)");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Saknar svenskt språkattribut (xml:lang) för egenskap: description - 1st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Saknar svenskt språkattribut (xml:lang) för egenskap: description - 1st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Saknar svenskt språkattribut (xml:lang) för egenskap: description - 1st\')]"));
   });
   casper.waitForSelector("#filtrera:nth-child(4)",
       function success() {
           test.assertExists("#filtrera:nth-child(4)");
           this.click("#filtrera:nth-child(4)");
       },
       function fail() {
           test.assertExists("#filtrera:nth-child(4)");
   });
   /*casper.waitForSelector(x("//*[contains(text(), \'Kan inte tolka URI:n: - 1st\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Kan inte tolka URI:n: - 1st\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Kan inte tolka URI:n: - 1st\')]"));
   });*/
   casper.waitForSelector("#show_all button",
       function success() {
           test.assertExists("#show_all button");
           this.click("#show_all button");
       },
       function fail() {
           test.assertExists("#show_all button");
   });
   casper.waitForSelector("tr:nth-child(2) dd",
       function success() {
           test.assertExists("tr:nth-child(2) dd");
           this.click("tr:nth-child(2) dd");
       },
       function fail() {
           test.assertExists("tr:nth-child(2) dd");
   });
   casper.waitForSelector("tr:nth-child(2) td:nth-child(5)",
       function success() {
           test.assertExists("tr:nth-child(2) td:nth-child(5)");
           this.click("tr:nth-child(2) td:nth-child(5)");
       },
       function fail() {
           test.assertExists("tr:nth-child(2) td:nth-child(5)");
   });
   casper.waitForSelector("tr:nth-child(2) dd",
       function success() {
           test.assertExists("tr:nth-child(2) dd");
           this.click("tr:nth-child(2) dd");
       },
       function fail() {
           test.assertExists("tr:nth-child(2) dd");
   });
   casper.waitForSelector("tr:nth-child(2) dd",
       function success() {
           test.assertExists("tr:nth-child(2) dd");
           this.click("tr:nth-child(2) dd");
       },
       function fail() {
           test.assertExists("tr:nth-child(2) dd");
   });
   casper.waitForSelector("tr:nth-child(2) dd",
       function success() {
           test.assertExists("tr:nth-child(2) dd");
           this.click("tr:nth-child(2) dd");
       },
       function fail() {
           test.assertExists("tr:nth-child(2) dd");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Saknar svenskt språkattribut\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Saknar svenskt språkattribut\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Saknar svenskt språkattribut\')]"));
   });
   casper.waitForSelector("tr:nth-child(3) td:nth-child(5)",
       function success() {
           test.assertExists("tr:nth-child(3) td:nth-child(5)");
           this.click("tr:nth-child(3) td:nth-child(5)");
       },
       function fail() {
           test.assertExists("tr:nth-child(3) td:nth-child(5)");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Kan inte tolka URI:n\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Kan inte tolka URI:n\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Kan inte tolka URI:n\')]"));
   });
   casper.waitForSelector("tr:nth-child(4) td:nth-child(5) div",
       function success() {
           test.assertExists("tr:nth-child(4) td:nth-child(5) div");
           this.click("tr:nth-child(4) td:nth-child(5) div");
       },
       function fail() {
           test.assertExists("tr:nth-child(4) td:nth-child(5) div");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Angiven URI matchar inte den URI\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Angiven URI matchar inte den URI\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Angiven URI matchar inte den URI\')]"));
   });

   casper.run(function() {test.done();});
});