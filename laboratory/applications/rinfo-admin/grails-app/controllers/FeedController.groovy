

class FeedController {
    
    def index = { redirect(action:list,params:params) }

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        [ feedInstanceList: Feed.list( params ), feedInstanceTotal: Feed.count() ]
    }

    def show = {
        def feedInstance = Feed.get( params.id )

        if(!feedInstance) {
            flash.message = "Feed not found with id ${params.id}"
            redirect(action:list)
        }
        else { return [ feedInstance : feedInstance ] }
    }

    def delete = {
        def feedInstance = Feed.get( params.id )
        if(feedInstance) {
            try {
                feedInstance.delete()
                flash.message = "Feed ${params.id} deleted"
                redirect(action:list)
            }
            catch(org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "Feed ${params.id} could not be deleted"
                redirect(action:show,id:params.id)
            }
        }
        else {
            flash.message = "Feed not found with id ${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def feedInstance = Feed.get( params.id )

        if(!feedInstance) {
            flash.message = "Feed not found with id ${params.id}"
            redirect(action:list)
        }
        else {
            return [ feedInstance : feedInstance ]
        }
    }

    def update = {
        def feedInstance = Feed.get( params.id )
        if(feedInstance) {
            if(params.version) {
                def version = params.version.toLong()
                if(feedInstance.version > version) {
                    
                    feedInstance.errors.rejectValue("version", "feed.optimistic.locking.failure", "Another user has updated this Feed while you were editing.")
                    render(view:'edit',model:[feedInstance:feedInstance])
                    return
                }
            }
            feedInstance.properties = params
            if(!feedInstance.hasErrors() && feedInstance.save()) {
                flash.message = "Feed ${params.id} updated"
                redirect(action:show,id:feedInstance.id)
            }
            else {
                render(view:'edit',model:[feedInstance:feedInstance])
            }
        }
        else {
            flash.message = "Feed not found with id ${params.id}"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def feedInstance = new Feed()
        feedInstance.properties = params
        return ['feedInstance':feedInstance]
    }

    def save = {
        def feedInstance = new Feed(params)
        if(!feedInstance.hasErrors() && feedInstance.save()) {
            flash.message = "Feed ${feedInstance.id} created"
            redirect(action:show,id:feedInstance.id)
        }
        else {
            render(view:'create',model:[feedInstance:feedInstance])
        }
    }
}
