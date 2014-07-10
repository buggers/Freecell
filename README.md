Freecell. No frills yet. Written with Swing (works on PC and Mac).

In honor of my dad -- this is the only computer game he'll play.

See the comments on Game.java for the latest bugs and version notes.
The latest commit is non-functional (I was transferring computers and needed to quickly save what I had) -- the one before that works fine.
   
Future features:
* - Give user option to enable or disable auto moves to ace stacks
 * -Add custom graphics when moving a stack, so it's clear what you're moving
*  -Unshare the transfer handlers, or at least change the icon each time so that
   -the transfer image matches the card being moved; optionally, set the cardbutton
itself to be invisible so it looks like you're moving it.
     -Add deal button so you can replay if you have no moves left
     -(Don't auto-exit upon win.) This'd require nulling out/clearing a large
     -amount of variables.
 * -Double clicking a playstack button moves it to a freecell
 * -Add testing. 
 * -Better graphics
 * -Card moving animation
   -Selection is pale grey on a PC -- should be green.
   -Review JavaDocs for clarity, completeness; eliminate excess or irrelevant comments
 
Possible future directions:
 * AI to solve all games (or declare them unwinnable)
 * Separate logging output for verbosity
 * Check if there are no possible moves or one possible move

Finished:  (v. 0.16)
* Fixed drag-and-drop-any-card bug, miscellaneous bugs
* Auto-move of aces and other cards when it would have no detrimental
  game impact (some would not like this, so this could be an option)
* Auto-move of stacks if cells are available via drag-and-drop

Finished: (v. 0.15)
-Implemented drag and drop
