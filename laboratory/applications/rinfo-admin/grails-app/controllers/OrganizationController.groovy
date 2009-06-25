class OrganizationController {
    
    def entryService
    def index = { redirect(action:list,params:params) }

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        [ organizationInstanceList: Organization.list( params ), organizationInstanceTotal: Organization.count() ]
    }

    def show = {
        def organizationInstance = Organization.get( params.id )

        if(!organizationInstance) {
            flash.message = "Organisation med id ${params.id} kunde inte hittas"
            redirect(action:list)
        }
        else { return [ organizationInstance : organizationInstance ] }
    }

    def delete = {
        def organizationInstance = Organization.get( params.id )
        if(organizationInstance) {
            try {
                entryService.deleteItem(organizationInstance)
                flash.message = "Organisation med id ${params.id} raderades"
                redirect(action:list)
            }
            catch(org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "Organisation med id ${params.id} kunde inte raderas"
                redirect(action:show,id:params.id)
            }
        }
        else {
            flash.message = "Organisation med id ${params.id} kunde inte hittas"
            redirect(action:list)
        }
    }

    def edit = {
        def organizationInstance = Organization.get( params.id )

        if(!organizationInstance) {
            flash.message = "Organisation med id ${params.id} kunde inte hittas"
            redirect(action:list)
        }
        else {
            return [ organizationInstance : organizationInstance ]
        }
    }

    def update = {
        def organizationInstance = Organization.get( params.id )
        if(organizationInstance) {
            if(params.version) {
                def version = params.version.toLong()
                if(organizationInstance.version > version) {
                    
                    organizationInstance.errors.rejectValue("version", "organization.optimistic.locking.failure", "En annan användare har uppdaterat denna organisation medan du redigerade den.")
                    render(view:'edit',model:[organizationInstance:organizationInstance])
                    return
                }
            }
            organizationInstance.properties = params
            if(!organizationInstance.hasErrors() && organizationInstance.save()) {
                entryService.createEntry(organizationInstance)
                flash.message = "Organisation med id ${params.id} uppdaterad"
                redirect(action:show,id:organizationInstance.id)
            }
            else {
                render(view:'edit',model:[organizationInstance:organizationInstance])
            }
        }
        else {
            flash.message = "Organisation med id ${params.id} kunde inte hittas"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def organizationInstance = new Organization()
        organizationInstance.properties = params
        return ['organizationInstance':organizationInstance]
    }

    def save = {
        def organizationInstance = new Organization(params)
        if(!organizationInstance.hasErrors() && organizationInstance.save()) {
            entryService.createEntry(organizationInstance)
            flash.message = "Organisation ${organizationInstance.id} skapades"
            redirect(action:show,id:organizationInstance.id)
        }
        else {
            render(view:'create',model:[organizationInstance:organizationInstance])
        }
    }
}
