package se.lagrummet.rinfo.service

import org.restlet.data.MediaType
import org.restlet.data.Status
import org.restlet.resource.Handler

import se.lagrummet.rinfo.collector.NotAllowedSourceFeedException


class RDFLoaderHandler extends Handler {

    @Override
    public boolean allowPost() { return true; }

    @Override
    public void handlePost() {
        // TODO: verify source of request (or only via loadScheduler.sourceFeedUrls)?
        // TODO: error handling.. (report and/or (public) status/log)

        def loadScheduler = (SesameLoadScheduler) getContext().getAttributes().get(
                ServiceApplication.SERVICE_COMPONENTS_CONTEXT_KEY).loadScheduler

        String feedUrl = request.getEntityAsForm().getFirstValue("feed")
        if (feedUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing feed parameter.")
            return
        }

        def msg = "Scheduled collect of <${feedUrl}>."
        def status = null

        try {
            boolean wasScheduled = loadScheduler.triggerFeedCollect(new URL(feedUrl))
            if (!wasScheduled) {
                msg = "The url <${feedUrl}> is already scheduled for collect."
            }
        } catch (NotAllowedSourceFeedException e) {
                msg = "The url <${feedUrl}> is not an allowed source feed."
                status = Status.CLIENT_ERROR_FORBIDDEN
        }

        if (status != null) {
            getResponse().setStatus(status)
        }
        getResponse().setEntity(msg, MediaType.TEXT_PLAIN)

    }

}
