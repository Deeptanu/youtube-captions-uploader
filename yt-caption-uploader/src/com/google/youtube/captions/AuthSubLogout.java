/* Copyright 2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */

package com.google.youtube.captions;

import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import com.google.gdata.client.http.AuthSubUtil;
import com.google.gdata.util.AuthenticationException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class AuthSubLogout extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(AuthSubLogout.class.getName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String username = Util.getCookie(req, Util.CURRENT_USERNAME);
    
    if (!Util.isEmptyOrNull(username)) {
      try {
        String authSubCookie = Util.getCookie(req, Util.AUTH_SUB_COOKIE);
        JSONObject cookieAsJSON = new JSONObject(authSubCookie);

        if (cookieAsJSON.has(username)) {
          String authSubToken = cookieAsJSON.getString(username);

          AuthSubUtil.revokeToken(authSubToken, null);

          cookieAsJSON.remove(username);

          Cookie cookie = new Cookie(Util.AUTH_SUB_COOKIE, cookieAsJSON.toString());
          cookie.setMaxAge(Util.COOKIE_LIFETIME);
          resp.addCookie(cookie);
          
          cookie = new Cookie(Util.CURRENT_AUTHSUB_TOKEN, "");
          cookie.setMaxAge(Util.COOKIE_LIFETIME);
          resp.addCookie(cookie);
          
          cookie = new Cookie(Util.CURRENT_USERNAME, "");
          cookie.setMaxAge(Util.COOKIE_LIFETIME);
          resp.addCookie(cookie);
        }
      } catch (AuthenticationException e) {
        LOG.log(Level.WARNING, "", e);
      } catch (GeneralSecurityException e) {
        LOG.log(Level.WARNING, "", e);
      } catch (JSONException e) {
        LOG.log(Level.WARNING, "", e);
      }
    }

    resp.sendRedirect("/");
  }
}
