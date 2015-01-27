
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
