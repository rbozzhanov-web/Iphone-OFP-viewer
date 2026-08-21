# OFP Viewer — the flight plan on the phone

The operational flight plan, read rather than filled in. It is for the hour
before the flight — in the car, on the bus, in the crew room — when the question
is what is in this plan, not what has to be written into it.

The whole app is `index.html`. The PDF is parsed on the device: no network after
installation, no account, no key, nothing uploaded anywhere. Nothing is ever
written back into the document either — there is no ETO or ATO column, no
altimeter checks, no export. It reads.

Requires iOS 16.4 or newer (`DecompressionStream` is needed to inflate PDF
streams). It works the same on an iPad, on a Mac and on a PC; the layout is
built for a phone held in one hand.

> This is the reader half of the [OFP
> Companion](https://github.com/rbozzhanov-web/ETO-ATO-filler), and it carries
> that app's own PDF engine and parsers — the same plans read the same way. The
> companion is what fills the document in and saves it; this is what reads it.

> **There is also an app.** `ios/` holds a SwiftUI shell around this same page,
> built into an `.ipa` on GitHub's macOS runners, for putting the viewer in
> another pilot's hands through TestFlight. It is unsigned and needs an Apple
> Developer account to go further — see [`ios/README.md`](ios/README.md). Added
> to the home screen from Safari, the page needs none of it.

---

## Publishing it

The app is five static files and needs no build step, so GitHub Pages serves it
as it stands:

1. **Settings → Pages** in this repository.
2. Under *Build and deployment*, source **Deploy from a branch**, branch
   **main**, folder **/ (root)**. Save.
3. Wait a minute, then open the address Pages gives you —
   `https://<account>.github.io/Iphone-OFP-viewer/`.

That address is the app. Every push to `main` republishes it.

The page itself is fetched from the network whenever there is one, so a new
version is picked up on the next launch rather than waiting on a service-worker
update check; if the network does not answer within 2.5 seconds the app starts
from its cache instead, so a slow link never delays it. A change to
`index.html` therefore needs nothing else done to it. Bump the `V` constant in
`sw.js` when one of the other cached files changes — the manifest or an icon —
since those are served from the cache first and a new generation is what
replaces them.

Offline it never updates — the version you leave the ground with is the version
you fly with.

## Putting it on the iPhone

Worth doing once: it then runs offline and behaves like a normal app rather than
a page.

1. Open the address in **Safari** — Chrome cannot add to the home screen.
2. Share button → **Add to Home Screen** → Add.
3. Launch it from the new icon. Try it once in airplane mode to confirm the
   offline cache is in place.

## Getting the plan in

Tap the box and pick the file: the plan is usually already on the phone, in
Files or in an attachment saved from Mail. A PDF dropped anywhere on the page
works too, on a device that can drag one.

The plan is then kept on the device, so opening the app again brings back the
same document without going to look for the file.

The house in the header returns to this screen with the plan still loaded, so
another one can be chosen — or **Back to the plan** returns to the one open.
**Reset** puts the document down for good: the plan, what was decoded from it,
and the copy kept for the next launch. The app then opens empty.

---

## Moving about it

The five tabs are turned by **swiping sideways** across the page, or by tapping
one in the strip. The page goes with the hand as it moves — at the speed of the
hand, fading as it goes — and is carried the rest of the way only once let go; a
short drag springs back and changes nothing, and the ends of the strip give a
little and return rather than refusing. A swipe that begins on something with its
own hold on the gesture — a chart, a line set wider than the screen — belongs to
that thing. Two fingers are never a swipe. Arrow keys turn the tabs on a
computer.

**Pinching zooms the chart, not the app.** Left to the system, two fingers on a
sheet pinch the whole interface — header, tabs and all — and the sheet is no
easier to read for it. So the chart box takes the gesture for itself: the sheet
is sized and moved on its own while the app around it holds still, and two taps
go in on what is under them and back to the whole sheet. Everywhere else there is
nothing to zoom and the system's own pinch is left alone.

## On its side

A phone turned sideways has four hundred points of height instead of eight
hundred, and chrome sized for the other shape takes two fifths of it. So in
landscape the bar is rebuilt: the clearance it keeps above itself in portrait is
for the island, which in landscape is at the side and wants none — that goes
first. Then the strip of tabs and the document's name, each of which takes a
whole row in portrait, share one, the name at the far end where it is out of the
way of the tab being reached for and still there to be read.

That is a quarter of the screen for the chrome instead of two fifths, and the
whole of a wind-component sheet on one screen without scrolling. Nothing is
dropped or shortened — an iPad on its side has the height for the ordinary
layout and keeps it.

## How it looks

The chrome is glass. The plan runs under the header rather than stopping at it,
and the header is a tinted, blurred sheet over whatever is passing beneath —
with the scroll edge the system uses: at the top of a tab there is nothing behind
the bar to show through, so it carries no blur and no rule and reads as part of
the page; the moment the plan starts to pass under it, the glass and the hairline
come up.

Controls are capsules. The toolbar buttons are round, the tab in hand is a
capsule lit along its top edge, and the cards are the system's grouped lists —
filled, wide-cornered, hairline-separated — rather than drawn boxes. Print
colours are untouched by any of it: what a figure means is never carried by how
it is lit.

Where **Reduce Transparency** is on, or the browser cannot make glass at all, the
chrome is simply solid. Where **Reduce Motion** is on, tabs do not slide. Nothing
is lost in either case but the effect.

## What is on the five tabs

### Flight

The page you look at first. The two aerodromes across the top with their names,
then the callsign, type, registration, date of flight and cost index; the route
ID, request number and release time under them, so the document on screen can be
checked against the one you were given.

Then STD / ETD / STA / ETA and the trip time, as figures big enough to read at
arm's length, with the time to STD counted against the clock — **STD in 3 h
12 min** — worked out from the DOF in the flight plan. Where the plan carries no
DOF the count says so, since it is then only good to the nearest half day.

Under that the weights, each against the certificate limit printed on the line
above it in the document:

```
TOW                    145 979
        −41 021 vs MTOW 187 000
```

so the margin is read rather than worked out. Then the fuel block as the plan
prints it, TRIP through BLOCK, with the planned remaining at destination taken
from the last waypoint of the main route. Last, the flight plan in brief: cruise
speed and level, alternates, en-route alternate, the FIR boundaries the plan
crosses, equipment, PBN, SELCAL.

### Route

Every waypoint with the leg times the plan gives it. The times down the right are
counted from the plan's own off-block time, which is named at the top of the
table. They are the plan's leg times, not a position report, and nothing on this
page is entered.

Each waypoint carries its running total and the planned fuel remaining, its
level and its wind, and the rest of what is printed on its two lines — airway,
tracks, speeds, distance to go, position — as a strip underneath. The position
goes at the end of that strip: it is the longest thing on the line and the least
use at a glance, so it is what a narrow screen clips. The alternate route
follows under a heading of its own. A leg still climbing shows CLB for its level,
as the form prints it, rather than a figure it does not have.

**SR** is shown beside each waypoint where the plan carries it — the figure
printed in the ground speed's column, on the line under it. **05 and above is
marked red**, so a rough stretch is seen rather than looked for.

The level, the wind and SR all come off the headings the form prints over them,
which is the only way to be sure which figure is which. The headings are found by
the column the ETO and ATO are written in — the two the whole table is built
around — and taken as the pair of lines they are, in the order they are printed.
That order is the point: the table is two lines to the waypoint and its headings
are laid out the same way, ETO over ATO and G/S over SR, so the upper heading
line names the upper line of every waypoint and the lower names the lower. Read
the wrong one of the two and a column gives up its neighbour's figure.

Nothing is counted along the row. A waypoint with no airway prints one token
fewer than its neighbour, and whole rows shift a character sideways where a
figure runs wider, so a column is matched on the page it covers instead. Where a
row leaves a column empty, empty is the answer: reaching for the nearest figure
is how a column comes to show its neighbour's.

Guessing from the shape of a figure is the last resort, for a form that heads
nothing. It is a poor one — a ground speed is a three-figure number in the same
range as a level, and a pair of figures over a slash is as likely to be the
magnetic track as the wind.

Where the plan's own leg times do not add up to its running total, the tab says
so at the top and names how many waypoints disagree.

### Weather

The METAR, TAF and NOTAMs the package carries, one aerodrome at a time. The
aerodromes of this flight are chips across the top for a single tap; the
dropdown under them holds everything the package covers, in three groups — this
flight, areas along the route, other aerodromes.

METAR and TAF are shown raw as printed, then each NOTAM with its number,
validity and subject line above the text, then the company NOTAMs. A busy
aerodrome runs to eighty-odd NOTAMs, so the first eight are shown and the rest
open on a tap.

Everything here is as old as the document. Re-brief from the current source
before acting on any of it.

### Charts

The full-page sheets in the package — wind components, tropopause and MORA
profile along the route, and the significant weather charts with the route drawn
on them. The images are pulled straight out of the PDF, so nothing is re-rendered
or re-compressed. **Pinch** sizes the sheet and a drag moves about it, with the
app around it held still; **two taps** go in on what is under them and back out
to the whole sheet; **Fit** puts it back from the toolbar. The sheet is held at
its edges rather than let go into the page, and a wheel or a trackpad does the
same on a computer.

### ICAO

The flight plan itself, reassembled into one text where it is printed across a
page break, with the page headers stripped out. Copy puts it on the clipboard as
a single line.

---

## What is inside

No external libraries — no pdf.js, no pdf-lib, no CDN. `index.html` carries the
OFP companion's own minimal PDF engine, reading only: it reads text with
coordinates out of FlateDecode streams, and the page images for the charts. The
overlay writer is left out, because this app has no use for it, and the dotted
blanks the companion fills in are not read at all — they are places to write.

Built for Air Astana plans: unencrypted PDF, classic xref table, uncompressed
objects, Courier font. A document of another shape is refused on load with the
reason, rather than half read.

The theme switch uses the key the OFP companion uses. Served from the same host
as that app — which is what github.io does for one account's repositories — a
phone carrying both does not switch to dark on one and light on the other. The
plan itself is kept under keys of this app's own, so nothing it does can disturb
a plan open in the companion on the same device.

One reading differs from the companion's: a NOTAM that has no subject line of
its own is left without one, rather than being printed under the previous
NOTAM's.

Built by **Ramil Bozzhanov 9871** with Claude.
