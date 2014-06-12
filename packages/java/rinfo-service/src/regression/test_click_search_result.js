var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Test click of search result', function(test) {
   casper.start(casper.cli.get("url")+'/ui/');
   casper.waitForSelector("input[name='q']",
       function success() {
           this.sendKeys("input[name='q']", "1999:175");
       },
       function fail() {
           test.assertExists("input[name='q']");
   });
   casper.waitForSelector("form#queryForm button",
       function success() {
           test.assertExists("form#queryForm button");
           this.click("form#queryForm button");
       },
       function fail() {
           test.assertExists("form#queryForm button");
   });
   casper.waitForSelector(x("//*[contains(text(), \'Förordning\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'Förordning\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'Förordning\')]"));
   });
   casper.waitForSelector(".row",
       function success() {
           test.assertExists(".row");
           this.click(".row");
       },
       function fail() {
           test.assertExists(".row");
   });
   casper.waitForSelector(".row",
       function success() {
           test.assertExists(".row");
           this.click(".row");
       },
       function fail() {
           test.assertExists(".row");
   });
/*
   casper.waitForSelector("#documentView div:nth-child(2) > dl",
       function success() {
           test.assertExists("#documentView div:nth-child(2) > dl");
           this.click("#documentView div:nth-child(2) > dl");
       },
       function fail() {
           test.assertExists("#documentView div:nth-child(2) > dl");
   });
*/
/*
   casper.waitForSelector("#documentView div:nth-child(2) dd:nth-child(12)",
       function success() {
           test.assertExists("#documentView div:nth-child(2) dd:nth-child(12)");
           this.click("#documentView div:nth-child(2) dd:nth-child(12)");
       },
       function fail() {
           test.assertExists("#documentView div:nth-child(2) dd:nth-child(12)");
   });
*/
/*
   casper.waitForSelector("#documentView div:nth-child(2) dd:nth-child(10)",
       function success() {
           test.assertExists("#documentView div:nth-child(2) dd:nth-child(10)");
           this.click("#documentView div:nth-child(2) dd:nth-child(10)");
       },
       function fail() {
           test.assertExists("#documentView div:nth-child(2) dd:nth-child(10)");
   });
*/
/*
   casper.waitForSelector(x("/[contains(text(), \'SFS 1999:175\')]"),
       function success() {
           test.assertExists(x("/[contains(text(), \'SFS 1999:175\')]"));
         },
       function fail() {
           test.assertExists(x("/[contains(text(), \'SFS 1999:175\')]"));
   });
   casper.waitForSelector(x("/[contains(text(), \'\')]"),
       function success() {
           test.assertExists(x("/[contains(text(), \'\')]"));
         },
       function fail() {
           test.assertExists(x("/[contains(text(), \'\')]"));
   });
*/

   casper.run(function() {test.done();});
});