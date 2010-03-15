package se.lagrummet.rinfo.main

import org.restlet.data.MediaType
import org.restlet.data.Status
import org.restlet./*resource.*/Handler

import se.lagrummet.rinfo.collector.NotAllowedSourceFeedException

import se.lagrummet.rinfo.main.storage.FeedCollectScheduler


class CollectorHandler extends Handler {

    static String BAD_MSG = "Requires POST and a feed query parameter (URL)."

    @Override
    public boolean allowPost() { return true; }

    @Override
    public void handleGet() {
        // TODO: some form of collect status page..
        getResponse().setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED, BAD_MSG)
    }

    @Override
    public void handlePost() {
        // TODO: verify source of request (or only via loadScheduler.sourceFeedUrls)?
        // TODO: error handling.. (report and/or (public) log)

        def collectScheduler = (FeedCollectScheduler) context.getAttributes().get(
                MainApplication.COLLECTOR_RUNNER_CONTEXT_KEY)

        // TODO: pubsubhubbub:
        // if ("publish".equals(POST["hub.mode"]) { ... POST["hub.url"] }
        String feedUrl = request.getEntityAsForm().getFirstValue("feed")
        if (feedUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, BAD_MSG)
            return
        }

        def msg = "Scheduled collect of <${feedUrl}>."
        def status = Status.SUCCESS_ACCEPTED // 202
        //  TODO:? The entity returned with this response SHOULD include an
        //  indication of the request's current status and either a pointer to
        //  a status monitor or some estimate of when the user can expect the
        //  request to be fulfilled.

        try {
            boolean wasScheduled = collectScheduler.triggerFeedCollect(new URL(feedUrl))
            if (!wasScheduled) {
                msg = "The url <${feedUrl}> is already scheduled for collect."
            }
        } catch (NotAllowedSourceFeedException e) {
                msg = "The url <${feedUrl}> is not an allowed source feed."
                status = Status.CLIENT_ERROR_FORBIDDEN
        }
        getResponse().setStatus(status)
        getResponse().setEntity(msg, MediaType.TEXT_PLAIN)
    }

}
