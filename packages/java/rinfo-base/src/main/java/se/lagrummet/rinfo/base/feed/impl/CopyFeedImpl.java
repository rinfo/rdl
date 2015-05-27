package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.*;
import se.lagrummet.rinfo.base.feed.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by christian on 5/21/15.
 */
public class CopyFeedImpl implements CopyFeed {

    ResourceLocator resourceLocator;
    Parser parser;

    public CopyFeedImpl(ResourceLocator resourceLocator, Parser parser) {
        this.resourceLocator = resourceLocator;
        this.parser = parser;
    }

    @Override
    public void copy(ResourceLocator.Resource resource, String targetPath, Report report) throws FailedToReadFeedException, EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException, MalformedURLException {
        Parser.FeedBuilder feedBuilder = parser.parse(resource, report);
        Feed feed = feedBuilder.toFeed();

        DownloadAndWriteToDisk downloadAndWriteToDisk = new DownloadAndWriteToDisk(extractContentList(feed), targetPath, report);
        downloadAndWriteToDisk.start();
        downloadAndWriteToDisk.waitUntilCompleted(120 * 60);
        resource.end(report);
    }

    Iterable<Feed.Content> extractContentList(Feed feed) {
        List<Feed.Content> contents = new LinkedList<>();
        for (Feed.Entry entry : feed.getEntries()) {
            for (Feed.Content content : entry.getContentList()) {
                contents.add(content);
            }
        }
        return contents;
    }

    private class DownloadAndWriteToDisk {
        Iterable<Feed.Content> contents;
        private String targetPath;
        private Report report;
        boolean completed = false;
        int remainingTasks = 0;

        private DownloadAndWriteToDisk(Iterable<Feed.Content> contents, String targetPath, Report report) {
            this.contents = contents;
            this.targetPath = targetPath;
            this.report = report;
        }


        private synchronized void countDownRemainingTasks() {
            remainingTasks--;
            if (remainingTasks==0) {
                completed = true;
                notifyAll();
            }
        }

        public synchronized void start() {
            for (Feed.Content content : contents) {
                if (content.getDocumentUrl()!=null) {
                    File file = new File(targetPath, content.getDocumentUrl().getName());
                    if (!file.exists()) {
                        remainingTasks++;
                        new DownloadTask(content, file, report).copy();
                    } else
                        //content.asResource().intermediate(report, "File exists '"+file+"'! NOT downloading!");
                        content.asResource().end(report);
                }
            }
        }

        public synchronized void waitUntilCompleted(int timeoutSec) {
            if (completed)
                return;
            try {
                wait(timeoutSec*1000);
            } catch (InterruptedException e) {}
            if (!completed)
                throw new SevereInternalException("Timeout! remainingTasks="+remainingTasks);
        }

        class DownloadTask implements ResourceLocator.Reply{
            private Feed.Content content;
            private File file;
            private Report report;

            DownloadTask(Feed.Content content, File file, Report report) {
                this.content = content;
                this.file = file;
                this.report = report;
            }

            @Override
            public void ok(ResourceLocator.Data data) {
                data.getResource().intermediate(report, "downloaded");
                if (content.getMd5Sum()!=null && data.getMd5Sum()!=null) {
                    if (!content.getMd5Sum().equals(data.getMd5Sum())) {
                        failed(ResourceLocator.Failure.Md5SumDiff, "feed md5 "+content.getMd5Sum()+" actual md5 "+data.getMd5Sum());
                        return;
                    }
                }
                try {
                    System.out.println("Downloaded file file="+file);
                    Files.copy(data.asInputStream(), file.toPath());
                    data.getResource().end(report);
                    countDownRemainingTasks();
                } catch (FileAlreadyExistsException e) {
                    data.getResource().end(report);
                    countDownRemainingTasks();
                    data.getResource().intermediate(report, "file already exists "+e.getFile());
                } catch (IOException e) {
                    failed(ResourceLocator.Failure.ResourceWrite, e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(ResourceLocator.Failure failure, String comment) {
                System.out.println("Download failed "+failure+" "+comment);
                countDownRemainingTasks();
            }

            public void copy() {
                ResourceLocator.Resource resource = null;
                try {
                    resource = content.asResource();
                } catch (Exception e) {
                    e.printStackTrace();
                    return ;
                }
                resource.start(report);
                resourceLocator.locate(resource, this);
            }
        }
    }

}
