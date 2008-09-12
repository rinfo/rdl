package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.CountingOutputStream;


public class SourceContent {

    public static enum Check {
        MD5, LENGTH;
    }

    private InputStream sourceStream;
    private String enclosedUriPath;
    private String mediaType;
    private String lang;
    private Map<Check, Object> datachecks = new HashMap<Check, Object>();

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

    public SourceContent(File sourceFile,
            String mediaType, String lang, String enclosedUriPath)
            throws FileNotFoundException {
        this(new FileInputStream(sourceFile), mediaType, lang, enclosedUriPath);
    }

    public SourceContent(File sourceFile, String mediaType, String lang)
            throws FileNotFoundException {
        this(sourceFile, mediaType, lang, null);
    }
    public SourceContent(File sourceFile, String mediaType)
            throws FileNotFoundException {
        this(sourceFile, mediaType, null);
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
        this.setSourceStream(sourceStream, true);
    }

    public void setSourceStream(InputStream sourceStream, boolean clearChecks) {
        this.sourceStream = sourceStream;
        if (clearChecks) {
            datachecks.clear();
        }
    }

    /**
     * A map with token, value checks to perform when calling {@link writeTo}.
     */
    public Map<Check, Object> getDatachecks() {
        return datachecks;
    }

    /**
     * @see {@link #writeTo(OutputStream)}.
     */
    public void writeTo(File file)
            throws IOException, IllegalStateException, SourceCheckException {
        FileOutputStream outStream = new FileOutputStream(file);
        writeTo(outStream);
    }

    /**
     * Writes the {@link #sourceStream} to the outStream and closes both streams
     * upon completion. Also runs any registered {@link #datachecks} at close
     * time.
     */
    public void writeTo(OutputStream outStream) throws IOException {
        outStream = checkedOutStream(outStream);
        try {
            IOUtils.copyLarge(sourceStream, outStream);
        } finally {
            try {
                sourceStream.close();
            } finally {
                outStream.close();
            }
        }
    }

    private OutputStream checkedOutStream(OutputStream outStream) throws IOException {
        for (Check check : datachecks.keySet()) {
            switch (check) {
                case MD5:
                    try {
                        outStream = new CheckMD5OutputStream(outStream);
                    } catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException(
                                "Cannot create a MessageDigest instance for MD5.", e);
                    }
                    break;
                case LENGTH:
                    outStream = new CheckLengthOutputStream(outStream);
                    break;
            }
        }
        return outStream;
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

    protected class CheckLengthOutputStream extends CountingOutputStream {

        public CheckLengthOutputStream(OutputStream outStream) {
            super(outStream);
        }

        public void close() throws IOException {
            super.close();
            checkLength();
        }

        private void checkLength() throws IOException {
            checkExpected(Check.LENGTH, getByteCount());
        }

    }

    protected class CheckMD5OutputStream extends DigestOutputStream {

        public CheckMD5OutputStream(OutputStream outStream)
                throws NoSuchAlgorithmException {
            super(outStream, MessageDigest.getInstance("MD5"));
        }

        public void close() throws IOException {
            super.close();
            checkMd5();
        }

        private void checkMd5() throws IOException {
            MessageDigest digest = getMessageDigest();
            String hexDigest = new String(Hex.encodeHex(digest.digest()));
            checkExpected(Check.MD5, hexDigest);
        }

    }

}
