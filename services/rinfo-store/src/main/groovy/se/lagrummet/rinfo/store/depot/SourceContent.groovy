package se.lagrummet.rinfo.store.depot

import java.nio.channels.Channels


class SourceContent {

    File sourceFile
    InputStream sourceStream
    String enclosedUriPath
    String mediaType
    String lang

    SourceContent(mediaType, lang=null, enclosedUriPath=null) {
        this.mediaType = mediaType
        this.lang = lang
        this.enclosedUriPath = enclosedUriPath
    }

    SourceContent(File sourceFile,
            mediaType, lang=null, enclosedUriPath=null) {
        this(mediaType, lang, enclosedUriPath)
        this.sourceFile = sourceFile
    }

    SourceContent(InputStream sourceStream,
            mediaType, lang=null, enclosedUriPath=null) {
        this(mediaType, lang, enclosedUriPath)
        this.sourceStream = sourceStream
    }

    void writeTo(File file) throws IOException, IllegalStateException {
        def destOutStream = new FileOutputStream(file)
        try {
            if (sourceFile != null) {
                def srcChannel = new FileInputStream(sourceFile).getChannel()
                def destChannel = destOutStream.getChannel()
                destChannel.transferFrom(srcChannel, 0, srcChannel.size())

            } else if (sourceStream != null) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = sourceStream.read(buf)) > 0) {
                    destOutStream.write(buf, 0, len);
                }
                sourceStream.close()

            } else {
                throw new IllegalStateException(
                        "Neither sourceStream nor sourceFile is set.")
            }
        } finally {
            destOutStream.close()
        }
    }

}
