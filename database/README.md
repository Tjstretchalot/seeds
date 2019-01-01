# Seeds - Database

This is the database layer of the application. In the majority of my projects I employed MySQL as
the database layer without an explicit wrapper. However, by doing so, it became very difficult to
optimize certain queries which did not perform adequately in MySQL (such as delete-heavy tables).
In particular, it would break my backup logic significantly.

The goal of the database layer is to be able to design a robust, testable, modular database which
requires explicit code at every level. The database must not buffer more than 5 minutes of activity
for any action, and must not lose any activity when closed safely. It must backup no less often than
daily, and it must be able to resolve backup requests.

There is no expectation that this can handle concurrency or redundancy, since I believe that is out
of the scope of database design and is a large part of why SQL often performs so much below
expectation for nearly every operation. Furthermore, for my applications I often only have a single
core that can be dedicated to the database, so further concurrency is pointless anyway.

There are three types of tables that I have encountered quite often that it must be able to handle:

- Generic - these are your conventional tables. Most of the activity against a generic table is
selection, with a moderate amount of inserts, a small amount of updates, and almost no deletion.
This is the focus of SQL-based solutions, so it is reasonable to delegate such activity to a sql
wrapper such as MySQL or sqlite. The database can assume that it is the only one using the database.
- Loss-prevention - This is effectively just meant as a quick dump of some data which is sent when
the simulator is asked to shutdown and retrieved again on startup. In the simplest case it might
just be the simulator passing a list of numbers off to the database process, restarting, and
retrieving that list. It's a bit more complicated when you realize that the database might restart
before the simulator gets back online.
- Configuration - These are typically tables that are read-only and uses as a slightly more complex
key/value store. They must be modifiable by the user and should be backed up with the rest of the
database.
