package se.lagrummet.rinfo.base.feed;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.exceptions.ResourceWriteException;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by christian on 5/25/15.
 */
public interface ResourceLocator {

    enum Failure {Timeout, Parse, ResourceWrite, Unknown}

    void locate(Resource resource, Reply reply);

    interface Resource {
        Integer size();
        Md5Sum writeTo(OutputStream outputStream, URL baseUrl) throws ResourceWriteException;
    }

    interface Data {
        Md5Sum getMd5Sum();
        InputStream asInputStream();
        Document asDocument() throws ParserConfigurationException, IOException, SAXException;
    }

    interface Reply {
        void ok(Data data);
        void failed(Failure failure, String comment);
    }

}
