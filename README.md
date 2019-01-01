# Seeds

This project is meant as a practice project for implementing unit testing and integration testing in
PHP. In my previous projects, I write code, push it to git, then pull it on the server and test it
manually. This was workable since my users were all people I could contact directly who were not
significantly affected by a few hours of downtime in return for new features. Nonetheless, as I
mature as a developer I've started to embrace testable design and development workstations in many
of my projects, and it's about time that I add that to my websites as well.

## Technical Features

This is meant to be a sample project that I can reference later when starting up new projects. In
order for it to helpful for that purpose, it must be at least somewhat similar. Thus, the project
must include a front-end that connects to a database layer which connects to a always-running layer.

The database layer must include generic application configuration (data that is read-only except by
manual intervention, which must not require restarts) as well as database level objects. The
simulation layer must include rolling file logs which can be fetched by the front-end. There must
also be file backups through SFTP, which backup the database going back 6 months and the last 10
configurations.

## Overview

This project is going to let you manage a virtual garden. You can plant parsley in early or late
spring and harvest it in the same part of summer. You have corn which can be planted in early summer
and can be harvested in early fall, and cucumber which can be planted in early fall and harvested in
late fall. You have unlimited seeds but can only plant up to 5 plants at a time. You may view your
inventory which never decays.

There will be three different components, the simulator engine which will be in Java, a database
layer which will be in Java, and then the website layer which will be in PHP. The database layer
will also contain all configuration except for the location of the database server.

For both the Java layers they will use sane performance considerations to give a more realistic
portrayal of the amount of boilerplate required. There will not be a login system, since there is no
uncertainty that I can implement those already.

## Folder Structure

The folders 'database', 'simulator', and 'website' each correspond to one independent application.
In each they contain a custom folder, which is specific to the seeds project and serves only as an
example, and a shared folder which is software that I expect to reuse in future projects.

There is the additional 'shared-java' folder which contains some code that is shared between the
database and simulator portions.
