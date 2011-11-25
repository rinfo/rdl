########################################################################
README - RInfo Project
########################################################################


Getting Started
========================================================================

See ``manage/running_rinfo_locally.txt``.

General Management
========================================================================

See ``manage/README.txt``.

Contents
========================================================================

The project consists of:

README.txt
    This file.

documentation/
    All forms of documentation, including system descriptions, data examples
    (both core data and service responses), acceptance specifications/tests
    etc.

manage/
    Management tools for config, build, integration and deployment.

resources/
    Shared data (and platform-independent code) that other things depend on.

    base/
        The most important library, containing the core model, support data and
        shared logic for RInfo.

    external/
        Code maintained by other parties but put here for convenience. Keep
        this to a minimum.

packages/

    All code modules/libraries/components live here, under directories named by
    implementation platform/language (e.g. "java/rinfo-main").

    All components (libraries and services) that constitute the RInfo
    architecture live here. (Each directory under the platform directory should
    represent a self-contained component.)

tools/
    Instrumental tools for development and (non-setup related) management.

laboratory/
    Designs, data examples, tools, experiments etc. Things here should either
    graduate into resources, code modules, become documentation or eventually
    be removed.

    applications/
        Example (client and/or server) applications who interact with the RInfo
        services in some way. *Each directory should represent a self-contained
        application.*

