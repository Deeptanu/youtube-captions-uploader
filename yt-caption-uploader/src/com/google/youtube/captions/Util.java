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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class Util {
  public static final String AUTH_SUB_COOKIE = "AuthSubCookie";
  public static final String CURRENT_AUTHSUB_TOKEN = "CurrentAuthsubToken";
  public static final String CURRENT_USERNAME = "CurrentUsername";
  public static final String CHANNEL_ID = "ChannelId";
  public static final int COOKIE_LIFETIME = 365 * 24 * 60 * 60; // 365 days, in seconds.
  // You MUST register for a developer key at
  // http://code.google.com/apis/youtube/dashboard/gwt/
  // and enter the key below prior to using this code!
  public static final String DEVELOPER_KEY = "REPLACE_ME!";
  
  public static boolean isEmptyOrNull(String string) {
    if (string == null || string.equals("")) {
      return true;
    } else {
      return false;
    }
  }
  
  public static String getCookie(HttpServletRequest req, String cookieName) {
    for (Cookie cookie : req.getCookies()) {
      if (cookie.getName().equals(cookieName)) {
        return cookie.getValue();
      }
    }
    
    return null;
  }
}
