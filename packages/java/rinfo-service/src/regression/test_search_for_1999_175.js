var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Test search for 1999:175', function(test) {
   casper.start(casper.cli.get("url")+'/ui/');
/*
   casper.waitForSelector("form#queryForm input[name='q']",
       function success() {
           test.assertExists("form#queryForm input[name='q']");
           this.click("form#queryForm input[name='q']");
       },
       function fail() {
           test.assertExists("form#queryForm input[name='q']");
   });
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
*/
/*
   casper.waitForSelector(x("/[contains(text(), \'gav 245 träffar\')]"),
       function success() {
           test.assertExists(x("/[contains(text(), \'gav 245 träffar\')]"));
         },
       function fail() {
           test.assertExists(x("/[contains(text(), \'gav 245 träffar\')]"));
   });
*/
/*
   casper.waitForSelector("tr:nth-child(2) td:nth-child(1)",
       function success() {
           test.assertExists("tr:nth-child(2) td:nth-child(1)");
           this.click("tr:nth-child(2) td:nth-child(1)");
       },
       function fail() {
           test.assertExists("tr:nth-child(2) td:nth-child(1)");
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
*/


   casper.run(function() {test.done();});
});