package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.CopyFeed;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

/**
* Created by christian on 5/21/15.
*/
public class EntryDocumentDownloaderImpl implements CopyFeed.EntryDocumentDownloader {

    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(60000);
    Executor executor = new ThreadPoolExecutor(100, 200, 20, TimeUnit.SECONDS, queue);
    private CopyFeed.FileNameCreator fileNameCreator;

    public EntryDocumentDownloaderImpl(CopyFeed.FileNameCreator fileNameCreator) {
        this.fileNameCreator = fileNameCreator;
    }

    @Override
    public void downloadEntryAndVerifyMd5Sum(final CopyFeed.EntryDocumentDownloaderReply reply, final Feed feed, final Feed.EntryId entryId, final DocumentUrl documentUrl, final Md5Sum md5Sum, final String targetPath) {
        queue.add(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileName = fileNameCreator.createFileName(feed, entryId, documentUrl);
                    Md5Sum md5SumOfDownloadedDocument = documentUrl.copyToFile(new File(targetPath + "/" + fileName));
                    if (!md5Sum.equals(md5SumOfDownloadedDocument)) {
                        reply.md5SumCheckFailed(md5SumOfDownloadedDocument);
                        return;
                    }
                    reply.completed();
                } catch (IOException e) {
                    reply.failed(e);
                } catch (NoSuchAlgorithmException e) {
                    reply.failed(e);
                }
            }
        });
    }
}
