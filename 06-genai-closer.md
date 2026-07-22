# GenAI closer (~10 min) — build the habit that saves you

*[← back to TODO.md](../TODO.md) · [diagrams](architecture.html) · [overview & cheatsheet](index.html)*

Every day this fortnight ends the same way: generate something, then **verify it before you
believe it**. Today's material is unusually good at punishing the unverified answer, because a
Dockerfile that builds and a compose file that starts can still be wrong in ways nothing tells
you about.

**1. Ask for a review of your own file.** Paste **only** your `Dockerfile` — not the compose
file, not your source, not the whole repo — into your assistant with exactly this:

> Here is my Dockerfile: `<paste>`. Review it for image size, build speed, and security.

**2. Read what comes back and expect a few of these.** Common suggestions, all real, none free:

| Suggestion | What it actually costs you |
|---|---|
| Run as a non-root `USER` | Correct and worth doing. Now your app cannot bind ports < 1024 or write where it used to. |
| Pin the base image by digest | Reproducible builds; someone must now bump that digest deliberately, forever. |
| Use `-alpine` for a smaller image | Different libc. JVM behaviour and DNS resolution can differ. Measure before you believe the saving. |
| Add a `HEALTHCHECK` | Useful — but compose already has one, and now there are two truths to keep in sync. |
| Combine `RUN` layers | Smaller image, worse layer caching. You just traded rebuild speed for megabytes. |
| Add `--no-install-recommends`, clean apt lists | Genuinely free. Take it. |

**3. Pick exactly one, and be able to defend the ones you rejected.** Apply a single suggestion,
rebuild, and confirm the stack still comes up:

```bash
docker compose up --build -d
curl -s localhost:8080/api/rates | head -c 120
```

If it broke, you just learned more from the rejection than the adoption.

**4. Now the harder ask — the one where the AI is most likely to be confidently wrong.** In a
**fresh** conversation:

> I have a Spring Boot app and a MySQL container in docker-compose. My app sometimes fails at
> startup with "Connection refused" but works if I restart it. How do I fix this?

You will very likely be offered `depends_on`, a `restart: always`, a retry loop, a
`wait-for-it.sh` script, or a sleep. **You already know the answer** — a healthcheck plus
`depends_on: condition: service_healthy`, because "container started" and "database ready" are
different moments.

Score the answer honestly:
- Did it distinguish *started* from *ready*, or just say "add `depends_on`"?
- Did it reach for `sleep`/`restart: always` — papering over the race rather than fixing it?
- Would its advice have survived your own `docker compose down -v && up` from Task 2?

**5. Write it down.** Three or four lines in `docs/genai-docker-review.md`, committed with today's
work:

- the one suggestion you took, and why
- one you rejected, and what it would have cost
- whether the startup-race answer matched what you proved yourself this morning

> The habit is the deliverable. An assistant that has never run your stack will give you advice
> that compiles, starts, and is still wrong — and today you have the one thing that settles it:
> a stack you can destroy and rebuild in thirty seconds.
