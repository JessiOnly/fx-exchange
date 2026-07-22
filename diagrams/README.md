# Diagrams — the system after each exercise

Nine images, in order. Drop them straight into slides, print them, or just open the folder.

| Image | Shows |
|---|---|
| `00-title.jpg` | how to read the diagrams (the legend) |
| `01-where-we-start.jpg` | a jar and four hand-installed things on one laptop |
| `02-task1-dockerfile.jpg` | the app in a container, and why it can't reach a database |
| `03-task2-compose-mysql.jpg` | app + MySQL on one compose network |
| `04-task3-liquibase.jpg` | Liquibase builds the schema; the seed mount is gone |
| `05-task4-fx-monitor-ports.jpg` | nginx serves the page and proxies `/api` |
| `06-task5-orchestrator.jpg` | five services, two databases, the loop closed |
| `07-feed-loop-sequence.jpg` | the rate feed as a sequence, incl. the DECLINED branch |
| `08-done-fx-monitor.jpg` | what "done" looks like in the browser |

These are C4 **container** diagrams: a "container" is a separately runnable thing (an app, a
database, a web server). It lines up with a Docker container here, which is convenient but a
coincidence.

Regenerated from `../architecture.html` — that page is the source; open it if you want to
present them as a deck (arrow keys or the on-screen controls) or edit a diagram.
