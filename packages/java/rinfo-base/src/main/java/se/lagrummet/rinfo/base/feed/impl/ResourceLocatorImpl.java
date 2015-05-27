package se.lagrummet.rinfo.base.feed.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.Report;
import se.lagrummet.rinfo.base.feed.ResourceLocator;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;
import se.lagrummet.rinfo.base.feed.util.Utils;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.*;

/**
 * Created by christian on 5/25/15.
 *
 * Simple resource locator. Downloads using input stream and Thread Executors.
 */
public class ResourceLocatorImpl implements ResourceLocator {

    Logger log = LoggerFactory.getLogger(ResourceLocatorImpl.class);

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    static {
        disableCertificateCheck();
    }

    final int DEFAULT_BUFFER_SIZE = 1000;

    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100000);
    Executor executor = new ThreadPoolExecutor(100, 200, 20, TimeUnit.SECONDS, queue);
    private URL baseUrl;
    private Report report;

    public ResourceLocatorImpl(URL baseUrl, Report report) {
        this.report = report;
        log.debug("created baseUrl={}",baseUrl);
        this.baseUrl = baseUrl;
    }

    @Override
    public void locate(final Resource resource, final Reply reply) {
        log.debug("resource={}", resource);
        executor.execute(new MyRunner(reply, resource));
    }

    private class MyRunner implements Runnable {
        private final Reply reply;
        private final Resource resource;

        public MyRunner(Reply reply, Resource resource) {
            this.reply = reply;
            this.resource = resource;
        }

        @Override
        public void run() {
            MyResourceWriter resourceWriter = new MyResourceWriter(DEFAULT_BUFFER_SIZE, reply, resource);
            resource.configure(resourceWriter);
            boolean retry = true;
            while (retry)
            try {
                retry = false;
                resourceWriter.download();
            } catch (IOException e) {
                if (resourceWriter.shouldRetry()) {
                    resourceWriter.increaseRetryCount();
                    retry = true;
                    resource.intermediate(report, "retry");
                } else {
                    resource.intermediate(report, "retry FAILED");
                    resourceWriter.failed(e);
                }
            }
        }
    }

    private class MyResourceWriter implements ResourceWriter, Data {
        private ByteArrayOutputStream buffer;
        private int size;
        private Reply reply;
        private Resource resource;
        private Md5Sum md5Sum;
        private String url;
        private int retryCount = 0;

        public MyResourceWriter(int size, Reply reply, Resource resource) {
            this.size = size;
            this.reply = reply;
            this.resource = resource;
        }

        @Override public Resource getResource() {return resource;}
        @Override public void setUrl(String url) {this.url = url;}
        @Override public void setSize(int size) {
            this.size = size;
        }

        public void download() throws IOException {
            URL targetUrl = null;
            long start = 0;
            try {
                targetUrl = Utils.parse(baseUrl, url);
                start = System.currentTimeMillis();
                InputStream in = new BufferedInputStream(targetUrl.openStream());
                buffer = new ByteArrayOutputStream(size);
                OutputStream out = new BufferedOutputStream(buffer);
                Md5Sum.Md5SumCalculator md5SumCalculator = Md5Sum.calculator();
                Utils.copyStream(in, out, md5SumCalculator);
                in.close();
                out.close();
                md5Sum = md5SumCalculator.create();
                reply.ok(this);
            } finally {
                long duration = System.currentTimeMillis() - start;
                log.debug("Download time {} was {}msec", targetUrl, duration);
            }
        }

        @Override public Md5Sum getMd5Sum() {return md5Sum;}
        @Override public InputStream asInputStream() {return new ByteArrayInputStream(buffer.toByteArray());}
        @Override public Document asDocument() throws ParserConfigurationException, IOException, SAXException {return dbf.newDocumentBuilder().parse(asInputStream());}

        public boolean shouldRetry() {
            return retryCount < 5;
        }

        public void increaseRetryCount() {
            retryCount++;
        }

        public void failed(Exception e) {
            reply.failed(Failure.ResourceWrite, e.getMessage());
        }
    }

    private static void disableCertificateCheck() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

}
