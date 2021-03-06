
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
//Parse.Cloud.define("hello", function(request, response) {
//  response.success("Hello world!");
//});

function filterDuplicates(array){
    var set = [];
    //console.log(array);
    for(var i = 0; i < array.length; i++){
      var commenter = array[i];
      set[commenter.id] = commenter;
    }
    var values = [];
    for(var key in set){
      values.push(set[key]);
    }
    return values;
}

//courtesy of http://stackoverflow.com/questions/1199352/smart-way-to-shorten-long-strings-with-javascript
String.prototype.trunc = String.prototype.trunc ||
      function(n){
          return this.length>n ? this.substr(0,n-1)+'...' : this;
      };


Parse.Cloud.afterSave("Comments", function(request, response){

  Parse.Cloud.useMasterKey();
  // if(!request.object.isNew()){
  //     // response.success();
  //     return;
  // }

  var currentUser = Parse.User.current();
  var currentUserID = currentUser.id;

  var flagId = request.object.get("flagId");
  var Comments = Parse.Object.extend("Comments");
	var query = new Parse.Query(Comments);
  query.equalTo("flagId", flagId);
  query.notEqualTo("commenter", currentUser);
  //query.include("flag");
  //query.include("owner");
  query.find({
    error: function(error){
			response.error(error);
		},
    success: function(results){
      /*
      console.log("Trovati "+results.length+" destinatari");
      if(results.length == 0){
        //response.success();
        return;
      }
      */

      var Posts = Parse.Object.extend("Posts");
    	var query_post_owner = new Parse.Query(Posts);
      query_post_owner.equalTo("objectId", request.object.get("flag").id);
      query_post_owner.first({
        success: function(first) {
          if(typeof first === "undefined"){
            return;
          }

          var users = [];
          for(var i in results){
            users.push(results[i].get("commenter"));
          }



          if(first.get("owner").id != currentUserID){
            users.push(first.get("owner"));
            console.log("Aggiungo utente corrente first.id: "+first.get("owner") + " currentUserID: "+currentUserID);
          }

          var listOfRecipients = filterDuplicates(users);

          if(listOfRecipients.length == 0){
            console.log("Nessuno a cui inviare le notifiche!!!");
            return;
          }



          console.log(listOfRecipients);
          var query_push = new Parse.Query(Parse.Installation);
          query_push.containedIn("owner", listOfRecipients);
          var alertText = request.object.get("username") + ": " + request.object.get("text").trunc(30);
          Parse.Push.send({
            where: query_push, // Set our Installation query
            collapse_key: "places_push",
            data: {
              alert: request.object.get("text").trunc(30),
              title: "Somebody commented a Flag...",
              commenter: request.object.get("commenter").id,
              commenter_name: request.object.get("username"),
              commented_flag: flagId,
              type: "comment"
            }
          }, {
            success: function() {
              console.log("Notifications sent!");
            },
            error: function(error) {
              console.log(error.message);
            }
          });

        },
        error: function(error) {
          console.log(error.message);
        }
      });

    }

  });
});


Parse.Cloud.beforeSave("Comments", function(request, response) {
  Parse.Cloud.useMasterKey();
    //if the comment is already present DO NOTHING (otherwise an invalid number will be shown to the user)
    if(!request.object.isNew()){
      response.success();
      return;
    }

  if(request.object.get("commenter") == null){
    request.object.set("commenter", Parse.User.current());
  }


	var flagId = request.object.get("flagId");
	var Posts = Parse.Object.extend("Posts");
	var query = new Parse.Query(Posts);
	query.equalTo("objectId", flagId);
	query.find({
		success: function(results){
			if(results.length == 0){
				console.log("Errore 0 flag trovati");
				return;
			}

			var flag = results[0];
			flag.increment("num_comments", 1);
	        Parse.Object.saveAll(results , {
	            success: function(list) {
	              response.success();

	            },
	            error: function(error) {
	              response.error(error.message);
	            },
	          });

	     },
		error: function(error){
			response.error(error.message);
		}
	});

});

Parse.Cloud.beforeSave(Parse.User, function(request, response) {
  //in case we have logged in with Facebook, we add
  //ad entry to the _User table with our Facebook ID
  if(Parse.FacebookUtils.isLinked(request.object)){
    request.object.set("fbId", request.object.get("authData").facebook.id);
    request.object.set("accountType", "fb");
    console.log("User successfully added with fbId: " + request.object.get("fbId"));
  }
  response.success();
});




Parse.Cloud.job("addFbId", function(request, status){
  Parse.Cloud.useMasterKey();
  var User = Parse.Object.extend("_User");
  var query = new Parse.Query(User);
  query.find({success:function(results){
    for(var i = 0; i < results.length; i++){
        if(Parse.FacebookUtils.isLinked(results[i])){
          // console.log("User with fbId: " + results[i].get("authData").facebook.id);
          results[i].set("fbId", results[i].get("authData").facebook.id);
        }
    }
    Parse.Object.saveAll(results , {
      success: function(list) {
        status.success("Facebook IDs added to eligible users");
      },
      error: function(error) {
        status.error(error.message);
      },
    });
  },
  error: function(error){status.error(error.message);}});
});

// Parse.Cloud.job("updateCommenter", function(request, status){
//   Parse.Cloud.useMasterKey();
//   var User = Parse.Object.extend("_User");
//   var query = new Parse.Query(User);
//   query.equalTo("objectId", "AnqfSWzvH3");
//   query.find({success:function(results){
//     var Comments = Parse.Object.extend("Comments");
//     var query_comments = new Parse.Query(Comments);
//     query_comments.incude("flag");
//     query_comments.find({success:function(results){
//       for (var i = 0; i < results.length; i++) {
//         //TODO implement here the user update functionality  
//       }  
//     },
//     error: function(error){status.error(error.message);}
//     });
//   },
//   error: function(error){status.error(error.message);}});
// });


function assignFlagsToTable(comments, index, status){
  if(index >= comments.length){
		Parse.Object.saveAll(comments , {
				success: function(list) {
					status.success("Migration completed successfully");
				},
				error: function(error) {
					status.error(error.message);
				},
			});
		return;
	}
  var current = comments[index];

  var Posts = Parse.Object.extend("Posts");
  var query_p = new Parse.Query(Posts);
  query_p.equalTo("objectId", current.get("flagId"));
  query_p.first({
      success: function(first) {
        current.set("flag", first);
        console.log(first);
        assignFlagsToTable(comments, index+1, status);
      },
      error: function(error) {
        status.error(error.message);
      },
    })


}

Parse.Cloud.job("addFlagsToComments", function(request, status) {
  var Comments = Parse.Object.extend("Comments");
  var query = new Parse.Query(Comments);
  query.doesNotExist("flag");
  query.limit(200);
  query.find({
		error: function(error){
			status.error(error.message);
		},
		success: function (results){
      assignFlagsToTable(results, 0, status);
		}
	});

});

Parse.Cloud.job("addFlagsToWoWs", function(request, status) {
  var WoWs = Parse.Object.extend("Wow_Lol_Boo");
  var query = new Parse.Query(WoWs);
  query.doesNotExist("flag");
  query.limit(200);
  query.find({
    error: function(error){
      status.error(error.message);
    },
    success: function (results){
      assignFlagsToTable(results, 0, status);
    }
  });

});

function getCommentsAndSetCommenter(comments, index, status){
	if(index >= comments.length){
		Parse.Object.saveAll(comments , {
				success: function(list) {

					status.success("Migration completed successfully");
				},
				error: function(error) {
					status.error(error.message);
				},
			});
		return;
	}

	var comment = comments[index];
	var fbId = comment.get("userId");


	var U = Parse.Object.extend("_User");
	var query = new Parse.Query(U);
	query.equalTo("fbId", fbId);
	query.first({success:function(item){
		comment.set("commenter", item);

		getCommentsAndSetCommenter(comments, index+1, status);

	}, error: function(error){ status.error(error.message); } });


}


Parse.Cloud.job("addCommenterToComments", function(request, status) {

	Parse.Cloud.useMasterKey();

  var Comments = Parse.Object.extend("Comments");
  var query = new Parse.Query(Comments);
	query.doesNotExist("commenter");
  query.limit(200);

	query.find({
		error: function(error){
			status.error(error.message);
		},
		success: function (results){
			getCommentsAndSetCommenter(results, 0, status);
		}
	});

});


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
  if(!request.object.isNew()){
    response.success();
    return;
  }


  if(request.object.get("owner") == null){
    request.object.set("owner", Parse.User.current());
  }
  request.object.set("num_comments", 0);

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

    if(request.object.get("uniqueId") == null){
      response.success();
      return;
    }

    var query = new Parse.Query(Parse.Installation);
    query.equalTo("uniqueId", request.object.get("uniqueId"));
    query.notEqualTo("installationId", request.object.get("installationId"));
    query.first().then(function(duplicate) {
        if (typeof duplicate === "undefined") {
            console.log("Duplicate does not exist,New installation");
            response.success();
        } else {
            console.log("Duplicate exist..Trying to delete " + duplicate.id);
            duplicate.destroy().then(function(duplicate) {
                console.log("Successfully deleted duplicate");
                response.success();
            }, function(error) {
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

Parse.Cloud.afterDelete("Posts", function(request) {
  var query = new Parse.Query("Comments");
  query.equalTo("flag", {
        __type: "Pointer",
        className: "Posts",
        objectId: request.object.id
    });
 
  query.find().then(function(comments) {
    console.log("Comments being deleted: " + comments.length);
    return Parse.Object.destroyAll(comments);
  }).then(function(success) {
    console.log("Successfully deleted posts");
  }, function(error) {
    console.error("Error deleting related comments: " + error.code + ": " + error.message);
  });

  var wowQuery = new Parse.Query("Wow_Lol_Boo");
  wowQuery.equalTo("flag", {
        __type: "Pointer",
        className: "Posts",
        objectId: request.object.id
    });
  wowQuery.find().then(function(wows) {
    console.log("WoWs being deleted: " + wows.length);
    return Parse.Object.destroyAll(wows);
  }).then(function(success) {
    console.log("Successfully deleted wows");
  }, function(error) {
    console.error("Error deleting related wows: " + error.code + ": " + error.message);
  });
});

Parse.Cloud.afterDelete("Comments", function(request) {
  var Deleted_Comments = Parse.Object.extend("Deleted_Comments");
  var delcomments = new Deleted_Comments();
  var delcomment_original = request.object;
  delcomments.set("previousObjectId", delcomment_original.id);
  delcomments.set("text", delcomment_original.get("text"));
  delcomments.set("userId", delcomment_original.get("userId"));
  delcomments.set("username", delcomment_original.get("username"));
  delcomments.set("commenter", delcomment_original.get("commenter"));
  delcomments.set("flag", delcomment_original.get("flag"));
  delcomments.save(null, {
      success: function(post) {
          // Execute any logic that should take place after the object is saved.
          alert('New deleted comment entry created with objectId: ' + post.id);
      },
      error: function(post, error) {
          // Execute any logic that should take place if the save fails.
          // error is a Parse.Error with an error code and message.
          alert('Failed to create new deleted comment entry, with error code: ' + error.message);
      }
  });
});

Parse.Cloud.afterDelete("Wow_Lol_Boo", function(request) {
  var Deleted_WoWs = Parse.Object.extend("Deleted_WoW");
  var delwows = new Deleted_WoWs();
  var delwow_original = request.object;
  delwows.set("previousObjectId", delwow_original.id);
  delwows.set("user", delwow_original.get("user"));
  delwows.set("flag", delwow_original.get("flag"));
  delwows.save(null, {
      success: function(post) {
          // Execute any logic that should take place after the object is saved.
          alert('New deleted wow entry created with objectId: ' + post.id);
      },
      error: function(post, error) {
          // Execute any logic that should take place if the save fails.
          // error is a Parse.Error with an error code and message.
          alert('Failed to create new deleted wow entry, with error code: ' + error.message);
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


// ========== Google Plus login ==========

var googleClientId = '575576178991-d4ri2g8ril72jmkb91fg6vjqolafs13k.apps.googleusercontent.com';  //The client ID obtained from the Google Developers Console
var googleClientSecret = 'YOUR-GOOGLE-CLIENT-SECRET';   //The client secret obtained from the Google Developers Console

var googleValidateEndpoint = 'https://www.googleapis.com/oauth2/v1/userinfo'; //this is the only verification link you need to verify the user's Google Access Token

var googleRedirectEndpoint = 'https://google.com/login/oauth/authorize?'; //not used (handled client side)
var googleUserEndpoint = 'https://api.google.com/user';           //not used (Google has HTTP requests for the user profile using their User ID but they're not necessary here, and this isn't the right address)


/**
 * In the Data Browser, set the Class Permissions for these 2 classes to
 *   disallow public access for Get/Find/Create/Update/Delete operations.
 * Only the master key should be able to query or write to these classes.
 */
var TokenRequest = Parse.Object.extend("TokenRequest");
var TokenStorage = Parse.Object.extend("TokenStorage");

/**
 * Create a Parse ACL which prohibits public access.  This will be used
 *   in several places throughout the application, to explicitly protect
 *   Parse User, TokenRequest, and TokenStorage objects.
 */
var restrictedAcl = new Parse.ACL();
restrictedAcl.setPublicReadAccess(false);
restrictedAcl.setPublicWriteAccess(false);

/**
 * Load needed modules.
 */
var express = require('express');
var querystring = require('querystring');
var _ = require('underscore');
var Buffer = require('buffer').Buffer;

/**
 * Create an express application instance
 */
var app = express();

/**
 * Global app configuration section
 */
app.set('views', 'cloud/views');  // Specify the folder to find templates
app.set('view engine', 'ejs');    // Set the template engine
app.use(express.bodyParser());    // Middleware for reading request body


/**
 * OAuth Callback route.
 *
 * This is intended to be accessed via redirect from Google.  The request
 *   will be validated against a previously stored TokenRequest and against
 *   another Google endpoint, and if valid, a User will be created and/or
 *   updated with details from Google.  A page will be rendered which will
 *   'become' the user on the client-side and redirect to the /main page.
 */
Parse.Cloud.define('accessGoogleUser', function(req, res) {
  console.log("Validating request...");

  var data = req.params;
  var token;
  /**
   * Validate that code and state have been passed in as query parameters.
   * Render an error page if this is invalid.
   */
  if (!(data && data.code)) {
    res.error('Invalid auth response received.');
    return;
  }
  Parse.Cloud.useMasterKey();
  Parse.Promise.as().then(function() {
    // Validate & Exchange the code parameter for an access token from Google
    var result = getGoogleAccessToken(data.code);
    console.log("Access token validated: " + JSON.stringify(result));
    return result;
  }).then(function(httpResponse) {
    var userData = httpResponse.data;
    if (userData && userData.id) {
      return upsertGoogleUser(token, userData, data.email);
    } else {
      return Parse.Promise.error("Unable to parse Google data");
    }
  }).then(function(user) {
    /**
     * Send back the session token in the response to be used with 'become/becomeInBackground' functions
     */
    res.success(user.getSessionToken());
  }, function(error) {
    /**
     * If the error is an object error (e.g. from a Parse function) convert it
     *   to a string for display to the user.
     */
    if (error && error.code && error.error) {
      error = error.code + ' ' + error.error;
    }
    res.error(JSON.stringify(error));
  });

});

/**
 * This function is called when Google redirects the user back after
 *   authorization.  It calls back to Google to validate and exchange the code
 *   for an access token.
 */
var getGoogleAccessToken = function(code) {
  var body = querystring.stringify({
    access_token: code
  });
  return Parse.Cloud.httpRequest({
    url: googleValidateEndpoint + '?access_token=' + code
  });
}

/**
 * This function checks to see if this Google user has logged in before.
 * If the user is found, update the accessToken (if necessary) and return
 *   the users session token.  If not found, return the newGoogleUser promise.
 */
var upsertGoogleUser = function(accessToken, googleData, emailId) {
  console.log("I'm checking access token validity");

  var query = new Parse.Query(TokenStorage);
  query.equalTo('accountId', googleData.id);

  //query.ascending('createdAt');
  // Check if this googleId has previously logged in, using the master key
  return query.first({ useMasterKey: true }).then(function(tokenData) {
    // If not, create a new user.
    if (!tokenData) {
      return newGoogleUser(accessToken, googleData, emailId);
    }
    // If found, fetch the user.
    var user = tokenData.get('user');
    return user.fetch({ useMasterKey: true }).then(function(user) {
      // Update the access_token if it is different.
      if (accessToken !== tokenData.get('access_token')) {
        tokenData.set('access_token', accessToken);
      }
      /**
       * This save will not use an API request if the token was not changed.
       * e.g. when a new user is created and upsert is called again.
       */
      return tokenData.save(null, { useMasterKey: true });
    }).then(function(obj) {
      // Return the user object.
      return Parse.Promise.as(user);
    });
  });
}

/**
 * This function creates a Parse User with a random login and password, and
 *   associates it with an object in the TokenStorage class.
 * Once completed, this will return upsertGoogleUser.  This is done to protect
 *   against a race condition:  In the rare event where 2 new users are created
 *   at the same time, only the first one will actually get used.
 */
var newGoogleUser = function(accessToken, googleData, email) {
  var user = new Parse.User();
  // Generate a random username and password.
  var username = new Buffer(24);
  var password = new Buffer(24);
  _.times(24, function(i) {
    username.set(i, _.random(0, 255));
    password.set(i, _.random(0, 255));
  });
  var name = googleData.name;
  name = name.split(" ");
  var firstName = name[0];
  if(name.length > 1)
  var lastName = name[name.length-1];
  user.set("username", username.toString('base64'));
  user.set("password", password.toString('base64'));
  user.set("email", email);
  // user.set("first_name", firstName);
  // user.set("last_name", lastName);
  user.set("name", firstName + " " + lastName);
  user.set("accountType", 'g+');
  // Sign up the new User
  return user.signUp().then(function(user) {
    // create a new TokenStorage object to store the user+Google association.

    console.log("Inserting new entry in TokenStorage table with access token: " + accessToken);

    var ts = new TokenStorage();
    ts.set('user', user);
    ts.set('accountId', googleData.id);
    ts.set('access_token', accessToken);
    ts.setACL(restrictedAcl);
    // Use the master key because TokenStorage objects should be protected.
    return ts.save(null, { useMasterKey: true });
  }).then(function(tokenStorage) {
    return upsertGoogleUser(accessToken, googleData);
  });
}


/**
 * This function calls the googleUserEndpoint to get the user details for the
 * provided access token, returning the promise from the httpRequest.
 * UNUSED: Android
 */
var getGoogleUserDetails = function(accessToken) {
  return Parse.Cloud.httpRequest({
    method: 'GET',
    url: googleUserEndpoint,
    params: { access_token: accessToken },
    headers: {
      'User-Agent': 'Parse.com Cloud Code'
    }
  });
}


/**
 * Google specific details, including application id and secret
 */

/**
 * Logged in route.
 *
 * JavaScript will validate login and call a Cloud function to get the users
 *   Google details using the stored access token.
 */
app.get('/main', function(req, res) {
  res.render('main', {});
});

/**
 * Attach the express app to Cloud Code to process the inbound request.
 */
app.listen();

/**
 * Main route.
 *
 * When called, render the login.ejs view
 */
app.get('/', function(req, res) {
  res.render('login', {});
});

/**
 * Login with Google route.
 *
 * When called, generate a request token and redirect the browser to Google.
 */
app.get('/authorize', function(req, res) {

  var tokenRequest = new TokenRequest();
  // Secure the object against public access.
  tokenRequest.setACL(restrictedAcl);
  /**
   * Save this request in a Parse Object for validation when Google responds
   * Use the master key because this class is protected
   */
  tokenRequest.save(null, { useMasterKey: true }).then(function(obj) {
    /**
     * Redirect the browser to Google for authorization.
     * This uses the objectId of the new TokenRequest as the 'state'
     *   variable in the Google redirect.
     */
    res.redirect(
      googleRedirectEndpoint + querystring.stringify({
        client_id: googleClientId,
        state: obj.id
      })
    );
  }, function(error) {
    // If there's an error storing the request, render the error page.
    res.render('error', { errorMessage: 'Failed to save auth request.'});
  });

});
