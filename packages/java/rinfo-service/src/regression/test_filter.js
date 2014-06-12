var x = require('casper').selectXPath;
casper.on('page.error', function(msg, trace) {
   this.echo('Error: ' + msg, 'ERROR');
   for(var i=0; i<trace.length; i++) {
       var step = trace[i];
       this.echo('   ' + step.file + ' (line ' + step.line + ')', 'ERROR');
   }
});
casper.test.begin('Test filter', function(test) {
   casper.start(casper.cli.get("url")+'/ui/');
   casper.waitForSelector("#type option:nth-child(1)",
       function success() {
           test.assertExists("#type option:nth-child(1)");
           this.click("#type option:nth-child(1)");
       },
       function fail() {
           test.assertExists("#type option:nth-child(1)");
   });
   casper.waitForSelector("#publisher option:nth-child(1)",
       function success() {
           test.assertExists("#publisher option:nth-child(1)");
           this.click("#publisher option:nth-child(1)");
       },
       function fail() {
           test.assertExists("#publisher option:nth-child(1)");
   });
   casper.waitForSelector("form#queryForm button",
       function success() {
           test.assertExists("form#queryForm button");
           this.click("form#queryForm button");
       },
       function fail() {
           test.assertExists("form#queryForm button");
   });
   /*casper.waitForSelector(x("//*[contains(text(), \'gav 27975\')]"),
       function success() {
           test.assertExists(x("//*[contains(text(), \'gav 27975\')]"));
         },
       function fail() {
           test.assertExists(x("//*[contains(text(), \'gav 27975\')]"));
   });*/
   casper.waitForSelector("tbody tr:nth-child(1) td:nth-child(1)",
       function success() {
           test.assertExists("tbody tr:nth-child(1) td:nth-child(1)");
           this.click("tbody tr:nth-child(1) td:nth-child(1)");
       },
       function fail() {
           test.assertExists("tbody tr:nth-child(1) td:nth-child(1)");
   });
/*
   casper.waitForSelector(x("/[contains(text(), \'SFS 1991:1733\')]"),
       function success() {
           test.assertExists(x("/[contains(text(), \'SFS 1991:1733\')]"));
         },
       function fail() {
           test.assertExists(x("/[contains(text(), \'SFS 1991:1733\')]"));
   });
*/

   casper.run(function() {test.done();});
});