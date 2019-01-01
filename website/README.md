# Seeds - Website

This folder corresponds with the website-portion of seeds. It is written in PHP and communicates
with both of the other layers. It displays information about the state of the simulation by going
through the database layer, but it is also capable of fetching the current logs directly by going
to the simulator. To figure out where the simulator is located, it must ask the database.
