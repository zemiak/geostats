# geostats
Geocaching Statistics generated from your Found GPX

## Todoist integration

This dumps tasks for Todoist for specified missing caches. It expects a gc.com "found caches" dump taken from [Pocket Queries](https://www.geocaching.com/pocket/default.aspx), section *My Finds*.

### Switches

```
Usage: parse [-fhrvVy] [-d=<daysText>] [-m=<monthsText>] <fileName>
Parses GPX of my-finds from Project-GC
      <fileName>          The filename of myfinds-NNN.gpx
  -d, --days=<daysText>   Day numbers list (1-12). You can specify multiple:
                            1,2,4. Default: all
  -f, --full              Full dump of a day. Default: only missing
  -h, --help              Show this help message and exit.
  -m, --months=<monthsText>
                          Month numbers list (1-12). You can specify multiple:
                            1,2,4. Default: all
  -r, --regular           Include regular caches
  -v, --verbose           Print the raw data from xml for the date(s) specified
  -V, --version           Print version information and exit.
  -y, --yellow            Include yellow/multi caches
```

### Example

Dump regular for March

```
cd src/main/todoist
jbang todoist.java 23322291.gpx -r -m 3
```
