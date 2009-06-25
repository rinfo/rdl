

class PublicationcollectionController {
    
    def entryService
    def index = { redirect(action:list,params:params) }

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        [ publicationcollectionInstanceList: Publicationcollection.list( params ), publicationcollectionInstanceTotal: Publicationcollection.count() ]
    }

    def show = {
        def publicationcollectionInstance = Publicationcollection.get( params.id )

        if(!publicationcollectionInstance) {
            flash.message = "Författningssamlingen kunde inte hittas"
            redirect(action:list)
        }
        else { return [ publicationcollectionInstance : publicationcollectionInstance ] }
    }

    def delete = {
        def publicationcollectionInstance = Publicationcollection.get( params.id )
        if(publicationcollectionInstance) {
            try {
                entryService.deleteItem(publicationcollectionInstance)
                flash.message = "Författningssamlingen raderades"
                redirect(action:list)
            }
            catch(org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "Författningssamlingen kudne inte raderas"
                redirect(action:show,id:params.id)
            }
        }
        else {
            flash.message = "Författningssamlingen kunde inte hittas"
            redirect(action:list)
        }
    }

    def edit = {
        def publicationcollectionInstance = Publicationcollection.get( params.id )

        if(!publicationcollectionInstance) {
            flash.message = "Författningssamlingen kunde inte hittas"
            redirect(action:list)
        }
        else {
            return [ publicationcollectionInstance : publicationcollectionInstance ]
        }
    }

    def update = {
        def publicationcollectionInstance = Publicationcollection.get( params.id )
        if(publicationcollectionInstance) {
            if(params.version) {
                def version = params.version.toLong()
                if(publicationcollectionInstance.version > version) {
                    
                    publicationcollectionInstance.errors.rejectValue("version", "publicationcollection.optimistic.locking.failure", "En annan användare har uppdaterat denna författningssamling medan du redigerade den.")
                    render(view:'edit',model:[publicationcollectionInstance:publicationcollectionInstance])
                    return
                }
            }
            publicationcollectionInstance.properties = params
            if(!publicationcollectionInstance.hasErrors() && publicationcollectionInstance.save()) {
                entryService.createEntry(publicationcollectionInstance)
                flash.message = "Författningssamlingen uppdaterades"
                redirect(action:show,id:publicationcollectionInstance.id)
            }
            else {
                render(view:'edit',model:[publicationcollectionInstance:publicationcollectionInstance])
            }
        }
        else {
            flash.message = "Författningssamlingen kunde inte hittas"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def publicationcollectionInstance = new Publicationcollection()
        publicationcollectionInstance.properties = params
        return ['publicationcollectionInstance':publicationcollectionInstance]
    }

    def save = {
        def publicationcollectionInstance = new Publicationcollection(params)
        if(!publicationcollectionInstance.hasErrors() && publicationcollectionInstance.save()) {
            entryService.createEntry(publicationcollectionInstance)
            flash.message = "Författningssamlingen skapades"
            redirect(action:show,id:publicationcollectionInstance.id)
        }
        else {
            render(view:'create',model:[publicationcollectionInstance:publicationcollectionInstance])
        }
    }
}
