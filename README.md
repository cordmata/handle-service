Handle Server Web API
=====================

This service provides the ability to manage (create, retrieve, update, delete)
URL Handles in a registered [CNRI Handle Server](http://www.handle.net).

* [Overview](#overview)
* [Installation and Configuration](#installation)
* [API Specification](#api)
* [Client Libraries](#clients)
* [Limitations and Potential To-dos](#limits)

<a name="overview" />
Overview
--------

Knowledge of the [Handle System's fundamentals](http://www.handle.net/overviews/system_fundamentals.html)
and [Handle syntax](http://www.handle.net/overviews/system_fundamentals.html#syntax)
is presupposed. Given that, we know that a handle is composed of a *prefix* and a *suffix*
separated by the ASCII slash */*.

URL's used to communicate with the service are constructed as follows:

	https://{service.location}/{prefix}/{suffix}

Given a service location of `example.org/handle-service` and Handle `2286.1/asulib.1007`
the resource URL would be:

	https://example.org/handle-service/2286.1/asulib.1007

**Notice:** It is a good idea to ensure that the suffix portion of the path is
url encoded.

<a name="installation" />
Installation and Configuration
------------------------------

This is a JAX-RS service implemented with [Apache CXF](http://cxf.apache.org/) and is
wired and configured with [Spring](http://www.springsource.org).

[Grab the source](https://github.com/cordmata/handle-service) and modify the file
`handle-server-config.properties` to reflect your registered Handle Server.

You will need to supply the following information to connect to your server.

 * handle.private.key             - Path to your admin private key file.
 * handle.private.key.passphrase  - The private key's passphrase.
 * handle.admin.handle            - Your assigned Admin Handle.
 * handle.key.index               - This is usually 300

If using the standard user service, the following properties will bootstrap an
admin user.

 * handleservice.admin.username
 * handleservice.admin.password

You will need [Maven](http://maven.apache.org/) to build the WAR:

	mvn -Pprod package

Then simply deploy the WAR to a Servlet container of your choice.

### User Management, Authentication and Authorization.

This service can be configured to allow non-administrative users access. These users
can be restricted to specified prefixes or suffix namespaces. If a user is restricted
to specified suffix namespace, they may only administer Handles where the suffix starts
with the specified namespace separated from the rest of the suffix by a period.

If user *benny* is restricted to prefix **1234.0** then he can only manage Handles that look
like `1234.0/{suffix}`. Likewise, if he is restricted to suffix **ben** he can only
create Handles that look like `1234.0/ben.{rest-of-suffix}`.

Out of the box, users can be specified in the Spring context (`src/main/context/beans.xml`
before packaging or `WEB-INF/classes/beans.xml` in the exploded WAR). To add a user, modify
the `userDetailsService` bean in the Spring context. Prefix and suffix restrictions are
specified as a comma-separated list of allowed values. If either `allowedPrefixes` or
`allowedSuffixes` is a '*' then that user will have no restrictions for the associated setting.

```xml
<bean id="userDetailsService" class="edu.asu.lib.handle.PropertiesUserDetailsService">
    <constructor-arg>
        <list>
            <props>
                <prop key="username">handleAdmin</prop>
                <prop key="password">blah</prop>
                <prop key="isAdmin">true</prop>
            </props>
            <props>
                <prop key="username">newUser</prop>
                <prop key="password">superSecret</prop>
                <prop key="isAdmin">false</prop>
                <prop key="isEnabled">true</prop>
                <prop key="allowedPrefixes">1234.5,1234.0</prop>
                <prop key="allowedSuffixes">repo, fass</prop>
            </props>
            <props>
                <!-- Grant user open access to an entire prefix. -->
                <prop key="username">prefixPowerUser</prop>
                <prop key="password">superSecret</prop>
                <prop key="isAdmin">false</prop>
                <prop key="isEnabled">true</prop>
                <prop key="allowedPrefixes">1234.5</prop>
                <prop key="allowedSuffixes">*</prop>
            </props>
        </list>
    </constructor-arg>
</bean>
```

Auth/Auth is implemented with [Spring Security](http://www.springsource.org/spring-security)
so it is possible to create your own UserDetails service which hooks into custom authentication.
You will have to return either a edu.asu.lib.handle.User (or subclass) or an API compatible
UserDetails object.

<a name="api" />
API Specification
-----------------

* [GET/Read](#read) - Look up the target URL for an existing Handle.
* [POST/Create](#create) - Create a new handle for a supplied target URL.
* [PUT/Update](#update) - Update the target URL for an existing Handle.
* [DELETE/Delete](#delete) - Purge the handle.
* [Authentication](#auth)

<a name="read" />
### GET / RETRIEVE

Return the target of the supplied handle in the "location" header.

#### Success Response Code:

* **204 No Content:** The target URL of the supplied handle will be in the "location" header. 

#### Error Response Codes:

* **400 Bad Request:** Handle was not supplied or the supplied target is a Malformed URI.
* **404 Not Found:** The supplied URL Handle was not found. This will also be returned if 
the handle is an Admin or Email handle.
* **401 Unauthorized:** Authentication failure.

#### cURL example:

Command

    $ curl -i -X GET http://handleAdmin:somethingSuperSecret@some.server.edu/handle-service/1234/foo.1

Response

    HTTP/1.1 204 No Content
    Server: Apache-Coyote/1.1
    Set-Cookie: JSESSIONID=5F822D7F1E744DBC09D07E4980F01BC4; Path=/handle-service
    Date: Fri, 05 Oct 2012 21:30:32 GMT
    Location: http://repository.asu.edu/items/15380

<a name="create" />
### POST / CREATE

Create a new handle with the supplied target URI. Returns the created handle url
in the "Location" header.

#### Body Content:

#### Success Response Code:

* **201 Created:** Location header will contain a URL pointing to the main handle
proxy server for the created handle.

#### Error Response Codes:

* **400 Bad Request:** Handle was not supplied or the supplied target is a Malformed URI.
* **409 Conflict:** Handle already exists.
* **401 Unauthorized:** Authentication failure.
* **403 Forbidden** Authorization failure.

#### cURL example:

Command

    $ curl -i -X POST -d "target=http%3A%2F%2Fexample.com" http://handleAdmin:somethingSuperSecret@some.server.edu/handle-service/1234/foo.1

Response

    HTTP/1.1 201 Created
    Server: Apache-Coyote/1.1
    Set-Cookie: JSESSIONID=FB974EDEEF25319E66A6DE85F6A84F69; Path=/handle-service
    Date: Fri, 05 Oct 2012 21:41:59 GMT
    Location: http://hdl.handle.net/2286.9/testme
    Content-Length: 0

<a name="update" />
### P U T  /  U P D A T E

Modify the supplied handle's URL target or create it if it does not exist. The
global handle proxy URL will be returned in the "location" header.


#### Query Parameters:

* **target:**  *Required*, valid URI 

#### Success Response Codes:

* **201 Created:** Handle was not found so one was created.
* **204 No Content:** Handle was updated successfully.

#### Error Response Codes:

* **400 Bad Request:** The supplied target is a Malformed URI.
* **401 Unauthorized:** Authentication failure.
* **403 Forbidden** Authorization failure.

#### cURL example:

Command

    $ curl -i -X PUT http://handleAdmin:somethingSuperSecret@some.server.edu/handle-service/1234/foo.1?target=http%3A%2F%2Fnew.example.com

Response

    HTTP/1.1 204 No Content
    Server: Apache-Coyote/1.1
    Set-Cookie: JSESSIONID=A882E76022607C9BBA0693B803A592C5; Path=/handle-service
    Date: Fri, 05 Oct 2012 21:57:37 GMT
    Location: http://hdl.handle.net/2286.9/testme

<a name="delete" />
### Delete

Delete the supplied handle.

#### Success Response:

* **204 No Content:** Handle was deleted successfully or did not exist.

#### Error Response Codes:

* **401 Unauthorized:** Authentication failure.
* **403 Forbidden** Authorization failure.

#### cURL example:

Command

    $ curl -i -X DELETE http://handleAdmin:somethingSuperSecret@some.server.edu/handle-service/1234/foo.1

Response

    HTTP/1.1 204 No Content
    Server: Apache-Coyote/1.1
    Set-Cookie: JSESSIONID=B26628AB91A447641A1D033733AB3143; Path=/handle-service
    Date: Fri, 05 Oct 2012 22:10:45 GMT

<a name="auth" />
### Authentication

This service is configured to use HTTP Basic Auth and therefore should
only be deployed over HTTPS. Clients should supply their credentials in the
`Authorization` header on each request.

<a name="clients" />
Clients Libraries
-----------------

Existing libraries for interacting with this API.

* Python: https://github.com/cordmata/handle-client

You can also simply use cURL. See the examples above.

<a name="limits" />
Limitations and Potential To-dos
--------------------------------

Currently this service is limited to managing Handles for URL values only. No other
Handle value types can be created, queried, or modified. That means no EMAIL, DESC
or [10320/loc](http://www.handle.net/overviews/handle_type_10320_loc.html)
values can be associated with a Handle.
