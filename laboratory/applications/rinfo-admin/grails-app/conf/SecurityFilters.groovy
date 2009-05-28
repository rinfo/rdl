class SecurityFilters {
    def filters = {

        // Ensure that all controllers and actions require an authenticated admin,
        // except for the "public" controller
        auth(controller: "*", action: "*") {
            before = {
                // Exclude the "public" controller.
                if (controllerName == "public") return true

                // Rest requires Admin privs
                accessControl { role("Administrator") } 
            } 
        }

        // Creating, modifying, or deleting a book requires the "Administrator" 
        // role. 
        ////bookEditing(controller: "book", action: "(create|edit|save|update|delete)") { 
        //    before = { 
        //        accessControl { role("Administrator") } 
        //    } 
        //}

        // Showing a book requires the "Administrator" *or* the "User" roles. 
        //bookShow(controller: "book", action: "show") { 
        //    before = { 
        //        accessControl { role("Administrator") || role("User") }
        //    } 
        //} 
    } 
}

