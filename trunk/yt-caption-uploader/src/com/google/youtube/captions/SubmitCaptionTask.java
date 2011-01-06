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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.channel.ChannelFailureException;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.util.ServiceException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class SubmitCaptionTask extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(SubmitCaptionTask.class.getName());
  private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
  private ChannelService channelService = ChannelServiceFactory.getChannelService();

  private static final String CAPTION_FEED_URL_FORMAT = "http://gdata.youtube.com/feeds/api/"
      + "videos/%s/captions";
  private static final String CONTENT_TYPE = "application/vnd.youtube.timedtext; charset=UTF-8";
  
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) {
    LOG.info("Starting up...");
    String fileName = null;
    String channelId = null;
    BlobKey blobKey = null;
    
    try {
      String blobKeyString = req.getParameter("blobKey");
      if (Util.isEmptyOrNull(blobKeyString)) {
        throw new IllegalArgumentException("Required parameter 'blobKey' not found.");
      }
      blobKeyString = URLDecoder.decode(blobKeyString, "UTF-8");
      blobKey = new BlobKey(blobKeyString);
      
      String authSubToken = req.getParameter("authSubToken");
      if (Util.isEmptyOrNull(authSubToken)) {
        throw new IllegalArgumentException("Required parameter 'authSubToken' not found.");
      }
      authSubToken = URLDecoder.decode(authSubToken, "UTF-8");
      
      channelId = req.getParameter("channelId");
      if (Util.isEmptyOrNull(channelId)) {
        throw new IllegalArgumentException("Required parameter 'channelId' not found.");
      }
      channelId = URLDecoder.decode(channelId, "UTF-8");
      LOG.fine("Channel id is " + channelId);

      BlobInfoFactory blobInfoFactory = new BlobInfoFactory();
      BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blobKey);

      // Possible valid track name formats:
      // 6VVuLGk8kVU_en.sbv
      // 6VVuLGk8kVU_es_Spanish Track.srt
      fileName = blobInfo.getFilename();

      String regex = "(.{11})_([^_]+?)(?:_(.+))?\\.\\w{3}";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(fileName);
      if (!matcher.matches()) {
        throw new IllegalArgumentException(String.format("Couldn't parse video id and language "
            + "from file name '%s'.", fileName));
      }
      
      String videoId = matcher.group(1);
      String languageCode = matcher.group(2);
      String trackName = matcher.group(3);

      LOG.info(String.format("File name is '%s', videoId is '%s', language code is '%s', track "
          + "name is '%s'.", fileName, videoId, languageCode, trackName));

      YouTubeService service = new YouTubeService(SystemProperty.applicationId.get(),
          Util.DEVELOPER_KEY);
      service.setAuthSubToken(authSubToken);

      String captionsUrl = String.format(CAPTION_FEED_URL_FORMAT, videoId);

      GDataRequest request = service.createInsertRequest(new URL(captionsUrl));
      request.setHeader("Content-Language", languageCode);
      if (trackName != null) {
        request.setHeader("Slug", trackName);
      }
      request.setHeader("Content-Type", CONTENT_TYPE);
      request.getRequestStream().write(blobstoreService.fetchData(blobKey, 0,
          blobInfo.getSize() - 1));
      request.execute();

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
          request.getResponseStream()));
      StringBuilder builder = new StringBuilder();
      String line = null;

      while ((line = bufferedReader.readLine()) != null) {
        builder.append(line);
      }
      bufferedReader.close();

      String responseBody = builder.toString();
      LOG.info("Response to captions request: " + responseBody);
      
      regex = ".+reasonCode=[\"'](.+?)[\"'].+";
      pattern = Pattern.compile(regex);
      matcher = pattern.matcher(responseBody);
      if (matcher.matches()) {
        throw new ServiceException("Caption file was rejected: " + matcher.group(1));
      }
      
      JSONObject jsonResponse = new JSONObject();
      jsonResponse.put("fileName", fileName);
      jsonResponse.put("success", true);
      
      try {
        channelService.sendMessage(new ChannelMessage(channelId, jsonResponse.toString()));
      } catch(ChannelFailureException e) {
        LOG.log(Level.WARNING, "", e);
      }
    } catch (IllegalArgumentException e) {
      reportError(channelId, fileName, e);
    } catch (MalformedURLException e) {
      reportError(channelId, fileName, e);
    } catch (IOException e) {
      reportError(channelId, fileName, e);
    } catch (ServiceException e) {
      reportError(channelId, fileName, e);
    } catch (JSONException e) {
      LOG.log(Level.WARNING, "", e);
    } finally {
      if (blobKey != null) {
        blobstoreService.delete(blobKey);
        LOG.info(String.format("Blob entry '%s' was deleted.", blobKey.getKeyString()));
      }
    }
  }
  
  private void reportError(String channelId, String fileName, Exception exception) {
    LOG.log(Level.WARNING, "", exception);
    
    if (!Util.isEmptyOrNull(channelId) && !Util.isEmptyOrNull(fileName)) {
      try {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("fileName", fileName);
        jsonResponse.put("success", false);
        jsonResponse.put("error", exception.getMessage());
        
        channelService.sendMessage(new ChannelMessage(channelId, jsonResponse.toString()));
      } catch(JSONException e) {
        LOG.log(Level.WARNING, "", e);
      } catch(ChannelFailureException e) {
        LOG.log(Level.WARNING, "", e);
      }
    }
  }
}
