class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?"{
	    constraints { // apply constraints here  
            }
	}

        "/"{
            controller = "event" //Defaultvyn
        }
	
        "500"(view:'/error')
        
    }
}
