# How to use yt-captions-uploader #

The public demo at [yt-captions-uploader.appspot.com](http://yt-captions-uploader.appspot.com/) is a working example of a caption uploader for YouTube.

To upload captions to a video, you must be the video owner.

The name of the caption file provides the video ID, and sets parameters like _Title_ and _Language_ in YouTube.

This uploader only works with caption files in srt and sbv formats. You cannot use it to align plain transcripts.

## Authenticating to YouTube ##
To upload captions, you must first log into YouTube and grant permission to the yt-captions-uploader.

  1. Open [yt-captions-uploader](http://yt-captions-uploader.appspot.com/) in your browser.
  1. In the top right corner, click **Add Account**.
    * If your Google Account is already signed into YouTube, you'll be directed to the YouTube Authentication page. If this isn't the YouTube account that the captioned videos belong to, log out of YouTube and log in as the owner of the video that you want to upload captions for.
    * If you haven't connected your Google Account and your YouTube account, you'll be redirected to a login page for YouTube. Log in as the owner of the video that you want to upload captions for.
  1. On the YouTube Authentication page, click **Allow Access** to permit yt-captions-uploader to post captions to your videos. The browser returns to yt-captions-uploader, where you should now see your YouTube channel name under _Current Account_.
  1. Optionally add additional accounts you have access to, and switch accounts using the **Current Account** popup menu.

YouTube access credentials are stored in a local browser cookie following authentication, and are not exposed for any other users of the web application. You can invalidate the local credentials by using the **Remove Current Account** link.

## Naming Your Caption Files ##
The filename is used to tell the uploader which video, language, and track title to use.

Use this syntax:
'videoID\_langID.srt' or 'videoID\_langID.sbv'

or, optionally, with a track title:

'videoID\_langID\_title.srt' or 'videoID\_langID\_title.srt'

Note that the video ID is an 11-character string, and may contain underscore characters. The language ID is the ISO 639-1 two-letter language code ([or locale](http://code.google.com/apis/youtube/2.0/reference.html#Localized_Category_Lists), such as pt-BR)that identifies the desired caption language.

So the file '5q3cwvbwX\_c\_en.srt' would upload as an English caption track for this video (http://www.youtube.com/watch?v=5q3cwvbwX_c).

If we uploaded it as '5q3cwvbwX\_c\_en\_ASL.srt' or '5q3cwvbwX\_c\_en\_ASL.sbv', it would create an English caption track named "ASL" for the same video.

And if the file were a Chinese translation (Simplified Chinese), we would name it '5q3cwvbwX\_c\_zh-CN.srt' or '5q3cwvbwX\_c\_zh-CN.sbv'.


## Uploading Captions ##
  1. Once you've named your files appropriately, click **Add Files** to add them from your computer. The selected files are shown in the upload queue.
  1. When you've done adding files, click **Start upload** to begin adding them to YouTube. When the upload is complete for each file, you'll see status below the upload queue.

If you want to add more files, refresh the page to clear the upload queue and start over.