package se.lagrummet.rinfo.base.feed.type;

import org.w3c.dom.Document;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.util.Utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/**
 * Created by christian on 5/21/15.
 */
public abstract class CommonUrl {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    protected String url;

    public URL getUrl() throws MalformedURLException {return new URL(url);}

    protected CommonUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return Utils.extractFileName(url);
    }

    public void copyToFile(Path path) throws IOException {
        Files.copy(getUrl().openStream(), path);
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public Md5Sum copyToFile(File target) throws IOException, NoSuchAlgorithmException {
        InputStream in = new BufferedInputStream(getUrl().openStream());
        OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        Md5Sum.Md5SumCalculator md5SumCalculator = Md5Sum.calculator();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
            md5SumCalculator.update(buffer, 0, len);
        }
        in.close();
        out.close();
        return md5SumCalculator.create();
    }

    public Document getDocument() throws FailedToReadFeedException {
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(getUrl().openStream());
        } catch(Exception e) {
            throw new FailedToReadFeedException(url.toString(),e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommonUrl commonUrl = (CommonUrl) o;

        if (url != null ? !url.toString().equals(commonUrl.url.toString()) : commonUrl.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
