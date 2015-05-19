package se.lagrummet.rinfo.base.feed

import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.HttpResponse
import org.apache.http.message.BasicNameValuePair

//import org.apache.http.impl.client.HttpClientBuilder
//import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FeedUpdatePingNotifyer implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(FeedUpdatePingNotifyer.class)

    URL feedUrl
    Collection pingTargets

    FeedUpdatePingNotifyer(feedUrl, pingTargets) {
        this.feedUrl = feedUrl
        this.pingTargets = pingTargets
    }

    public void run() {
        for (pingTarget in pingTargets) {
            doPing(pingTarget, feedUrl)
        }
    }

    void doPing(URL pingTarget, URL feedUrl) {
        logger.info("${pingTarget} ${feedUrl}")

        /*
            Credentials defaultcreds = new UsernamePasswordCredentials("username", "password");
            client.getState().setCredentials(new AuthScope("myhost", 80, AuthScope.ANY_REALM), defaultcreds);
         */

        HttpClient http = new DefaultHttpClient(); //HttpClientBuilder.create().build();

        try {
            HttpResponse response
            if (feedUrl) {
                HttpPost post = new HttpPost(pingTarget.toString())
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1)
                nameValuePairs.add(new BasicNameValuePair("feed",feedUrl.toString()))
                post.setEntity(new UrlEncodedFormEntity(nameValuePairs))
                response = http.execute(post)
            } else {
                response = http.execute(new HttpGet(pingTarget.toString()))
            }
            if (response.statusLine.statusCode < 200 || response.statusLine.statusCode >= 300) {
                logger.error("Failed code ${response.statusLine.statusCode} because ${response.statusLine.reasonPhrase}")
                return
            }
            logger.info("Success ${response.statusLine.statusCode}")
        } catch (MalformedURLException e) {
            logger.error("Malformed url becase ${e.message}", e)
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("Input Output error caused the request to fail because ${e.message}", e)
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Request failed because ${e.message}", e)
            e.printStackTrace();
        }
    }



}
