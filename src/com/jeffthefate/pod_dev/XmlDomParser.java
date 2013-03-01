package com.jeffthefate.pod_dev;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.XmlTreeBuilder;
import org.jsoup.select.Elements;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jeffthefate.pod_dev.activity.ActivitySearchFeed;
import com.jeffthefate.pod_dev.service.UpdateService;

public class XmlDomParser {

    private String feedAddress;
    private String address;
    private long currSortOrder;
    HashMap<String, String> episodeHash;
    ArrayList<HashMap<String, String>> itemList;
    private int numPodcastItems;

    public XmlDomParser(String feedAddress) {
        this.feedAddress = feedAddress;
        if (this.feedAddress.startsWith("feed/"))
            address = this.feedAddress.substring("feed/".length());
        else if (this.feedAddress.startsWith("http"))
            address = this.feedAddress;
        itemList = new ArrayList<HashMap<String, String>>();
    }
    /*
     * Download the XML from the parser's address.  Saves it to a temp file if
     * it is a search, otherwise it saves the XML in the feed folder.  Parses
     * the file as a string.
     * 
     * Returns the string or an empty string if there was an error.
     */
    private String getXmlFromAddress(boolean isSearch) {
        /* Default to search file */
        int responseCode = -1;
        String filename = ApplicationEx.cacheLocation +
                Constants.TEMP_LOCATION + "temp.xml";
        File file = new File(filename);
        try {
            int fileSize = 0;
            /* Delete the old file */
            if (file.exists()) {
                fileSize = (int) file.length();
                Util.deleteRecursive(file);
            }
            /* Download the new XML */
            HttpURLConnection connection = (HttpURLConnection) 
                    new URL(address).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            responseCode = connection.getResponseCode();
            byte data[] = new byte[1024];
            OutputStream output = new FileOutputStream(filename);
            InputStream input = new BufferedInputStream(
                    connection.getInputStream());
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (MalformedURLException e) {
            Log.e(Constants.LOG_TAG, "Unable to create URL from " + address, e);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Unable to open connection " + address, e);
        }
        /* Parse the file to string */
        String stringOutput = Util.readStringFromExternalFile(filename);
        /* Delete the temp file if it is a search */
        if (isSearch)
            Util.deleteRecursive(file);
        /* No error, return the string */
        if (stringOutput != null)
            return stringOutput;
        /* The feed is bad if we get 404 from the address */
        else if (responseCode == 404)
            return null;
        /* Otherwise return an empty string to indicate an error */
        else {
            return "";
        }
    }
    /*
     * Returns a list of hashes
     * The first hash is the feed hash containing:
     *      title
     *      itunes|author
     *      link
     *      description
     *      itunes|image
     *      image > url
     *      lastBuildDate
     * The next hashes are each episodes containing:
     *      url
     *      pubDate
     *      itunes|summary
     *      description
     *      type
     *      length
     *      title
     *      link
     *      itunes|duration
     * The last hash is always empty
     * 
     * Returns null if there was an error parsing the XML for some reason.
     */
    public ArrayList<HashMap<String, String>> parseXml(boolean isSearch) {
        /* Parse the XML to a string */
        String output = getXmlFromAddress(isSearch);
        /* We need to try and find the podcast and update it */
        if (output == null) {
            Intent failIntent = new Intent(ApplicationEx.getApp(),
                    ActivitySearchFeed.class);
            String title = ApplicationEx.dbHelper.getFeedTitle(feedAddress);
            if (title != null) {
                failIntent.putExtra("term", title);
                int id = ApplicationEx.dbHelper.getFeedId(feedAddress);
                Util.deleteRecursive(new File(ApplicationEx.cacheLocation +
                        Constants.FEEDS_LOCATION + id + File.separator));
                boolean sync = ApplicationEx.isSyncing();
                if (sync) {
                    Intent intent = new Intent(ApplicationEx.getApp(), 
                            UpdateService.class);
                    intent.putExtra("sync", sync);
                    intent.putExtra("feed", feedAddress);
                    intent.putExtra("title", title);
                    intent.putExtra("unsubscribe", true);
                    ApplicationEx.getApp().startService(intent);
                }
                else
                    ApplicationEx.dbHelper.deleteFeed(id);
            }
            VersionedNotificationBuilder nBuilder =
                    VersionedNotificationBuilder.newInstance();
            nBuilder.create(ApplicationEx.getApp()).
                    setSmallIcon(R.drawable.ic_launcher).
                    setWhen(System.currentTimeMillis()).
                    setContentTitle("Feed Missing").
                    setContentText("Touch this to search for a replacement").
                    setContentIntent(PendingIntent.getActivity(
                            ApplicationEx.getApp(), 0, failIntent, 0));
            Notification notification = nBuilder.getNotification();
            NotificationManager nManager = 
                (NotificationManager) ApplicationEx.getApp().getSystemService(
                        Context.NOTIFICATION_SERVICE);
            nManager.notify(null, 5555, notification);
            return itemList;
        }
        /* If there is an error return null to indicate the error */
        if (output.equals(""))
            return null;
        /* Get the sort order that is determined for the feed; the default is
         * -1 */
        int sortOrder = ApplicationEx.dbHelper.getInt(feedAddress, 
                DatabaseHelper.COL_FEED_SORT, DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ADDRESS);
        /* New JSOUP parser */
        Parser parser = new Parser(new XmlTreeBuilder());
        /* Get the DOM doc */
        Document doc = Jsoup.parse(output, address, parser);
        /* Create the default feed hash */
        HashMap<String, String> feedHash = new HashMap<String, String>();
        feedHash.put("title", null);
        feedHash.put("itunes|author", null);
        feedHash.put("link", null);
        feedHash.put("description", null);
        feedHash.put("itunes|image", null);
        feedHash.put("image > url", null);
        feedHash.put("lastBuildDate", null);
        Elements elements = null;
        /* Step through each feed key */
        for (Entry<String, String> entry : feedHash.entrySet()) {
            /* Find the feed key under the channel parent element */
            elements = doc.select("channel > " + entry.getKey());
            /* The element is found */
            if (elements.size() > 0) {
                if (entry.getKey().equals("itunes|image"))
                    /* Get the attribute instead of just the content */
                    feedHash.put(entry.getKey(), elements.first().attr("href"));
                else {
                    /* Only get the image > url if there is no itunes|image */
                    if (entry.getKey().equals("image > url") && 
                            feedHash.get("itunes|image") != null)
                        continue;
                    feedHash.put(entry.getKey(), elements.first().text());
                }
            }
        }
        /* Otherwise, add the feed hash and move on to parsing the episodes */
        itemList.add(feedHash);
        /* Create the list with all the values to parse in each episode */
        ArrayList<String> tagList = new ArrayList<String>();
        tagList.add("url");
        tagList.add("pubDate");
        tagList.add("itunes|summary");
        tagList.add("description");
        tagList.add("type");
        tagList.add("length");
        tagList.add("title");
        tagList.add("link");
        tagList.add("itunes|duration");
        /* Get all the 'item' elements from the doc */
        elements = doc.select("item");
        /* Holds the number of podcast items found in the document */
        numPodcastItems = 0;
        /* Holds the published date of the current item */
        long pubDate = -1;
        /* Get and hold the published date of the last episode */
        long lastPub = -1;
        /* If this is from a search, default to -1 sort order */
        if (!isSearch)
            lastPub = ApplicationEx.dbHelper.getFeedLastEpTime(
                    ApplicationEx.dbHelper.getInt(feedAddress, 
                DatabaseHelper.COL_FEED_ID, 
                DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ADDRESS));
        else
            sortOrder = -1;
        /* Sort order values:
         *      0   newest item to oldest (correct to the spec)
         *      1   oldest to newest
         *      -1  unknown
         */
        Element currEl;
        Elements pubElements;
        Element first;
        int oldEpisodeCount = 0;
        switch(sortOrder) {
        case 0:
            /* Step forward through episodes */
            for (int i = 0; i < elements.size(); i++) {
                currEl = elements.get(i);
                /* Get the published date */
                if (currEl != null) {
                    pubElements = currEl.select("pubDate");
                    if (!pubElements.isEmpty()) {
                        first = pubElements.first();
                        if (first != null)
                            pubDate = Util.dateStringToEpoch(first.text());
                    }
                }
                /* Compare with the last published date from database */
                if (pubDate <= lastPub) {
                    oldEpisodeCount++;
                    if (oldEpisodeCount > 2 && itemList.size() <= 1) {
                        /* If there is no new episode, based on the published date,
                         * end the list and return it */
                        itemList.add(new HashMap<String, String>());
                        return itemList;
                    }
                }
                else
                    lastPub = pubDate;
                /* Parse the episode, looking for all the tags */
                parseTags(tagList, elements, i);
            }
            break;
        case 1:
            /* Step backward through episodes */
            for (int i = elements.size()-1; i >= 0; i--) {
                currEl = elements.get(i);
                /* Get the published date */
                if (currEl != null) {
                    pubElements = currEl.select("pubDate");
                    if (!pubElements.isEmpty()) {
                        first = pubElements.first();
                        if (first != null)
                            pubDate = Util.dateStringToEpoch(first.text());
                    }
                }
                /* Compare with the last published date from database */
                if (pubDate <= lastPub && itemList.size() <= 1) {
                    /* If there is no new episode, based on the published date,
                     * end the list and return it */
                    itemList.add(new HashMap<String, String>());
                    return itemList;
                }
                /* Parse the episode, looking for all the tags */
                parseTags(tagList, elements, i);
            }
            break;
        case -1:
            /* Determines the sort order only */
            long lastEpPub = -1;
            /* Step forward through episodes */
            for (int i = 0; i < elements.size(); i++) {
                currEl = elements.get(i);
                /* Get the published date */
                if (currEl != null) {
                    pubElements = currEl.select("pubDate");
                    if (!pubElements.isEmpty()) {
                        first = pubElements.first();
                        if (first != null)
                            pubDate = Util.dateStringToEpoch(first.text());
                    }
                }
                /* Compare with the last date and determine the sort order */
                if (pubDate > lastEpPub)
                    currSortOrder = 1;
                else if (pubDate < lastEpPub || elements.size() == 1)
                    currSortOrder = 0;
                /* Parse the episode, looking for all the tags. Don't keep track
                 * of it it if true is returned */
                boolean cont = parseTags(tagList, elements, i);
                if (cont)
                    continue;
                lastEpPub = pubDate;
            }
            break;
        }
        /* This indicates that the feed is a podcast, so we close the list with
         * an empty map */
        if (numPodcastItems > elements.size()-numPodcastItems) {
            /* Indicate in the notification that we're adding a new feed */
            if (!ApplicationEx.dbHelper.feedExists(feedAddress)) {
                Intent intent = new Intent(
                        Constants.ACTION_UPDATE_NOTIFICATION);
                intent.putExtra("title", feedHash.get("title"));
                ApplicationEx.getApp().sendBroadcast(intent);
            }
            itemList.add(new HashMap<String, String>());
        }
        return itemList;
    }
    
    private boolean parseTags(ArrayList<String> tagList, Elements elements, 
            int iter) {
        /* Clear the episode hash */
        episodeHash = new HashMap<String, String>();
        /* Select the enclosure item for the current episode
         * Ex: <enclosure url="http://www.kuow.org/podcast/Conversation/ConversationA20121012.mp3" length="7131971" type="audio/mpeg"/>
         */
        Elements itemElements = elements.get(iter).select("enclosure");
        boolean found = false;
        /* Should only be one enclosure element */
        for (Element element : itemElements) {
            Attributes attributes = element.attributes();
            /* If no attributes for enclosure found, not a valid episode */
            if (attributes.size() == 0)
                return true;
            /* If it is audio or video media, then it is a podcast episode,
             * otherwise no episode found */
            if (attributes.get("type").contains("audio") || 
                    attributes.get("type").contains("video"))
                numPodcastItems++;
            else
                return true;
            /* Grab the values for each tag */
            for(String tag : tagList) {
                /* For these special cases, we have to get attributes of the
                 * enclosure item */
                if (tag.equals("url") || tag.equals("type") || 
                        tag.equals("length"))
                    found = getEpisodes(attributes, tag);
                /* Otherwise we get the value text for each tag, if any */
                else {
                    Elements tagElements = elements.get(iter).select(tag);
                    if (tagElements.size() > 0)
                        episodeHash.put(tag, tagElements.first().text());
                    else
                        episodeHash.put(tag, null);
                }
            }
            /* Add this episode to the list if it is valid */
            if (found) {
                itemList.add(episodeHash);
                break;
            }
            /* Otherwise, no episode found */
            else
                return true;
        }
        return false;
    }
    
    private boolean getEpisodes(Attributes attributes, String tag) {
        /* Looking for the length value to make sure it isn't null */
        if (attributes.hasKey("length") && 
                !attributes.get("length").equals("null")) {
            episodeHash.put(tag, attributes.get(tag));
            return true;
        }
        /* Otherwise, we just want the value */
        else if (!attributes.hasKey("length")) {
            episodeHash.put(tag, attributes.get(tag));
            return true;
        }
        /* If neither, not a valid episode */
        return false;
    }
    
    protected ArrayList<String> insertFeed() {
        /* Get the XML and parse it to get feed and episode information; not a
         * search */
        ArrayList<HashMap<String, String>> feedList = parseXml(false);
        if (feedList == null) {
            /* Unable to parse XML, add failure - likely connection issue */
            ApplicationEx.dbHelper.addFailedTry(feedAddress, 
                    Constants.REASON_CONNECTION);
            /* -1 indicates general failure */
            return null;
        }
        /* Otherwise, success, so reset the fail count */
        ApplicationEx.dbHelper.resetFail(feedAddress);
        /* Empty feedlist indicates no new episodes, so return 0 */
        if (feedList.isEmpty()) {
            return new ArrayList<String>();
        }
        /* If there are new episodes */
        return Util.insertFeed(feedAddress, feedList, currSortOrder);
    }

}