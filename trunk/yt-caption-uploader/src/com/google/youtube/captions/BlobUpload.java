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

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class BlobUpload extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(BlobUpload.class.getName());
  private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String authSubToken = Util.getCookie(req, Util.CURRENT_AUTHSUB_TOKEN);
    String authSubCookie = Util.getCookie(req, Util.AUTH_SUB_COOKIE);
    String channelId = Util.getCookie(req, Util.CHANNEL_ID);

    try {
      if (Util.isEmptyOrNull(authSubToken)) {
        throw new IllegalArgumentException(String.format("Cookie '%s' is null or empty.",
            Util.CURRENT_AUTHSUB_TOKEN));
      }
      authSubToken = URLDecoder.decode(authSubToken, "UTF-8");
      
      if (Util.isEmptyOrNull(authSubCookie)) {
        throw new IllegalArgumentException(String.format("Cookie '%s' is null or empty.",
            Util.AUTH_SUB_COOKIE));
      }
      
      if (Util.isEmptyOrNull(channelId)) {
        throw new IllegalArgumentException(String.format("Cookie '%s' is null or empty.",
            Util.CHANNEL_ID));
      }

      Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(req);

      Queue queue = QueueFactory.getDefaultQueue();
      for (Map.Entry<String, BlobKey> entry : blobs.entrySet()) {
        String blobKey = entry.getValue().getKeyString();
        LOG.info("Scheduling caption submission for caption in blob " + blobKey);

        queue.add(Builder
            .withUrl("/SubmitCaptionTask")
            .param("blobKey", blobKey)
            .param("authSubToken", authSubToken)
            .param("channelId", channelId));
      }
    } catch (IllegalArgumentException e) {
      LOG.log(Level.WARNING, "", e);
    }

    resp.sendRedirect("/");
  }
}
