/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.youtube.captions;

import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import com.google.gdata.client.http.AuthSubUtil;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.youtube.UserProfileEntry;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class AuthSubLogin extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(AuthSubLogin.class.getName());

  private static final String PROFILE_URL = "http://gdata.youtube.com/feeds/api/users/default";

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      String authSubToken = AuthSubUtil.getTokenFromReply(req.getQueryString());
      if (authSubToken == null) {
        throw new IllegalStateException("Could not parse token from AuthSub response.");
      } else {
        authSubToken = URLDecoder.decode(authSubToken, "UTF-8");
      }

      authSubToken = AuthSubUtil.exchangeForSessionToken(authSubToken, null);

      YouTubeService service = new YouTubeService(SystemProperty.applicationId.get(),
          Util.DEVELOPER_KEY);
      service.setAuthSubToken(authSubToken);

      UserProfileEntry profileEntry = service.getEntry(new URL(PROFILE_URL),
          UserProfileEntry.class);
      String username = profileEntry.getUsername();

      String authSubCookie = Util.getCookie(req, Util.AUTH_SUB_COOKIE);
      JSONObject cookieAsJSON = new JSONObject();
      if (!Util.isEmptyOrNull(authSubCookie)) {
        try {
          cookieAsJSON = new JSONObject(authSubCookie);
        } catch (JSONException e) {
          LOG.log(Level.WARNING, "Unable to parse JSON from the existing cookie: " + authSubCookie,
              e);
        }
      }

      try {
        cookieAsJSON.put(username, authSubToken);
      } catch (JSONException e) {
        LOG.log(Level.WARNING, String.format("Unable to add account '%s' and AuthSub token '%s'"
            + " to the JSON object.", username, authSubToken), e);
      }

      Cookie cookie = new Cookie(Util.AUTH_SUB_COOKIE, cookieAsJSON.toString());
      cookie.setMaxAge(Util.COOKIE_LIFETIME);
      resp.addCookie(cookie);

      cookie = new Cookie(Util.CURRENT_AUTHSUB_TOKEN, authSubToken);
      cookie.setMaxAge(Util.COOKIE_LIFETIME);
      resp.addCookie(cookie);

      cookie = new Cookie(Util.CURRENT_USERNAME, username);
      cookie.setMaxAge(Util.COOKIE_LIFETIME);
      resp.addCookie(cookie);
    } catch (IllegalStateException e) {
      LOG.log(Level.WARNING, "", e);
    } catch (AuthenticationException e) {
      LOG.log(Level.WARNING, "", e);
    } catch (GeneralSecurityException e) {
      LOG.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      LOG.log(Level.WARNING, "", e);
    }

    resp.sendRedirect("/");
  }
}
