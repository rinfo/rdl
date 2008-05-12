########################################################################
RInfo - Main README
########################################################################


Getting Started
========================================================================



Repository Contents
========================================================================

The top-level directories separate packages by intrinsic purpose. Naming needs
in each partition may differ as well.

applications/
    Complete (client and/or server) applications who interact with the RInfo
    services in some way. *Each directory should represent a self-contained
    application.*

laboratory/
    Designs, data examples, tools, experiments etc. Things here should either
    graduate into applications, libraries or services, become documentation, or
    eventually be removed.

libraries/
    Shared data and code that other things depend on.

    base/
        The most important library, containing the core model, support data and
        shared logic for RInfo.

    external/
        Code maintained by other parties but put here for convenience. Keep
        this to a minimum!

    *Proper code libraries should live under directories named by implementation
    language (e.g. "libraries/java/rinfo-util").*

README.txt
    This file.

services/
    Primary services that constitute the RInfo architecture. *Each directory
    should represent a self-contained service.*

setup/
    Core development and deployment setup tools.


