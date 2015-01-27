
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
//Parse.Cloud.define("hello", function(request, response) {
//  response.success("Hello world!");
//});

Parse.Cloud.beforeSave("Posts", function(request, response) {
  if (request.object.get("text").length < 5) {
    response.error("Please, write something meaningful ;)");
  } else {
    response.success();
  }
});

Parse.Cloud.beforeSave(Parse.Installation, function(request, response) {
    Parse.Cloud.useMasterKey();
    var query = new Parse.Query(Parse.Installation);
    query.equalTo("owner", request.user);
    query.equalTo("uniqueID", request.object.get("uniqueID"));
    query.first().then(function(duplicate) {
        if (typeof duplicate === "undefined") {
            console.log("Duplicate does not exist,New installation");
            response.success();
        } else {
            console.log("Duplicate exist..Trying to delete " + duplicate.id);
            duplicate.destroy().then(function(duplicate) {
                console.log("Successfully deleted duplicate");
                response.success();
            }, function() {
                console.log(error.code + " " + error.message);
                response.success();
            });

        }
    }, function(error) {
        console.warn(error.code + error.message);
        response.success();
    });
});
