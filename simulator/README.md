# Seeds - Simulator

This folder corresponds with the simulator-portion of seeds. It is written in Java and it must
progress the state of the game by communicating with the database. Upon request, it must be able
to shutdown within 5 minutes, and when it restarts it must continue from where it left off. In this
example project there are no particularly tight loops that might take a long time to evaluate, so
the pause/resume code will seem a bit extreme.

The application should also be able to recover from *most* types of errors. That is to say, if the
application has a bug which does not corrupt any data but loops indefinitely, it should save that
data and shutdown gracefully, such that after the code is updated to remove this indefinite loop,
as much data as possible given the bug is recovered.

In most projects the simulator, database, and website will be running all on the same server.
However, it should be possible to shutdown the simulator and relaunch it on a different computer
where the only configuration necessary is telling it where the database is.
