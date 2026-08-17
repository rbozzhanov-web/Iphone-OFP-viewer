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

To release a new version, bump the `V` constant in `sw.js` in the same commit as
the change to `index.html`. The page itself is fetched from the network whenever
there is one, so a new version is picked up on the next launch rather than
waiting on a service-worker update check; if the network does not answer within
2.5 seconds the app starts from its cache instead, so a slow link never delays
it. Offline it never updates — the version you leave the ground with is the
version you fly with.

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
same document without going to look for the file. **Open** in the header — the
second button — takes another one, and offers the way back if you change your
mind.

---

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

Every waypoint with the leg times the plan gives it. The times down the right
are counted from the off-block time in the box at the top; it starts at the
plan's own ETD and the whole column follows if it is changed, which is how a
delay is read off the table without arithmetic.

Each waypoint carries its running total and the planned fuel remaining, the
level and the wind where the form's own columns can be recognised beyond doubt,
and the rest of what is printed on its two lines — airway, temperature,
distance, ground speed, position — as a strip underneath. The position goes at
the end of that strip: it is the longest thing on the line and the least use at
a glance, so it is what a narrow screen clips. The alternate route follows under
a heading of its own.

**SR** is shown beside each waypoint where the plan carries it — the figure in
the ground speed's column, on the line under it.

Columns are found by the heading printed over them and read off by where that
heading sits across the page, not by counting along the row: a waypoint with no
airway prints one token fewer than its neighbour, and counting would then take
the wrong figure. The headings are read as a block and kept in the order they
are printed, because the table is two lines to the waypoint and its headings are
laid out the same way — ETO over ATO, G/S over SR — so the upper heading line
names the upper line of every waypoint and the lower names the lower. Where the
form heads only the G/S, SR is taken as whatever stands in that column on the
waypoint's other line. A plan that has neither shows no such column.

The level is read the same way where the form heads it, and only guessed at from
the shape of the figure where it does not — a ground speed is a three-figure
number in the same range as a level, and the two are otherwise easy to confuse.

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
or re-compressed. **Pinch** to zoom and drag to move about the sheet, **double
tap** to go in and back out, **Fit** to put the whole sheet back on the screen.
The sheet is held at its edges rather than let go into the page, and a wheel or a
trackpad does the same on a computer.

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
