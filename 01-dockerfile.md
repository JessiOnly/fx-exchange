# 任务 1 — 把它装进盒子（上午，约 65 分钟）

*[← 返回 TODO.md](../TODO.md) · [架构图](architecture.html) · [总览与速查表](index.html) · [参考答案](../solution/) — 先自己思考再看答案*

### 为什么要做这个

你的 API 能跑起来。在*你的*电脑上，用*你的* JDK 21、*你的* Maven、*你的* MySQL，
端口 3306，种子数据完全正确。但坐你旁边的人的机器不是这样，生产环境也不是。

今天，你要停止交付"说明书"（安装 Java 21、再装 MySQL、再……），开始交付机器本身。
今晚，整个交易所——API、数据库、前端、上游数据源——在一台什么都没装的笔记本上，
一条命令全部启动。

**你将掌握的技能**
- 一条一条读写 `Dockerfile`，而不是直接粘贴
- 理解什么是*层（layer）*，以及指令顺序如何决定重建速度
- 发布端口、注入配置、从容器外挂载存储
- 诊断最常见的容器错误：谁的 `localhost`？

### 你将交付的成果

一个新仓库，作为工作区，包含 `fx-app-spring/`，项目根目录有一个 `Dockerfile`，
构建出的镜像无需任何 JDK 或 Maven 即可运行。

---

### 操作步骤

> **本任务不需要 MySQL。** 如果它在运行，先停掉——今天是关于打包的，数据库在任务 2 才加入。
> 以下所有步骤均无需数据库即可验证。

**0. 首要任务：创建你自己的仓库。**

今天你不是在一个项目上添加代码，而是在组装一个由三个服务构成的**系统**，
今晚你将拥有 API、前端和上游数据源同时运行。三个应用意味着三个并列文件夹，
没有一个嵌套在另一个里面。

**每位同学都在自己的仓库里操作。** 在 GitHub 上创建一个名为 `fx-exchange` 的新空仓库
——不加 README、不加 `.gitignore`，什么都不加；你马上要自己推送这些内容。然后：

```bash
mkdir fx-exchange && cd fx-exchange
git init -b main
cp -R <day-package>/challenge/fx-app-spring .
```

Write a `README.md` at the root **with your name in it**. This is your repo, and for the rest of
the week your name on it is how anyone knows whose stack they are looking at:
在根目录写一个 `README.md`，**里面要有你的名字**。这是你的仓库，在接下来的整周里，
你的名字是别人区分各自 stack 的方式：

```markdown
# FX exchange — Week 2 Day 3

**Name:** Your Name Here

| Folder | What it is | Built by |
|---|---|---|
| `fx-app-spring/` | the API and its database | me |
```

Commit and push that starting point to `main` **before you change anything**:

```bash
git add .
git commit -m "chore: fx-app-spring and README"
git remote add origin git@github.com:<your-username>/fx-exchange.git
git push -u origin main
```

Check GitHub: `main` holds `fx-app-spring/` and your README, and nothing else. That is your
baseline — the known-good state you can always come back to.

**Now branch.** Everything you build today rides a branch and lands on `main` through a pull
request, one branch per exercise:

```bash
git switch -c exercise-01
```

> Working directly on `main` is a habit that costs you nothing today, when you are alone in the
> repo, and costs you a great deal in Week 3, when four of you are in one. Build the habit while
> it is free.

Where you are heading, so the shape makes sense from the start:

```
fx-exchange/                 ← your repo root
├── README.md
├── docker-compose.yml       ← Task 2 puts this here
├── fx-app-spring/           ← your API. Its Dockerfile goes at ITS root.
├── fx-monitor/              ← Task 4 adds this, beside — not inside
└── fx-orchestrator/         ← Task 5 adds this, also beside
```

> `fx-app-spring/` is the same project you finished on Day 2 — nothing is lost by starting a
> fresh repo. Everything below happens **inside that folder** unless a step says otherwise.

**1. Build the jar the old way, once, so you know what goes in the box.**

```bash
cd fx-app-spring
./mvnw -q clean package -DskipTests
ls -lh target/*.jar
java -jar target/fx-app-spring-0.1.0-SNAPSHOT.jar
```

**It starts.** With no database anywhere. Check it in a second terminal:

```bash
curl localhost:8080/health          # {"status":"UP"}
curl localhost:8080/api/health/db   # {"status":"DOWN","hint":"Is MySQL running..."}
```

Two different answers about the same app, and both are true. The connection pool is **lazy** —
Spring doesn't dial the database until something actually asks for data, so the app starts
perfectly happily without one. Your own Day-2 probe is the thing that tells you the truth.

> Bank that: **"the process is up" and "the app works" are different claims.** It will cost
> somebody a confused hour on Thursday when a container is green and broken at the same time.

Stop it with Ctrl-C. That jar already contains Tomcat — Boot's plugin put it there.
**It is the only thing the container actually needs to run.**

**2. The naive Dockerfile — write it, run it, and feel what's wrong with it.**

Create `Dockerfile` **inside `fx-app-spring/`** — each app owns its own, at the root of its own
folder. Run every `docker build` in this task from inside `fx-app-spring/` too, so the `.` context
is that project and nothing else:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/fx-app-spring-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t fx-app-spring:0.1 .
docker images fx-app-spring
docker run fx-app-spring:0.1
```

It boots. Now open a second terminal and try to reach it:

```bash
curl localhost:8080/health
```

**Nothing.** The app is listening on port 8080 *inside its own network namespace*. Nothing on
your laptop is. A container does not get your machine's ports by existing — you have to say so.

Stop it (Ctrl-C) and run it again, publishing the port:

```bash
docker run -p 8080:8080 fx-app-spring:0.1
curl localhost:8080/health          # {"status":"UP"}
curl -i localhost:8080/             # 404 — alive, just nothing mapped there
```

`-p 8080:8080` — **left is your laptop, right is inside the container.** They are two different
numbers that happen to match here. They will not match later today.

**3. Now the failure worth having. Ask it for data:**

```bash
curl -i localhost:8080/api/rates
```

A `500`, and a deliberately uninformative body — that's the error hardening you added on Day 2
doing its job. The real answer is in the *container's* log:

```bash
docker logs <container-id> | grep -iE "connection|jdbc"
```

`CannotGetJdbcConnectionException` … `Connection refused`. And your own probe agrees:

```bash
curl localhost:8080/api/health/db
```

So far, so unsurprising — there is no MySQL running. But look at **where** it went looking. The
app's `application.properties` says `jdbc:mysql://localhost:3306/fxdb`, and this is the part that
catches everybody:

> **`localhost` inside a container is the container.** Not your laptop. Even if you started MySQL
> right now, on this machine, on 3306, this container still would not find it — it is searching
> its own four walls. The container has its own network namespace, and `localhost` means "me".

Sit with that, because it is the single most common container mistake and you will make it again
in a different costume in about an hour.

**We are not going to fix it here.** The cheap fix is to point the app back out at your laptop,
which just re-creates the problem the whole day is about: an app that only runs next to something
you installed by hand. Task 2 does it properly — the database gets its own container and the two
talk over a network they share.

*(Optional, if you happen to have a local MySQL running and want to prove the diagnosis:
`-e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/fxdb` and the 500 becomes data.
`host.docker.internal` is Docker Desktop's name for "the machine hosting me"; on plain Linux you
need `--add-host=host.docker.internal:host-gateway`. Skip it without guilt — Task 2 supersedes it.)*

**4. Go multi-stage.** The image you just built has a problem you can't see: it only works
because *you* ran `./mvnw package` first. Someone cloning your repo gets a build failure. And a
`.jar` filename is pinned in the Dockerfile, so the version bump breaks it.

Replace the whole file:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t fx-app-spring:1.0 .
docker images fx-app-spring
```

Two stages, one image out. The `build` stage carries Maven, a full JDK, your source and the
whole `.m2` cache — **and none of it ships.** Only `app.jar` crosses the `COPY --from=build`
line. Note the `*.jar` glob: no version pinned any more.

`EXPOSE 8080` documents the port. It does **not** publish it — `-p` still does that.

**5. Prove the layer cache, because it is the reason those two `COPY` lines are in that order.**

Rebuild with nothing changed:

```bash
docker build -t fx-app-spring:1.0 .
```

Every step says `CACHED`. Now touch a source file and rebuild:

```bash
touch src/main/java/com/fx/api/web/RateController.java
docker build -t fx-app-spring:1.0 .
```

Watch which steps re-run. `COPY pom.xml` and `dependency:go-offline` stay **CACHED**; everything
from `COPY src` down re-runs.

Now change `pom.xml` (bump `<description>`), rebuild, and watch `dependency:go-offline` re-run
too — it sits above a layer whose input changed, so its cache is invalid.

> **Docker re-runs a layer when the files it copies have changed, and every layer after it.**
> Dependencies change rarely; source changes constantly. That is the entire reason `pom.xml` is
> copied on its own, before `src`. Get this order wrong and every one-character fix
> re-downloads the internet.

**6. Add a `.dockerignore`.** Before Docker runs a single instruction it uploads the *build
context* — everything in the folder — to the daemon. Check what you're sending:

```bash
du -sh .
```

Most of that is `target/` and `.git/`. Create `.dockerignore`:

```
target/
.git/
.github/
.idea/
*.iml
docs/
README.md
```

Rebuild and watch the `transferring context` line shrink. This is not only speed: whatever lands
in the build context can end up in an image layer, so keeping secrets and junk out of it is a
security habit, not a tidiness one.

**7. Change the app's behaviour without rebuilding it.** The image is fixed; its *configuration*
is not. Move the app to a different port inside the container:

```bash
docker run -p 8080:9090 -e SERVER_PORT=9090 fx-app-spring:1.0
curl localhost:8080/health
```

Still answers on 8080 — because you published `8080:9090`. Check the log: `Tomcat started on
port 9090`. **Same image, different behaviour, nothing rebuilt.** Spring's relaxed binding turns
`SERVER_PORT` into `server.port`, and the environment beats the properties file. That is
config-not-code, one layer further out than `application.properties` — and it is exactly the
mechanism Task 2 uses to hand the app a database address.

**8. Mount storage from outside.** Containers are disposable — anything written inside one dies
with it. Run with a **named volume** for logs and a **read-only bind mount** of `ops/`:

```bash
docker run -p 8080:8080 \
  -v fx-logs:/app/logs \
  -v "$PWD/ops:/app/ops:ro" \
  fx-app-spring:1.0
```

Two different things, deliberately in one command:
- `fx-logs:/app/logs` — a **named volume**. Docker owns it, it lives outside the container, it
  survives `docker rm`. Confirm with `docker volume ls`.
- `"$PWD/ops:/app/ops:ro"` — a **bind mount**. A real directory on your laptop, appearing inside
  the container, **read-only**. Change a file on the host and the container sees it immediately.

Prove both:

```bash
docker exec $(docker ps -q --filter ancestor=fx-app-spring:1.0) ls /app/ops
docker exec $(docker ps -q --filter ancestor=fx-app-spring:1.0) touch /app/ops/nope
docker volume ls | grep fx-logs
```

The `touch` is refused: `Read-only file system`. That `:ro` is doing real work — and mounting
your seed data read-only is how you make sure a container can never corrupt the thing it was
given.

**9. Learn the everyday commands.** With the container running, in another terminal:

```bash
docker ps                     # what's running
docker ps -a                  # ...including what died
docker logs <container-id>    # its stdout, after the fact
docker exec -it <id> sh       # a shell INSIDE it — try `ls /app`, `ps`, then `exit`
docker stop <id>
docker rm <id>
docker images                 # what you've built
docker rmi fx-app-spring:0.1  # bin the naive one
```

Inside that `exec` shell, run `ps`. You will see about two processes. **That is the container:**
not a machine, just your process with its own view of the world.

**Checkpoint.** No database anywhere. From a clean `docker run -p 8080:8080`:

```bash
curl -s localhost:8080/health          # {"status":"UP"}
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/       # 404 — alive, nothing mapped
curl -s localhost:8080/api/health/db   # {"status":"DOWN", ...}
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/rates   # 500
```

That combination **is** the pass mark: your app is running inside a container, on a machine with
no JDK, no Maven and no MySQL involved in running it — and it is honest about the one thing it
still can't reach. `docker images` shows the multi-stage image is markedly smaller than the
single-stage one, and a rebuild after touching a source file leaves `dependency:go-offline`
`CACHED`.

The 10 rates and EUR/USD 1.0818 arrive in **Task 2**, when the database joins the stack.

**10. Ship it.** The work is done and proven — now land it on `main`, the same way every
change lands on `main` for the rest of this course.

```bash
git add -A
git commit -m "feat: containerise fx-app-spring"
git push -u origin exercise-01
```

Open a pull request from `exercise-01` into `main` on GitHub, and merge it. Then bring your local
`main` back in line before you start the next task:

```bash
git switch main
git pull
```

> One branch per exercise, every exercise merged through a PR. Nobody commits straight to `main` —
> not today, and not in Week 3 when it is four of you in one repo instead of one of you alone.

<details>
<summary>Stuck?</summary>

**`COPY target/...` says "not found".** You're on the naive Dockerfile and haven't run
`./mvnw package`, or `target/` is in `.dockerignore` (it should be — that's why step 4 stops
needing it).

**`/api/rates` returns 500.** Correct — there is no database yet, and step 3 explains why the
container couldn't reach one even if you started it. Task 2 fixes it. `/health` and
`/api/health/db` are your checkpoints today.

**`port is already allocated`.** Something already holds 8080 — an old container
(`docker ps`), or your app still running in IntelliJ. Stop it, or publish on another host port:
`-p 8090:8080`.

**Everything says CACHED even though I changed a file.** You changed a file that `.dockerignore`
excludes, so the context Docker sees is identical.

**Ctrl-C doesn't stop it.** You started it detached (`-d`). `docker ps` then `docker stop <id>`.
</details>
