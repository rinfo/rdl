package se.lagrummet.rinfo.base.feed;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.exceptions.MD5SumVerificationFailedException;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by christian on 5/25/15.
 */
public interface ResourceLocator {

    enum Failure {Timeout, Parse, ResourceWrite, Md5SumDiff, Unknown}

    void locate(Resource resource, Reply reply);
    void printStatusOneLiner();

    interface Resource extends Report.Reporter {
        Integer size();
        void configure(ResourceWriter resourceWriter) ;
        String getUrl();
        void verifyMd5Sum(Md5Sum md5Sum) throws MD5SumVerificationFailedException;
    }

    interface ResourceWriter {
        void setUrl(String url);
        void setSize(int size);
    }

    interface Data {
        Resource getResource();
        Md5Sum getMd5Sum();
        InputStream asInputStream();
        Document asDocument() throws ParserConfigurationException, IOException, SAXException;
    }

    interface Reply {
        void ok(Data data);
        void failed(Failure failure, String comment);
    }

}
