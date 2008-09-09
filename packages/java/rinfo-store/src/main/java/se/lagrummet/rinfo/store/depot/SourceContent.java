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

    private File sourceFile;
    public File getSourceFile() { return sourceFile; }

    private InputStream sourceStream;
    public InputStream getSourceStream() { return sourceStream; }

    private String enclosedUriPath;
    public String getEnclosedUriPath() { return enclosedUriPath; }

    private String mediaType;
    public String getMediaType() { return mediaType; }

    private String lang;
    public String getLang() { return lang; }

    private Map<Check, Object> datachecks = new HashMap<Check, Object>();
    /**
     * A map with token, value checks to perform when calling {@link writeTo}.
     */
    public Map<Check, Object> getDatachecks() { return datachecks; }

    public SourceContent(File sourceFile,
            String mediaType, String lang, String enclosedUriPath) {
        this(mediaType, lang, enclosedUriPath);
        this.sourceFile = sourceFile;
    }

    public SourceContent(File sourceFile, String mediaType, String lang) {
        this(sourceFile, mediaType, lang, null);
    }
    public SourceContent(File sourceFile, String mediaType) {
        this(sourceFile, mediaType, null);
    }

    // TODO: URL instead? Perhaps allow sourceStream too, but not closing it?
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


    void writeTo(File file) throws IOException, IllegalStateException, SourceCheckException {
        FileOutputStream outStream = new FileOutputStream(file);
        writeTo(outStream);
        checkData(file);
    }

    // TODO: public again if we do Check stuff byte by byte (which we "should")
    private void writeTo(FileOutputStream outStream)
            throws IOException, IllegalStateException {
        try {
            if (sourceFile != null) {
                FileChannel srcChannel = new FileInputStream(sourceFile).getChannel();
                FileChannel destChannel = outStream.getChannel();
                destChannel.transferFrom(srcChannel, 0, srcChannel.size());

            } else if (sourceStream != null) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = sourceStream.read(buf)) > 0) {
                    outStream.write(buf, 0, len);
                }
                sourceStream.close();

            } else {
                throw new IllegalStateException(
                        "Neither sourceStream nor sourceFile is set.");
            }
        } finally {
            outStream.close();
        }
    }

    private void checkData(File file) throws IOException {
        checkLength(file);
        checkMd5(file);
    }

    private void checkLength(File file) throws IOException {
        checkExpected(Check.LENGTH, file.length());
    }

    private void checkMd5(File file) throws IOException {
        // TODO: don't calculate unless Check.MD5 in datachecks
        String md5Hex = DigestUtils.md5Hex(FileUtils.readFileToByteArray(file));
        checkExpected(Check.MD5, md5Hex);
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
