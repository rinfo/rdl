package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.*;
import java.nio.channels.FileChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.codec.digest.DigestUtils;


public class SourceContent {

    public static enum Check {
        MD5, LENGTH;
    }

    private InputStream sourceStream;
    private String enclosedUriPath;
    private String mediaType;
    private String lang;
    private Map<Check, Object> datachecks = new HashMap<Check, Object>();

    public SourceContent(File sourceFile,
            String mediaType, String lang, String enclosedUriPath)
            throws FileNotFoundException {
        this(mediaType, lang, enclosedUriPath);
        this.sourceStream = new FileInputStream(sourceFile);
    }

    public SourceContent(File sourceFile, String mediaType, String lang)
            throws FileNotFoundException {
        this(sourceFile, mediaType, lang, null);
    }
    public SourceContent(File sourceFile, String mediaType)
            throws FileNotFoundException {
        this(sourceFile, mediaType, null);
    }

    /**
     * @param sourceStream. An open InputStream. This will be closed when
     *  {@link #writeTo} is called.
     */
    public SourceContent(InputStream sourceStream,
            String mediaType, String lang, String enclosedUriPath) {
        this(mediaType, lang, enclosedUriPath);
        this.sourceStream = sourceStream;
    }

    public SourceContent(InputStream sourceStream, String mediaType, String lang) {
        this(sourceStream, mediaType, lang, null);
    }

    public SourceContent(InputStream sourceStream, String mediaType) {
        this(sourceStream, mediaType, null);
    }

    private SourceContent(String mediaType, String lang, String enclosedUriPath) {
        this.mediaType = mediaType;
        this.lang = lang;
        this.enclosedUriPath = enclosedUriPath;
    }

    public String getEnclosedUriPath() { return enclosedUriPath; }

    public String getMediaType() { return mediaType; }

    public String getLang() { return lang; }

    public void setSourceStream(InputStream sourceStream) {
        this.sourceStream = sourceStream;
    }

    /**
     * A map with token, value checks to perform when calling {@link writeTo}.
     */
    public Map<Check, Object> getDatachecks() { return datachecks; }


    public void writeTo(File file) throws IOException, IllegalStateException, SourceCheckException {
        FileOutputStream outStream = new FileOutputStream(file);
        writeTo(outStream);
    }

    // TODO:IMPROVE: compute expected values for Check by byte
    public void writeTo(OutputStream outStream) throws IOException {
        ByteArrayOutputStream checkStream = null;
        if (datachecks.size() > 0) {
            checkStream = new ByteArrayOutputStream();
        }
        try {
            byte[] buf = new byte[1024];
            int len;
            long total = 0;
            while ((len = sourceStream.read(buf)) > 0) {
                outStream.write(buf, 0, len);
                if (checkStream != null) {
                    checkStream.write(buf, 0, len);
                }
            }
            sourceStream.close();
        } finally {
            outStream.close();
        }
        if (checkStream != null) {
            checkData(checkStream);
        }
        checkStream = null;
    }

    private void checkData(ByteArrayOutputStream checkStream) throws IOException {
        checkLength(checkStream);
        checkMd5(checkStream);
    }

    private void checkLength(ByteArrayOutputStream checkStream) throws IOException {
        checkExpected(Check.LENGTH, Long.valueOf(checkStream.size()));
    }

    private void checkMd5(ByteArrayOutputStream checkStream) throws IOException {
        if (datachecks.containsKey(Check.MD5)) {
            String md5Hex = DigestUtils.md5Hex(checkStream.toByteArray());
            checkExpected(Check.MD5, md5Hex);
        }
        /* TODO:IMPROVE: To compute md5 while writing (instead of the checkStream
           duplicated buffer; see above), do::
            try {
                MessageDigest algorithm = MessageDigest.getInstance("MD5");
                algorithm.reset();
                algorithm.update(defaultBytes);
                byte messageDigest[] = algorithm.digest();
                StringBuffer hexString = new StringBuffer();
                for (int i=0;i<messageDigest.length;i++) {
                    hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
                }
                String hexValue = hexString.toString();
            } catch(NoSuchAlgorithmException e) {
            }
        */
    }

    private void checkExpected(Check check, Object real)
            throws IOException {
        Object expected = datachecks.get(check);
        if (expected == null) {
            return;
        }
        if (!real.equals(expected)) {
            throw new SourceCheckException(check, expected, real);
        }
    }

}
