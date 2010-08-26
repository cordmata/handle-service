This is a description of the REST-style Handle administration interface for 
the CNRI Handle Server.

C L I E N T   I N T E R F A C E
---------------------------------

Clients that wish to make use of this interface will interact via HTTP. HTTP 
methods are mapped to the Handle Server's administrative actions in the 
following manner:

    POST    -> Create
    GET     -> Retrieve 
    PUT     -> Update
    DELETE  -> Delete

URL's used to communicate with the service are constructed as follows:

Given the complete RESOURCE IDENTIFIER:
    http://base.url/handle-admin/handle/10111/2212

    The BASE URL of the service is - http://base.url/handle-admin/

    The RESOURCE TYPE IDENTIFIER is - handle/

    The HANDLE is - 10111/2212

P O S T  /  C R E A T E 
-------------------------

Create a new handle and return it in the Location header of the response. While 
you are not required to provide parameters, it is suggested that "url" or "file"
parameters be supplied with a valid URI (an error will result if the URI 
supplied is malformed). Providing no parameters will produce a HS_ADMIN admin 
record.

Example:
http://some.server.edu/handle-admin/handle/10111/2212?url=http://foo.bar.com/sometarget.html
	
        Parameters:
		url=valid URI - OPTIONAL
		file=valid URI - OPTIONAL
                email=Email address - OPTIONAL
                desc=Description - OPTIONAL

	Error Response Codes:
		400 Bad Request: 
                    The supplied target is a Malformed URI.
		401 Unauthorized: 
                    Passphrase was not supplied or was wrong (see 
                    Authentication). Various messages from Handle system.
		502 Bad Gateway:
                    Various messages from Handle system.
	
	Success Response Code:
		201 Created:
                    Location header will contain the created handle.

G E T  /  R E T R I E V E
---------------------------

Return info about the supplied handle in XML.

http://some.server.edu/handle-admin/handle/10111/2212

        Parameters:
                NONE

        Error Response Codes:
		404 Not Found: 
                    The supplied Handle is not found.

        Success Response:
                XML containing the Handle values.

P U T  /  U P D A T E
-----------------------

Modify the supplied handle. The value supplied to the handle's type-value 
parameter will overwrite the corresponding handle's type-value. If the 
type-value supplied does not exist in the requested handle then no action will 
be taken and the Handle will remain unchanged. If you need to add or remove 
handle-values which are not pre-existing then you should delete the handle and 
recreate it with the additional or removed handle values. 

http://some.server.edu/handle-admin/handle/10111/2212?url=http://foo.bar.com/sometarget.html?email=hdladmin@uiuc.edu

        Parameters:
		url=valid URI - OPTIONAL
		file=valid URI - OPTIONAL
                email=Email address - OPTIONAL
                desc=Description - OPTIONAL

        Error Response Codes:
		400 Bad Request: 
                    The supplied target is a Malformed URI.
		401 Unauthorized: 
                    Passphrase was not supplied or was wrong (see 
                    Authentication). Various messages from Handle system.
		502 Bad Gateway:
                    Various messages from Handle system.
	
	Success Response Code:
		204 No Content:
                    Handle was updated successfully.

D E L E T E
-------------

Delete the supplied handle.

http://some.server.edu/handle-admin/handle/10111/2212
        Parameters:
                NONE

        Error Response Codes:
		401 Unauthorized:
                    Passphrase was not supplied or was wrong (see 
                    Authentication). Various messages from Handle system.
		502 Bad Gateway:
                    Various messages from Handle system.

        Success Response:
                204 No Content:
                    Handle was deleted successfully.

A U T H E N T I C A T I O N
------------------------------

Authentication information should supplied in the Authorization header and 
follows HTTP conventions. The format is:

username:base-64-encoded-password

Because the application is aware of the public-key of the handle service, the 
username in this application will be ignored so any value can be supplied and it
will be valid. The password that needs to be Base64 encoded and placed after 
the colon(:) is the passphrase which corresponds to the Handle Server's 
public/private key pair. More information can be found at 
https://www-s2.library.uiuc.edu/bluestem-docs/handle/handle.html. 

BASIC authorization is the only scheme currently supported (we can deploy on an 
HTTPS server if needed).