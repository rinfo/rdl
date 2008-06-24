########################################################################
RInfo - Main README
########################################################################


Getting Started
========================================================================



Repository Contents
========================================================================

The top-level directories separate packages by intrinsic purpose.

Shared code modules/libraries should live under directories named by
implementation platform/language (e.g. "java/rinfo-util").

applications/
    Complete (client and/or server) applications who interact with the RInfo
    services in some way. *Each directory should represent a self-contained
    application.*

laboratory/
    Designs, data examples, tools, experiments etc. Things here should either
    graduate into resources, code modules, become documentation or eventually
    be removed.

resources/
    Shared data (and platform-independent code) that other things depend on.

    base/
        The most important library, containing the core model, support data and
        shared logic for RInfo.

    external/
        Code maintained by other parties but put here for convenience. Keep
        this to a minimum.

README.txt
    This file.

services/
    Primary services that constitute the RInfo architecture. *Each directory
    should represent a self-contained service.*

setup/
    Core development and deployment setup tools.


