

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
            flash.message = "Organisation med id ${params.id} kunde inte hittas"
            redirect(action:list)
        }
        else { return [ organizationInstance : organizationInstance ] }
    }

    def delete = {
        def organizationInstance = Organization.get( params.id )
        if(organizationInstance) {
            try {
                organizationInstance.delete()
                flash.message = "Organisation med id ${params.id} raderades"

                //Skapa atomentry om denna radering
                def entry = new Entry()
                entry.title = organizationInstance.name + " raderades"
                entry.uri = organizationInstance.rinfoURI()
                entry.content = ""
                entry.content_md5 = ""
                entry.dateDeleted = new Date()
                entry.lastUpdated = new Date()
                entry.save()
                entry.errors.allErrors.each {
                    println it
                }

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

                //Skapa atomentry om denna uppdatering
                def entry = new Entry()
                def entry_time = new Date()

                //Hitta tidigare entry om denna post
                def first_entry = Entry.findByitem_classANditem_id(organizationInstance.class, organizationInstance.id)
                println(first_entry)

                entry.relateTo(organizationInstance)

                entry.title = organizationInstance.name + " uppdaterades"
                entry.uri = organizationInstance.rinfoURI()
                entry.content = organizationInstance.toRDF()
                entry.content_md5 = ""
                entry.dateCreated = entry_time
                entry.last_updated = entry_time
                entry.save()
                entry.errors.allErrors.each {
                    println it
                }
                                
                //Move along
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

            //Skapa atomentry om denna post
            flash.message = "Organisation ${organizationInstance.id} skapades"
            redirect(action:show,id:organizationInstance.id)
        }
        else {
            render(view:'create',model:[organizationInstance:organizationInstance])
        }
    }
}
