/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.asu.lib.handle.service;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author dshughe1
 */
public class HandleManagerServlet extends HttpServlet {

    private HandleAuthProvider _authProvider;
    private HandleDAL _hdal;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String endpoint = getServerHandle(request);
        String handle = getResourceHandle(request);

        return;
        // extract server endpoint (e.g. 2286.0) from url
        // extract handle resource from url
        // make call to get the handle resource from the proper server
            // if it exists, return in a format specified by the 'format' param
            // otherwise, return SC_NOT_FOUND
    } 

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String endpoint = getServerHandle(request);
        // extract server endpoint from url
        // generate a new handle for the provided resource
        // arguments: url, file, email, desc

        // targetHandle = handleGenerator.next(serverEndpoint)
        // handleObj = createHandleObj(arguments, targetHandle)
        // get authentication struct from auth provider
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String endpoint = getServerHandle(request);
        // extract server endpoint from url
        // arguments: url, file, email, desc

        // handleObj = createHandleObj(arguments)
        // resolver.update(handleObj)
        // get authentication struct from auth provider
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // redirect to 'PUT' with specific deletion page
    }

    private String getServerHandle(HttpServletRequest req) {
        String pathInfo = req.getPathInfo().replaceFirst("/", "");
        int slashIndex = pathInfo.indexOf("/");
        return (slashIndex < 0) ? pathInfo : pathInfo.substring(0, slashIndex);
    }

    private String getResourceHandle(HttpServletRequest req) {
        String pathInfo = req.getPathInfo().replaceFirst("/", "");
        int slashIndex = pathInfo.indexOf("/");
        return (slashIndex < 0) ? null : pathInfo.substring(slashIndex);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }

    public void setAuthProvider(HandleAuthProvider ap) { _authProvider = ap; }
    public void setHandleDAL(HandleDAL dal) { _hdal = dal; }
}
