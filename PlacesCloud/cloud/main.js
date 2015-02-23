
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
//Parse.Cloud.define("hello", function(request, response) {
//  response.success("Hello world!");
//});

Parse.Cloud.job("userMigration", function(request, status) {
  // Set up to modify user data
  Parse.Cloud.useMasterKey();
  var User = Parse.Object.extend("_User");
  var query = new Parse.Query(User);
  query.limit(100);
  query.find({success:function(results){
	  
	  var map = [];
	  
	  for (var i = 0; i < results.length; i++) { 
	        var object = results[i];
	        var data = object.get('authData');
			//console.log(data["facebook"]["id"]);
			map[data["facebook"]["id"]] = object;
	  }
	  
	  
	  var Posts = Parse.Object.extend("Posts");
	  var query_p = new Parse.Query(Posts);
	  query_p.limit(1000);
	  query_p.doesNotExist("owner");
	  query_p.find({error: function(error){status.error(error);}, 
	  success: function(results){
		  console.log("Migrating "+results.length+" records");
		  for(var k = 0; k < results.length; k++){
			  var obj = results[k];
			  var owner = map[obj.get("fbId")];
			  obj.set("owner", owner);
		  }
		  
		  Parse.Object.saveAll(results , {
		      success: function(list) {
		        status.success("Migration completed successfully");
		      },
		      error: function(error) {
		        status.error(error.message);
		      },
		    });
		  
	  }
  		
  
  	});
 
  	
  },
	error: function(error){status.error(error.message);}});
  
  
});

Parse.Cloud.beforeSave("Posts", function(request, response) {
	
	//If owner is not present it is automatically added
	//in this way old version of the app will continue generating valid falgs
	
	if(request.object.get("owner") == null){
		request.object.set("owner", Parse.User.current());
	}
	
	if (request.object.get("text").length < 5 
    	&& request.object.get("audio") == null
    	&& request.object.get("picture") == null
    	&& request.object.get("video") == null
    	&& request.object.get("phone_media") == null) {
			
    	response.error("Please, write something meaningful ;)");
		
		}
 	   else {
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
        delpost.set("previousObjectId", post[0].id);
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

Parse.Cloud.afterSave("Reported_Posts", function(request, status) {
    var post = request.object.get("reported_flag");
    query = new Parse.Query("Reported_Posts");
    query.equalTo("reported_flag", post);
    query.count({
        success: function(count){
            if(count >= 10){
                alert("Post with id: " + post.id + " has been reported " + count + " times");
            }
        },
        error: function(error){
            console.error("Error while counting reports " + error.code + ": " + error.message);
        }
    });
});
