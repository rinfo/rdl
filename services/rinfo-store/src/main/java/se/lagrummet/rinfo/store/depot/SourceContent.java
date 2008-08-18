package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;


public class SourceContent {


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


    void writeTo(File file) throws IOException, IllegalStateException {
        FileOutputStream outStream = new FileOutputStream(file);
        writeTo(outStream);
    }

    void writeTo(FileOutputStream outStream)
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

}
