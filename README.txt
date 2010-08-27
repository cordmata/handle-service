This is a description of the REST-style Handle administration service for 
the CNRI Handle Server.

S E R V I C E  I N T E R F A C E
---------------------------------

Clients that wish to make use of this service will interact via HTTP. HTTP 
methods are mapped to the Handle Server's administrative actions in the 
following manner:

    POST    -> Create
    GET     -> Retrieve 
    PUT     -> Update
    DELETE  -> Delete

The URL represents a handle as a "Resource" as commonly defined by REST; 
that is, the URL is a "Resource Identifier". URL's used to communicate with 
the service are constructed as follows:

* Given the handle 2286/asulib:1007: 
	
	* the Resource Identifier URL is: http://base.url/handle-admin/services/handle/2286%2Fasulib:1007

    * The BASE URL of the service is - http://base.url/handle-admin/services

    * The RESOURCE TYPE IDENTIFIER is - handle
    
    * The HANDLE is - 2206%2Fasulib:1007
    
    	* It is a good idea to encode any special characters in this portion of 
    	the path -- even the "/" separator. Handles not using special 
    	characters may work un-encoded but no guarantee is made.

P O S T  /  C R E A T E 
-------------------------

Create a new handle with the supplied target URI.

Example:
http://base.url/handle-admin/services/handle/2286%2F2212?target=http://foo.bar.com/sometarget.html
	
	Parameters:
		* target - REQUIRED 

	Success Response Code:
		201 Created:
			Location header will contain a URL pointing to the main handle 
			proxy server for the created handle.
		
	Error Response Codes:
		400 Bad Request:
			The supplied target is a Malformed URI.
		401 Unauthorized:
	    	Indicates issue with the supplied private key.
		500 Server Error:

G E T  /  R E T R I E V E
---------------------------

Return the target of the supplied handle in the "location" header.

http://some.server.edu/handle-admin/services/handle/10111%2F2212

        Parameters:
            NONE
            
        Success Response Code:
        	204 No Content:
        		The target URL of the supplied handle will be in the "location"
        		header. 

        Error Response Codes:
			404 Not Found: 
				The supplied Handle is not found.
			401 Unauthorized:
		    	Indicates issue with the supplied private key.
			500 Server Error:


P U T  /  U P D A T E
-----------------------

Modify the supplied handle's URL target. 

http://some.server.edu/handle-admin/handle/10111%2F2212?target=http://foo.bar.com/sometarget.html

	Parameters:
		* target - valid URI - REQUIRED

	Success Response Code:
		204 No Content:
        	Handle was updated successfully.
        	
	Error Response Codes:
		400 Bad Request:
			The supplied target is a Malformed URI.
		401 Unauthorized:
	    	Indicates issue with the supplied private key.
		500 Server Error:

D E L E T E
-------------

Delete the supplied handle.

http://some.server.edu/handle-admin/services/handle/10111%2F2212

	Parameters:
    	NONE
	
	Success Response:
    	204 No Content:
        	Handle was deleted successfully or did not exist.
                  
	Error Response Codes:
		401 Unauthorized:
	    	Indicates issue with the supplied private key.
		500 Server Error:

A U T H E N T I C A T I O N
------------------------------

This service endpoint is protected by HTTP Basic Auth. This is controlled by 
the Servlet container. A user should be created with the role handleAdmin in
the container and the client should supply this user's credentials to gain 
access to the service.
  