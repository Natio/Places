
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

Parse.Cloud.beforeDelete("Posts", function(request, response) {
  query = new Parse.Query("Posts");
  query.equalTo("objectId", request.object.id);
  query.find({
    success: function(post){
        var Deleted_Posts = Parse.Object.extend("Deleted_Posts");
        var delpost = new Deleted_Posts();
        delpost.set("previousObjectId", post[0].get("objectId"));
        delpost.set("previousCreatedAt", post[0].get("createdAt"));
        delpost.set("category", post[0].get("category"));
        delpost.set("fbId", post[0].get("fbId"));
        delpost.set("fbName", post[0].get("fbName"));
        delpost.set("location", post[0].get("location"));
        delpost.set("text", post[0].get("text"));
        delpost.set("weather", post[0].get("weather"));
        delpost.set("audio", post[0].get("audio"));
        delpost.set("picture", post[0].get("picture"));
        delpost.set("video", post[0].get("video"));
        delpost.save(null, {
            success: function(post) {
                // Execute any logic that should take place after the object is saved.
                alert('New object created with objectId: ' + post.id);
                response.success();
            },
            error: function(post, error) {
                // Execute any logic that should take place if the save fails.
                // error is a Parse.Error with an error code and message.
                alert('Failed to create new object, with error code: ' + error.message);
                response.error("An error occurred while deleting the Flag");
            }
        });
    },
    error: function() {
      response.error("An error occurred while deleting the Flag");
    }
  });
});
