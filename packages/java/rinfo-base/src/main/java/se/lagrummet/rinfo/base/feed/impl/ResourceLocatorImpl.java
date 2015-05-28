package se.lagrummet.rinfo.base.feed.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.Report;
import se.lagrummet.rinfo.base.feed.ResourceLocator;
import se.lagrummet.rinfo.base.feed.exceptions.MD5SumVerificationFailedException;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;
import se.lagrummet.rinfo.base.feed.util.Utils;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.SocketException;
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

    private BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100000);
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 200, 20, TimeUnit.SECONDS, queue);
    private URL baseUrl;
    private Report report;
    private long totalCopied;

    public ResourceLocatorImpl(URL baseUrl, Report report) {
        this.report = report;
        log.debug("created baseUrl={}",baseUrl);
        this.baseUrl = baseUrl;
    }

    @Override
    public void locate(final Resource resource, final Reply reply) {
        log.debug("resource={}", resource);
        new MyRunner(reply, resource).enqueue();
    }

    @Override
    public void printStatusOneLiner() {
        System.out.println("Queue: "+queue.size()+" bytes copied: "+getTotalCopied()+" executor: "+executor);
    }

    private synchronized void addTotalCopied(long add) {
        totalCopied += add;
    }

    private synchronized long getTotalCopied() {
        return totalCopied;
    }

    private class MyRunner implements Runnable {
        MyResourceWriter resourceWriter;
        private final Reply reply;
        private final Resource resource;

        public MyRunner(Reply reply, Resource resource) {
            this.reply = reply;
            this.resource = resource;
            this.resourceWriter = new MyResourceWriter(DEFAULT_BUFFER_SIZE, reply, resource);
            this.resource.configure(resourceWriter);
        }

        public void enqueue() {
            executor.remove(this);
            executor.execute(this);
        }

        public void retry(Exception e, String because) {
            if (resourceWriter.shouldRetry()) {
                resourceWriter.increaseRetryCount();
                resource.intermediate(report, "retry");
                System.out.println("se.lagrummet.rinfo.base.feed.impl.ResourceLocatorImpl.MyRunner.run " + because + " RETRY " + resourceWriter.retryCount + " of " + resource.getUrl());
                enqueue();
            } else {
                resource.intermediate(report, "retry "+because+" FAILED");
                resourceWriter.failed(e);
            }
        }

        @Override
        public void run() {
            try {
                resourceWriter.download();
            } catch (MD5SumVerificationFailedException e) {
                retry(e, "MD5SUM diff");
            } catch (FileNotFoundException e) {
                retry(e, "File not found");
            } catch (SocketException e) {
                retry(e, e.getMessage());
            } catch (IOException e) {
                retry(e, "IO Error");
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                resourceWriter.failed(e);
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
                addTotalCopied( Utils.copyStream(in, out, md5SumCalculator) );
                in.close();
                out.close();
                md5Sum = md5SumCalculator.create();
                resource.verifyMd5Sum(md5Sum);
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

        public void failed(Throwable e) {
            reply.failed(Failure.ResourceWrite, e.getMessage());
            System.out.println("se.lagrummet.rinfo.base.feed.impl.ResourceLocatorImpl.MyResourceWriter.failed e.message="+e.getMessage()+" class="+e.getClass().getName());
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
