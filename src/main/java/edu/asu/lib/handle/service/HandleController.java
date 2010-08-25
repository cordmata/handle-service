/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.asu.lib.handle.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.Configuration;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.ResolutionResponse;
import net.handle.hdllib.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author dshughe1
 */
public class HandleController implements Controller {

    private static Log log = LogFactory.getLog(HandleController.class);
    private DocumentBuilder _docBuilder;
    private Transformer _transformer;
    private HandleAuthProvider _authProvider;
    private HandleDAL _handleDAL;
    private String _authHandle;
    private int _authIndex;

    public void setAuthProvider(HandleAuthProvider ap) { _authProvider = ap; }
    public void setHandleDAL(HandleDAL dal) { _handleDAL = dal; }
    public void setAuthHandle(String authHandle) { _authHandle = authHandle; }
    public void setAuthIndex(int authIndex) { _authIndex = authIndex; }

    public void initXmlComponents() throws Exception {
        if(_docBuilder == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            _docBuilder = dbf.newDocumentBuilder();
        }

        if(_transformer == null) {
            System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            _transformer = t;
        }
    }

    public ModelAndView handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        testTransactions();
        String method = req.getMethod();
        if("GET".equals(method)) doGet(req, resp);
        /*else if("POST".equals(method)) doPost(req, resp);
        else if("PUT".equals(method)) doPut(req, resp);
        else if("DELETE".equals(method)) doDelete(req, resp);*/
        return null;
    }

    private void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        log.debug("Starting HTTP GET.");

        String server = getServerHandle(request);
        String handle = getResourceHandle(request);
        String fullHandle = String.format("%s/%s", server, handle);

        try {
            HandleResolver res = new HandleResolver();
            Configuration cfg = res.getConfiguration();
            ResolutionRequest req = new ResolutionRequest(fullHandle.getBytes("UTF8"), null, null, null);

            AbstractResponse resp = res.processRequest(req);
            log.debug("Handle ResolutionRequest processed.");

            // The responseCode value for a response indicates the status of the
            // request.  A successful resolution will always return RC_SUCCESS.
            // Failed resolutions could return one of several response codes,
            // including RC_ERROR, RC_SERVER_TOO_BUSY, and RC_HANDLE_NOT_FOUND.
            if (resp.responseCode == AbstractMessage.RC_SUCCESS) {
                initXmlComponents();

                // Create main response XML element
                Document doc = _docBuilder.newDocument();
                Element responseElt = doc.createElement("response");
                doc.appendChild(responseElt);

                responseElt.setAttribute("status", "1");
                responseElt.setAttribute("message", AbstractMessage.getResponseCodeMessage(resp.responseCode));

                // Append handle values to response
                for(HandleValue hVal : ((ResolutionResponse) resp).getHandleValues()) {
                    Element hElt = doc.createElement("handle-value");
                    hElt.setAttribute("index", Integer.toString(hVal.getIndex()));
                    hElt.setAttribute("data", hVal.getDataAsString());
                    hElt.setAttribute("type", hVal.getTypeAsString());
                    responseElt.appendChild(hElt);
                }

                // Write reponse out to servlet response object
                log.debug("Attempting to write the DOM document to a Writer.");
                response.setContentType("text/xml");
                _transformer.transform(new DOMSource(doc), new StreamResult(response.getWriter()));
                _handleDAL.logEvent(server, "GET", request.getRemoteAddr(), handle, "");
                log.debug("Wrote a DOM Document to a PrintWriter.");
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, AbstractMessage.getResponseCodeMessage(resp.responseCode));
            }

        } catch (Exception ex) {
            log.error(ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    private void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            log.debug("Starting HTTP POST.");
            String server = getServerHandle(request);

            String target = null;
            String email = null;
            String desc = null;

            Map params = request.getParameterMap();
            if (params.containsKey("url")){
                target = request.getParameter("url");
                log.debug("Set URL.");
            }

            if (params.containsKey("email")){
                email = request.getParameter("email");
                log.debug("Set EMAIL.");
            }

            if (params.containsKey("desc")){
                desc = request.getParameter("desc");
                log.debug("Set DESC.");
            }

            PublicKeyAuthenticationInfo auth = _authProvider.getAuthenticationInfo(request);
            if (auth == null) {
                response.setHeader("WWW-Authenticate", "BASIC realm=\"handle-admin\"");
                response.sendError(response.SC_UNAUTHORIZED);
                return;
            }

            HandleResolver resolver = new HandleResolver();
            log.debug("Handle Resolver retrieved.");

            // We don't want to create a handle without an admin value-- otherwise
            // we would be locked out.  Give ourselves all permissions, even
            // ones that only apply for NA handles.
            AdminRecord admin = new AdminRecord(_authHandle.getBytes("UTF8"), _authIndex, true, true, true, true, true, true, true, true, true, true, true, true);
            log.debug("AdminRecord Created");

            // All handle values need a timestamp, so get the current time in
            // seconds since the epoch
            int timestamp = (int) (System.currentTimeMillis() / 1000);

            // argument is the value's index, 100 in this case.  The second
            // argument is the value's type.  The third argument holds the value's
            // data.  Since this is binary data, not textual data like a URL, we have
            // to encode it first.
            //
            // The fourth argument indicates whether the time to live for the
            // value is absolute or relative.  The fifth argument is the time to
            // live, 86400 seconds(24 hours) in this case.  The sixth argument is
            // the timestamp we created earlier.  The seventh argument is a
            // ValueReference array.  You will almost always want to leave this
            // null; read the RFC's for more information.  The last four arguments
            // are the permissions for the value: admin read, admin write, public
            // read, and public write.
            //
            // whew!
            Vector handVals = new Vector();
            log.debug("Created Vector to hold HandleValues");

            handVals.add(new HandleValue(100, "HS_ADMIN".getBytes("UTF8"), Encoder.encodeAdminRecord(admin), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false));
            log.debug("Added HS_ADMIN");

            if (target != null){
                handVals.add(new HandleValue(101, "URL".getBytes("UTF8"), target.getBytes("UTF8"), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false));
                log.debug("Added URL");
            }

            if (email != null){
                  handVals.add(new HandleValue(102, "EMAIL".getBytes("UTF8"), email.getBytes("UTF8"), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false));
                  log.debug("Added EMAIL");
            }

            if (desc != null){
                  handVals.add(new HandleValue(103, "DESC".getBytes("UTF8"), desc.getBytes("UTF8"), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false));
                  log.debug("Added Description");
            }

            HandleValue[] val = (HandleValue[]) handVals.toArray(new HandleValue[handVals.size()]);
            log.debug("Converted Vector to Array");

            // Now we can build our CreateHandleRequest object.  As its first
            // parameter it takes the handle we are going to create.  The second
            // argument is the array of initial values the handle should have.
            // The final argument is the authentication object that should be
            // used to gain permission to perform the creation.
            String handle = getNextHandle(server);
            String fullHandle = String.format("%s/%s", server, handle);
            CreateHandleRequest req = new CreateHandleRequest(fullHandle.getBytes("UTF8"), val, auth);

            // Finally, we are ready to send the message.  We do this by calling
            // the processRequest method of the resolver object with the request
            // object as an argument.  The result is returned as either a
            // GenericResponse or ErrorResponse object.  It is important to note that
            // a failed resolution will not throw an exception, only return a
            // ErrorResponse.
            AbstractResponse resp = resolver.processRequest(req);
            log.debug("CreateHandleRequest processed. Response code: " + AbstractMessage.getResponseCodeMessage(resp.responseCode));

            // The responseCode value for a response indicates the status of
            // the request.  A successful resolution will always return
            // RC_SUCCESS.  Failed resolutions could return one of several
            // response codes, including RC_ERROR, RC_INVALID_ADMIN, and
            // RC_INSUFFICIENT_PERMISSIONS.
            if (resp.responseCode == AbstractMessage.RC_SUCCESS) {
                response.setStatus(response.SC_CREATED);
                response.setHeader("Location", "http://hdl.handle.net/" + handle);
                _handleDAL.logEvent(server, "POST", request.getRemoteAddr(), handle, "");
                return;
            } else {
                response.sendError(response.SC_BAD_GATEWAY, AbstractMessage.getResponseCodeMessage(resp.responseCode));
                return;
            }
        } catch (HandleException ex) {
            response.sendError(response.SC_BAD_GATEWAY, ex.getMessage());
            log.error(ex.getMessage());
        } catch (SQLException ex) {
            String errorMsg = "Error accessing handle seed data: " + ex.getMessage();
            response.sendError(response.SC_INTERNAL_SERVER_ERROR, errorMsg);
            log.error(errorMsg);
        }
    }

    private void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            log.debug("Starting HTTP PUT.");
            String server = getServerHandle(request);
            String handle = getResourceHandle(request);
            String fullHandle = String.format("%s/%s", server, handle);

            String target = null;
            String email = null;
            String desc = null;

            Map params = request.getParameterMap();

            if (params.containsKey("url")){
                target = request.getParameter("url");
                log.debug("Set URL");
            }

            if (params.containsKey("email")){
                email = request.getParameter("email");
                log.debug("Set EMAIL");
            }

            if (params.containsKey("desc")){
                desc = request.getParameter("desc");
                log.debug("Set DESC");
            }

            PublicKeyAuthenticationInfo auth = _authProvider.getAuthenticationInfo(request);
            if (auth == null) {
                response.setHeader("WWW-Authenticate", "BASIC realm=\"handle-admin\"");
                response.sendError(response.SC_UNAUTHORIZED);
                return;
            }

            HandleResolver resolver = new HandleResolver();
            log.debug("Handle Resolver retrieved.");

            // first handle the removal requests
            ResolutionRequest req = new ResolutionRequest(fullHandle.getBytes("UTF8"), null, null, null);

            AbstractResponse resp = resolver.processRequest(req);
            log.debug("Handle ResolutionRequest processed.");

            // The responseCode value for a response indicates the status of the
            // request.  A successful resolution will always return RC_SUCCESS.
            // Failed resolutions could return one of several response codes,
            // including RC_ERROR, RC_SERVER_TOO_BUSY, and RC_HANDLE_NOT_FOUND.
            if (resp.responseCode == AbstractMessage.RC_SUCCESS) {

                ResolutionResponse rresp = (ResolutionResponse) resp;
                HandleValue[] values = rresp.getHandleValues();

                // modify the entries indicated
                for (int i = 0; i < values.length; i++) {
                    //check to see if there is a value with type URL or FILE
                    if ("URL".equalsIgnoreCase(values[i].getTypeAsString())){
                        values[i].setData(target.getBytes("UTF8"));
                    }

                    if ( email != null && "EMAIL".equalsIgnoreCase(values[i].getTypeAsString())){
                        values[i].setData(email.getBytes("UTF8"));
                    }

                    if (desc != null && "DESC".equalsIgnoreCase(values[i].getTypeAsString())){
                        values[i].setData(desc.getBytes("UTF8"));
                    }
                }

                ModifyValueRequest modreq = new ModifyValueRequest(rresp.handle, values, auth);
                AbstractResponse modresp = resolver.processRequest(modreq);
                if(modresp!=null && modresp.responseCode == AbstractMessage.RC_SUCCESS) {
                    response.setStatus(response.SC_NO_CONTENT);
                    _handleDAL.logEvent(server, "PUT", request.getRemoteAddr(), handle, target);
                } else {
                    response.sendError(response.SC_BAD_GATEWAY,  AbstractMessage.getResponseCodeMessage(modresp.responseCode));
                    log.error(AbstractMessage.getResponseCodeMessage(modresp.responseCode));
                    return;
                }
            } else {
                response.sendError(response.SC_BAD_GATEWAY,  AbstractMessage.getResponseCodeMessage(resp.responseCode));
                log.error(AbstractMessage.getResponseCodeMessage(resp.responseCode));
                return;
            }

        } catch (HandleException ex) {
            response.sendError(response.SC_BAD_GATEWAY, ex.getMessage());
            log.error(ex.getMessage());
        }
    }

    private void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            log.debug("Starting HTTP DELETE.");
            String server = getServerHandle(request);
            String handle = getResourceHandle(request);
            String fullHandle = String.format("%s/%s", server, handle);

            PublicKeyAuthenticationInfo auth = _authProvider.getAuthenticationInfo(request);
            if (auth == null) {
                response.setHeader("WWW-Authenticate", "BASIC realm=\"handle-admin\"");
                response.sendError(response.SC_UNAUTHORIZED);
                return;
            }

            HandleResolver resolver = new HandleResolver();
            log.debug("Handle Resolver retrieved.");

            DeleteHandleRequest req = new DeleteHandleRequest(Util.encodeString(fullHandle), auth);
            AbstractResponse resolveResponse = resolver.processRequest(req);
            String mess = AbstractMessage.getResponseCodeMessage(resolveResponse.responseCode);
            log.debug("DeleteHandleRequest processed. Response code: " + mess);
            if (resolveResponse == null || resolveResponse.responseCode != AbstractMessage.RC_SUCCESS) {
                log.error(mess);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "error deleting handle: " + handle + " -- " + mess);
                return;
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                _handleDAL.logEvent(server, "DELETE", request.getRemoteAddr(), handle, "");
                return;
            }
        } catch (HandleException ex) {
            log.error(ex.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, ex.getMessage());
            return;
        }
    }

    /*
     * Returns the parts of a handle in the format [server, handle, fullHandle (i.e. server/handle)]
     */
    private String[] getHandleParts(HttpServletRequest req) {
       String pathInfo = req.getPathInfo().replaceFirst("/", "");
       int slashIndex = pathInfo.indexOf("/");
       if(slashIndex < 0)
           return new String[] {null, null, pathInfo};
       else
           return new String[] {pathInfo.substring(0, slashIndex), pathInfo.substring(slashIndex + 1), pathInfo};
    }

    private String getServerHandle(HttpServletRequest req) {
        return getHandleParts(req)[0];
    }

    private String getResourceHandle(HttpServletRequest req) {
        return getHandleParts(req)[1];
    }

    private String getNextHandle(String server) throws SQLException {
        long seed = _handleDAL.getNextSeed(server);
        System.out.println("Seed: " + seed);
        return createAlphabeticHandle(seed);
    }

    private String createAlphanumericHandle(long seed) {
        return Long.toString(seed, 36);
    }

    private String createAlphabeticHandle(long seed) {
        String targetAlphabet = "abcdefghijklmnopqrstuvwxyz";
        String base26Alphabet = "0123456789abcdefghijklmnop";
        String base26Conversion = Long.toString(seed, 26);
        String target = "";
        for(int i = 0; i < base26Conversion.length(); ++i) {
            char currentChar = base26Conversion.charAt(i);
            int index = base26Alphabet.indexOf(currentChar);
            target += targetAlphabet.charAt(index);
        }
        return target;
    }

    private void testTransactions() {
        final HandleDAL hdal = _handleDAL;
        final HashMap testMap = new HashMap();
        final int maxRuns = 100;
        int numThreads = 25;

        for(int i = 0; i < numThreads; ++i) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    for(int i = 0; i < maxRuns; ++i) {
                        try {
                            String seed = getNextHandle("test");
                            synchronized(testMap) {
                                if(testMap.containsKey(seed))
                                    System.out.println("Transaction error!");
                                testMap.put(seed, seed);
                            }
                        }
                        catch(Exception ex) {
                            System.out.println("Error: " + ex);
                        }
                    }
                }
            });
            t.start();
        }
    }
}