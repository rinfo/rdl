

class OrganizationController {
    
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
            flash.message = "Organization not found with id ${params.id}"
            redirect(action:list)
        }
        else { return [ organizationInstance : organizationInstance ] }
    }

    def delete = {
        def organizationInstance = Organization.get( params.id )
        if(organizationInstance) {
            try {
                organizationInstance.delete()
                flash.message = "Organization ${params.id} deleted"
                redirect(action:list)
            }
            catch(org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "Organization ${params.id} could not be deleted"
                redirect(action:show,id:params.id)
            }
        }
        else {
            flash.message = "Organization not found with id ${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def organizationInstance = Organization.get( params.id )

        if(!organizationInstance) {
            flash.message = "Organization not found with id ${params.id}"
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
                    
                    organizationInstance.errors.rejectValue("version", "organization.optimistic.locking.failure", "Another user has updated this Organization while you were editing.")
                    render(view:'edit',model:[organizationInstance:organizationInstance])
                    return
                }
            }
            organizationInstance.properties = params
            if(!organizationInstance.hasErrors() && organizationInstance.save()) {
                flash.message = "Organization ${params.id} updated"
                redirect(action:show,id:organizationInstance.id)
            }
            else {
                render(view:'edit',model:[organizationInstance:organizationInstance])
            }
        }
        else {
            flash.message = "Organization not found with id ${params.id}"
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
            flash.message = "Organization ${organizationInstance.id} created"
            redirect(action:show,id:organizationInstance.id)
        }
        else {
            render(view:'create',model:[organizationInstance:organizationInstance])
        }
    }
}
