package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.*;
import se.lagrummet.rinfo.base.feed.exceptions.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public Feed copy(ResourceLocator.Resource resource, String targetPath, Report report) throws FailedToReadFeedException, EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException, IOException, ParserConfigurationException, TransformerException {
        Parser.FeedBuilder feedBuilder = parser.parse(resource, report);
        Feed feed = feedBuilder.toFeed();

        DownloadAndWriteToDisk downloadAndWriteToDisk = new DownloadAndWriteToDisk(extractContentList(feed), targetPath, report);
        downloadAndWriteToDisk.start();
        downloadAndWriteToDisk.waitUntilCompleted(120 * 60);

        File atomFile = new File(targetPath+"/index.atom");
        if (atomFile.exists())
            atomFile.delete();
        FileOutputStream fileOutputStream = new FileOutputStream(atomFile);

        FeedWriter feedWriter = new FeedXmlBuilderImpl();
        FeedWriterImpl writer = new FeedWriterImpl();
        feedWriter.write(feed, writer, report);

        writer.writeTo(fileOutputStream);

        fileOutputStream.close();

        resource.intermediate(report, "Written atom file: %!",atomFile);

        return feed;
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
            boolean nothingDownloaded = true;
            for (Feed.Content content : contents) {
                if (content.getDocumentUrl()!=null) {
                    File file = new File(targetPath, content.getDocumentUrl().getName());
                    if (!file.exists()) {
                        nothingDownloaded = false;
                        remainingTasks++;
                        new DownloadTask(content, file, report).copy();
                    } else
                        content.asResource().intermediate(report, "File exists '%1'! NOT downloading!", file);
                        content.asResource().end(report);
                }
            }
            if (nothingDownloaded) {
                completed = true;
                notifyAll();
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
                if (content.getMd5Sum()!=null && data.getMd5Sum()!=null) {
                    if (!content.getMd5Sum().equals(data.getMd5Sum())) {
                        data.getResource().intermediate(report, "Md5SumDiff expected %1 but was %2", content.getMd5Sum(), data.getMd5Sum());
                        failed(ResourceLocator.Failure.Md5SumDiff, "feed md5 "+content.getMd5Sum()+" actual md5 "+data.getMd5Sum()+" of resource "+data.getResource().getUrl());
                        return;
                    }
                }
                try {
                    data.getResource().intermediate(report, "Downloading", file);
                    Files.copy(data.asInputStream(), file.toPath());
                    data.getResource().end(report);
                    countDownRemainingTasks();
                } catch (FileAlreadyExistsException e) {
                    data.getResource().end(report);
                    countDownRemainingTasks();
                    data.getResource().intermediate(report, "file already exists %1",e.getFile());
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
