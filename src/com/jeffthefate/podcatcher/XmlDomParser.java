package com.jeffthefate.podcatcher;

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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;

import com.jeffthefate.podcatcher.activity.ActivitySearchFeed;
import com.jeffthefate.podcatcher.service.UpdateService;

public class XmlDomParser {

    private String feedAddress;
    private int currSortOrder = -1;
    private HashMap<String, String> episodeHash;
    private ArrayList<HashMap<String, String>> itemList;
    private int numPodcastItems;
    private StringBuilder filename = new StringBuilder();
    private HttpURLConnection connection;
    private int responseCode;
    private byte[] data;
    private int contentLength;
    private OutputStream output;
    private InputStream input;
    private int count;
    private StringBuilder stringOutput = new StringBuilder();
    private StringBuilder toProcess = new StringBuilder();
    private Intent failIntent;
    private String feedTitle;
    private int feedId;
    private StringBuilder cacheLocation = new StringBuilder();
    private boolean sync;
    private Intent updateIntent;
    private Builder nBuilder;
    private NotificationManager nManager;
    private int sortOrder;
    private Parser parser;
    private Document doc;
    private HashMap<String, String> feedHash;
    private Elements elements;
    private ArrayList<String> tagList;
    private long pubDate;
    private long lastPub;
    private Element currEl;
    private Elements pubElements;
    private Element first;
    private int oldEpisodeCount;
    private long lastEpPub;
    private boolean cont;
    private Intent notifyIntent;
    private Elements itemElements;
    private boolean found;
    private ArrayList<HashMap<String, String>> feedList;

    public XmlDomParser(String feedAddress) {
        this.feedAddress = feedAddress;
        itemList = new ArrayList<HashMap<String, String>>();
    }
    /*
     * Download the XML from the parser's address.  Saves it to a temp file if
     * it is a search, otherwise it saves the XML in the feed folder.  Parses
     * the file as a string.
     * 
     * Returns the string or an empty string if there was an error.
     */
    private StringBuilder getXmlFromAddress(boolean isSearch) {
        /* Default to search file */
        responseCode = -1;
        filename.setLength(0);
        filename.append(ApplicationEx.cacheLocation)
                .append(Constants.TEMP_LOCATION)
                .append(Constants.TEMP_NAME);
        File file = new File(filename.toString());
        try {
            /* Delete the old file */
            if (file.exists())
                Util.deleteRecursive(file);
            /* Download the new XML */
            connection = (HttpURLConnection) new URL(feedAddress)
                    .openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            responseCode = connection.getResponseCode();
            contentLength = connection.getContentLength();
            if (contentLength > -1)
                data = new byte[contentLength];
            else
                data = new byte[1024];
            output = new FileOutputStream(filename.toString());
            input = new BufferedInputStream(connection.getInputStream());
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (MalformedURLException e) {
            Log.e(Constants.LOG_TAG, "Unable to create URL from " + feedAddress,
                    e);
            stringOutput.setLength(0);
            return stringOutput;
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Unable to open connection " + feedAddress,
                    e);
            stringOutput.setLength(0);
            return stringOutput;
        }
        /* Parse the file to string */
        stringOutput.setLength(0);
        stringOutput.append(Util.readStringFromExternalFile(filename));
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
            stringOutput.setLength(0);
            return stringOutput;
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
    public ArrayList<HashMap<String, String>> parseXml(boolean isSearch,
            boolean force) {
        /* Parse the XML to a string */
        toProcess = getXmlFromAddress(isSearch);
        /* We need to try and find the podcast and update it */
        if (toProcess == null) {
            failIntent = new Intent(ApplicationEx.getApp(),
                    ActivitySearchFeed.class);
            feedTitle = ApplicationEx.dbHelper.getFeedTitle(feedAddress);
            if (feedTitle != null) {
                failIntent.putExtra(Constants.TERM, feedTitle);
                feedId = ApplicationEx.dbHelper.getFeedId(feedAddress);
                cacheLocation.setLength(0);
                cacheLocation.append(ApplicationEx.cacheLocation)
                        .append(Constants.FEEDS_LOCATION)
                        .append(feedId)
                        .append(File.separator);
                Util.deleteRecursive(new File(cacheLocation.toString()));
                sync = ApplicationEx.isSyncing();
                if (sync) {
                    updateIntent = new Intent(ApplicationEx.getApp(), 
                            UpdateService.class);
                    updateIntent.putExtra(Constants.SYNC, sync);
                    updateIntent.putExtra(Constants.FEED, feedAddress);
                    updateIntent.putExtra(Constants.TITLE, feedTitle);
                    updateIntent.putExtra(Constants.UNSUBSCRIBE, true);
                    ApplicationEx.getApp().startService(updateIntent);
                }
                else
                    ApplicationEx.dbHelper.deleteFeed(feedId);
                nBuilder = new NotificationCompat.Builder(
                        ApplicationEx.getApp());
                nBuilder.setSmallIcon(R.drawable.ic_launcher).
                        setWhen(System.currentTimeMillis()).
                        setContentTitle(Constants.FEED_MISSING).
                        setContentText(Constants.SEARCH_MISSING).
                        setContentIntent(PendingIntent.getActivity(
                                ApplicationEx.getApp(), 0, failIntent, 0));
                nManager = (NotificationManager)
                        ApplicationEx.getApp().getSystemService(
                                Context.NOTIFICATION_SERVICE);
                nManager.notify(null, 5555, nBuilder.build());
            }
            return itemList;
        }
        /* If there is an error return null to indicate the error */
        if (toProcess.length() == 0) {
            return null;
        }
        /* Get the sort order that is determined for the feed; the default is
         * -1 */
        if (force)
            sortOrder = -1;
        else
            sortOrder = ApplicationEx.dbHelper.getInt(feedAddress, 
                    DatabaseHelper.COL_FEED_SORT, DatabaseHelper.FEED_TABLE, 
                    DatabaseHelper.COL_FEED_ADDRESS);
        /* New JSOUP parser */
        parser = new Parser(new XmlTreeBuilder());
        /* Get the DOM doc */
        doc = Jsoup.parse(toProcess.toString(), feedAddress, parser);
        /* Create the default feed hash */
        feedHash = new HashMap<String, String>();
        feedHash.put(Constants.TITLE, null);
        feedHash.put(Constants.ITUNES_AUTHOR, null);
        feedHash.put(Constants.LINK, null);
        feedHash.put(Constants.DESCRIPTION, null);
        feedHash.put(Constants.ITUNES_IMAGE, null);
        feedHash.put(Constants.IMAGE_URL_XML, null);
        feedHash.put(Constants.LAST_BUILD_DATE, null);
        elements = null;
        /* Step through each feed key */
        for (Entry<String, String> entry : feedHash.entrySet()) {
            /* Find the feed key under the channel parent element */
            elements = doc.select(TextUtils.concat(Constants.CHANNEL_XML,
                    entry.getKey()).toString());
            /* The element is found */
            if (elements.size() > 0) {
                if (entry.getKey().equals(Constants.ITUNES_IMAGE))
                    /* Get the attribute instead of just the content */
                    feedHash.put(entry.getKey(), elements.first().attr(Constants.HREF));
                else {
                    /* Only get the image > url if there is no itunes|image */
                    if (entry.getKey().equals(Constants.IMAGE_URL_XML) && 
                            feedHash.get(Constants.ITUNES_IMAGE) != null)
                        continue;
                    feedHash.put(entry.getKey(), elements.first().text());
                }
            }
        }
        /* Otherwise, add the feed hash and move on to parsing the episodes */
        itemList.add(feedHash);
        /* Create the list with all the values to parse in each episode */
        tagList = new ArrayList<String>();
        tagList.add(Constants.URL);
        tagList.add(Constants.PUB_DATE);
        tagList.add(Constants.ITUNES_SUMMARY);
        tagList.add(Constants.DESCRIPTION);
        tagList.add(Constants.TYPE);
        tagList.add(Constants.LENGTH);
        tagList.add(Constants.TITLE);
        tagList.add(Constants.GUID);
        tagList.add(Constants.LINK);
        tagList.add(Constants.ITUNES_DURATION);
        /* Get all the 'item' elements from the doc */
        elements = doc.select(Constants.ITEM);
        /* Holds the number of podcast items found in the document */
        numPodcastItems = 0;
        /* Holds the published date of the current item */
        pubDate = -1;
        /* Get and hold the published date of the last episode */
        lastPub = -1;
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
        oldEpisodeCount = 0;
        switch(sortOrder) {
        case 0:
            /* Step forward through episodes */
            for (int i = 0; i < elements.size(); i++) {
                currEl = elements.get(i);
                /* Get the published date */
                if (currEl != null) {
                    pubElements = currEl.select(Constants.PUB_DATE);
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
                    pubElements = currEl.select(Constants.PUB_DATE);
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
            lastEpPub = -1;
            int forwardOrderNum = 0;
            int backwardOrderNum = 0;
            /* Step forward through episodes */
            for (int i = 0; i < elements.size(); i++) {
                currEl = elements.get(i);
                /* Get the published date */
                if (currEl != null) {
                    pubElements = currEl.select(Constants.PUB_DATE);
                    if (!pubElements.isEmpty()) {
                        first = pubElements.first();
                        if (first != null)
                            pubDate = Util.dateStringToEpoch(first.text());
                    }
                }
                /* Compare with the last date and determine the sort order */
                if (pubDate > lastEpPub)
                    backwardOrderNum++;
                else if (pubDate < lastEpPub || elements.size() == 1)
                    forwardOrderNum++;
                /* Parse the episode, looking for all the tags. Don't keep track
                 * of it it if true is returned */
                cont = parseTags(tagList, elements, i);
                if (cont)
                    continue;
                lastEpPub = pubDate;
            }
            if (forwardOrderNum > backwardOrderNum)
                currSortOrder = 0;
            else if (backwardOrderNum > forwardOrderNum)
                currSortOrder = 1;
            break;
        }
        /* This indicates that the feed is a podcast, so we close the list with
         * an empty map */
        if (numPodcastItems > elements.size()-numPodcastItems) {
            /* Indicate in the notification that we're adding a new feed */
            if (!ApplicationEx.dbHelper.feedExists(feedAddress)) {
                notifyIntent = new Intent(Constants.ACTION_UPDATE_NOTIFICATION);
                notifyIntent.putExtra(Constants.TITLE,
                        feedHash.get(Constants.TITLE));
                ApplicationEx.getApp().sendBroadcast(notifyIntent);
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
        itemElements = elements.get(iter).select(Constants.ENCLOSURE);
        found = false;
        /* Should only be one enclosure element */
        for (Element element : itemElements) {
            Attributes attributes = element.attributes();
            /* If no attributes for enclosure found, not a valid episode */
            if (attributes.size() == 0)
                return true;
            /* If it is audio or video media, then it is a podcast episode,
             * otherwise no episode found */
            if (attributes.get(Constants.TYPE).contains(Constants.AUDIO) || 
                    attributes.get(Constants.TYPE).contains(Constants.VIDEO))
                numPodcastItems++;
            else
                return true;
            /* Grab the values for each tag */
            for(String tag : tagList) {
                /* For these special cases, we have to get attributes of the
                 * enclosure item */
                if (tag.equals(Constants.URL) || tag.equals(Constants.TYPE) || 
                        tag.equals(Constants.LENGTH))
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
        if (attributes.hasKey(Constants.LENGTH) && 
                !attributes.get(Constants.LENGTH).equals(Constants.NULL)) {
            episodeHash.put(tag, attributes.get(tag));
            return true;
        }
        /* Otherwise, we just want the value */
        else if (!attributes.hasKey(Constants.LENGTH)) {
            episodeHash.put(tag, attributes.get(tag));
            return true;
        }
        /* If neither, not a valid episode */
        return false;
    }
    
    protected ArrayList<String> insertFeed(boolean force) {
        /* Get the XML and parse it to get feed and episode information; not a
         * search */
        feedList = parseXml(false, force);
        if (feedList == null) {
            /* Unable to parse XML, add failure - likely connection issue */
            ApplicationEx.dbHelper.addFailedTry(feedAddress);
            /* -1 indicates general failure */
            return null;
        }
        /* Otherwise, success, so reset the fail count */
        ApplicationEx.dbHelper.resetFail(feedAddress);
        /* Empty feedlist indicates no new episodes, so return 0 */
        if (feedList.isEmpty())
            return new ArrayList<String>();
        /* If there are new episodes */
        return Util.insertFeed(feedAddress, feedList, currSortOrder);
    }

}