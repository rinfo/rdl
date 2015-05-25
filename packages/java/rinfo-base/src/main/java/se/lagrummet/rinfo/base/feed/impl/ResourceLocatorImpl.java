package se.lagrummet.rinfo.base.feed.impl;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.ResourceLocator;
import se.lagrummet.rinfo.base.feed.exceptions.ResourceWriteException;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.concurrent.*;

/**
 * Created by christian on 5/25/15.
 *
 * Simple resource locator. Downloads using input stream and Thread Executors.
 */
public class ResourceLocatorImpl implements ResourceLocator {

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    final int DEFAULT_BUFFER_SIZE = 1000;

    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(60000);
    Executor executor = new ThreadPoolExecutor(100, 200, 20, TimeUnit.SECONDS, queue);
    private URL baseUrl;

    public ResourceLocatorImpl(URL baseUrl) {
        this.baseUrl = baseUrl;
        System.out.println("se.lagrummet.rinfo.base.feed.impl.ResourceLocatorImpl.ResourceLocatorImpl baseUrl="+baseUrl);
    }

    @Override
    public void locate(final Resource resource, final Reply reply) {
        System.out.println("se.lagrummet.rinfo.base.feed.impl.ResourceLocatorImpl.locate "+resource);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream(resource.size() != null ? resource.size() : DEFAULT_BUFFER_SIZE);
                try {
                    final Md5Sum md5Sum = resource.writeTo(buffer, baseUrl);
                    reply.ok(new Data() {
                        @Override public Md5Sum getMd5Sum() {return md5Sum;}
                        @Override public InputStream asInputStream() {return new ByteArrayInputStream(buffer.toByteArray());}

                        @Override
                        public Document asDocument() throws ParserConfigurationException, IOException, SAXException {
                            DocumentBuilder db = dbf.newDocumentBuilder();
                            return db.parse(asInputStream());
                        }
                    });
                } catch (ResourceWriteException e) {
                    reply.failed(Failure.ResourceWrite, e.getMessage());
                }

            }
        });
    }
}
