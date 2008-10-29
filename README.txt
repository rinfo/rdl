########################################################################
RInfo - Project README
########################################################################


Getting Started
========================================================================



Repository Contents
========================================================================

The top-level directories separate packages by intrinsic purpose.

documentation/
    All forms of documentation, including system descriptions, data examples
    (both core data and service responses), acceptance specifications/tests
    etc.

laboratory/
    Designs, data examples, tools, experiments etc. Things here should either
    graduate into resources, code modules, become documentation or eventually
    be removed.

    applications/
        Example (client and/or server) applications who interact with the RInfo
        services in some way. *Each directory should represent a self-contained
        application.*

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

packages/

    All code modules/libraries/components live here, under directories named by
    implementation platform/language (e.g. "java/rinfo-main").

    All components (libraries and services) that constitute the RInfo
    architecture live here. (Each directory under the platform directory should
    represent a self-contained component.)

setup/
    Core development and deployment setup tools.


