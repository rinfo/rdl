

class FeedController {
    
    def index = { redirect(action:list,params:params) }
    def entryService

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        [ feedInstanceList: Feed.list( params ), feedInstanceTotal: Feed.count() ]
    }

    def show = {
        def feedInstance = Feed.get( params.id )

        if(!feedInstance) {
            flash.message = "Inhämtningskällan kunde inte hittas"
            redirect(action:list)
        }
        else { return [ feedInstance : feedInstance ] }
    }

    def delete = {
        def feedInstance = Feed.get( params.id )
        if(feedInstance) {
            try {
                entryService.deleteItem(feedInstance)
                flash.message = "Inhämtningskällan raderad"
                redirect(action:list)
            }
            catch(org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "Inhämtningskällan kunde inte raderas"
                redirect(action:show,id:params.id)
            }
        }
        else {
            flash.message = "Inhämtningskällan kunde inte hittas"
            redirect(action:list)
        }
    }

    def edit = {
        def feedInstance = Feed.get( params.id )

        if(!feedInstance) {
            flash.message = "Inhämtningskällan kunde inte hittas"
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
            if(!feedInstance.hasErrors() && feedInstance.save(flush:true)) {
                entryService.createEntry(feedInstance)
                flash.message = "Inhämtningskällan uppdaterad"
                redirect(action:show,id:feedInstance.id)
            }
            else {
                render(view:'edit',model:[feedInstance:feedInstance])
            }
        }
        else {
            flash.message = "Inhämtningskällan kunde inte hittas"
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
            entryService.createEntry(feedInstance)
            flash.message = "Inhämtningskällan skapades"
            redirect(action:show,id:feedInstance.id)
        }
        else {
            render(view:'create',model:[feedInstance:feedInstance])
        }
    }
}
