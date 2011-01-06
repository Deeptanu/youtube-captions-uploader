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

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class GenerateChannelToken extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(GenerateChannelToken.class.getName());
  private ChannelService channelService = ChannelServiceFactory.getChannelService();
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String channelId = String.format("%s-%d", Util.getCookie(req, Util.CURRENT_USERNAME),
        System.currentTimeMillis());
    // There's a not-very-well-documented limit on 64 characters for the channel id.
    if (channelId.length() >= 64) {
      channelId = channelId.substring(0, 63);
    }
    
    String token = channelService.createChannel(channelId);
    LOG.fine(String.format("Channel id is '%s' and token is '%s'.", channelId, token));
    
    Cookie cookie = new Cookie(Util.CHANNEL_ID, channelId);
    resp.addCookie(cookie);
    
    resp.setContentType("text/plain");
    resp.getWriter().print(token);
  }
}