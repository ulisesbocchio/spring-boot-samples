// Call the console.log function. npm i less --save-dev
var less = require('less');
//process.stdout.write(process.argv[2]);

less.logger.addListener({
                            debug: function(msg) {
                                console.log("DEBUG: " + msg);
                            },
                            info: function(msg) {
                                console.log("INFO: " + msg);
                            },
                            warn: function(msg) {
                                console.log("WARN: " + msg);
                            },
                            error: function(msg) {
                                console.log("ERROR: " + msg);
                            }
                        });

var options = {};

less.render(process.argv[2], options)
    .then(function(output) {
              process.stdout.write(output.css);
          },
          function(error) {
              console.log(error);
              process.exit(1);
          });
