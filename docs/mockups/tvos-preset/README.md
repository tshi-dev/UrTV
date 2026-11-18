# The «tvOS» preset's home with UrTV's cards (4.4 beta 2, mockups only)

My part of the joint proposal with the UI session (they write the preset's row, wording, picker, «تخصيص», the upgrade and the weak-TV
rules). Nothing here is built; the owner approves first. The manner is tvOS Launcher's (Apache-2.0), named «tvOS» in the app.

1. `tvos-home-1080.png`: the clock, the greeting and the date where they are today; our three cards (the next prayer with the day's five,
   the weather, a live match) as glass cards above the «tvOS» bar; the focused app's name over the bar alone; «كل التطبيقات» its last tile.
2. `tvos-home-card-focus.png`: the focus on a card: the same focus ring as today, the card a little bigger.
3. `tvos-home-720-large.png`: 720p with the TV's large font: a card's lines wrap, no word is cut; the focus on «كل التطبيقات».
4. `tvos-home-light.png`: the weak TV (Ram.low): no live blur, no shadows, no growth: a flat translucent fill, the ring only.

The bar is today's built «tvOS» way (FavBar.GLASS, 100 %, one row). The photo is one of UrTV's backgrounds; the app icons are neutral
stand-ins (on the TV each installed app's own icon); the teams are plain discs (no club logos). Arabic first and right to left, no
capsules, no «·». A look change goes through HomeLook.change() (the UI session's path), never a new one.
