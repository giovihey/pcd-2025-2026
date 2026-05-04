# Assignment #02 — FSStat Report

**PCD a.y. 2025-2026 — ISI LM UNIBO — Cesena Campus**

---

## 1. Problem Analysis

The core task is to recursively walk a directory tree, stat each file, and accumulate a size-distribution report. From a **concurrent programming** perspective, the key observations are:

**I/O bound, not CPU bound.** Reading directory entries and file metadata involves blocking system calls. The bottleneck is waiting on disk/OS, not computation. This means concurrency pays off even on a single CPU core, and the right model is one that can overlap many pending I/O operations rather than parallelizing computation.

**Natural tree-shaped parallelism.** Each subdirectory can be explored independently. The traversal naturally decomposes into a tree of tasks where siblings are independent and parents must wait for all their children before being considered complete.

**Shared mutable state.** All tasks write into the same report (counters per band). This is a classic concurrency hazard: updates must be atomic, or otherwise serialized, to avoid lost increments.

---

## 2. Strategy per Approach

### 2.1 Event-Loop (async/await — Node.js)

**Strategy:** `walkDir` is a recursive async function that calls `readdir` and, for each entry, `lstat`. Subdirectories are explored by spawning recursive calls collected into a `Promise.all`, so all siblings are traversed concurrently within each level. Files are handed to a `ReportBuilder` callback synchronously as they are discovered.

The `ReportBuilder` class accumulates state in plain mutable fields — this is safe because Node.js has a single-threaded event loop: callbacks never run truly in parallel, so no atomic operations are needed.

`getFSReport` simply `await`s the full traversal and returns the built report.

**Key design choice:** concurrency is achieved via `Promise.all` at each directory level. The event loop interleaves the many pending `readdir`/`lstat` operations without blocking.

---

### 2.2 Reactive (RxJS — Node.js)

**Strategy:** `walkDir` returns an `Observable<{fullPath, stats}>` that emits one item per file. It is built using `from(readdir(...))` piped through `mergeMap` to recursively expand subdirectories — `mergeMap` subscribes to all inner observables concurrently, naturally mirroring the `Promise.all` structure of the event-loop version but in a declarative, composable style.

`getFSReport` pipes the file stream through `scan`, which folds each incoming file into an accumulator and **emits an updated report after every file**. This makes the reactive version inherently live: a subscriber can react to each intermediate snapshot.

**Key design choice:** `scan` replaces the mutable `ReportBuilder` with a pure fold, producing a new immutable report object on each step. The observable returned by `getFSReport` can be subscribed to for live updates, or `.pipe(last())` can be added if only the final result is needed.

---

### 2.3 Virtual Threads (Java 21)

**Strategy:** `FSStatLib.getFSReport` creates a `newVirtualThreadPerTaskExecutor` and launches `traverseDirectory` on it. Each call to `traverseDirectory` runs on a virtual thread: it opens a `DirectoryStream`, iterates entries synchronously (reading attributes inline), records regular files directly on the shared `FSStatAccumulator`, and spawns a new virtual thread for each subdirectory. It then **blocks** with `CompletableFuture.allOf(...).join()` waiting for all children.

Blocking here is intentional and cheap: virtual threads yield their carrier thread when blocked, so the OS thread is free to execute other virtual threads. This is the key advantage of virtual threads — sequential, blocking code reads simply, while the runtime provides high concurrency for free.

`FSStatAccumulator` uses `AtomicLong` and `AtomicLongArray` for the counters, since multiple virtual threads may call `recordFile` truly in parallel on different OS carrier threads.

**Key design choice:** unlike the other two approaches, shared state **does** require atomic operations here. The accumulator is the synchronization boundary; the traversal itself is parallel.

---

## 3. Optional Extension — Interactive GUI

The interactive GUI is built on top of the **reactive version**, which is the natural fit: `getFSReport` already emits a live stream of intermediate report snapshots via `scan`, so attaching a GUI requires no changes to the library itself.

**Architecture.** A lightweight Node.js HTTP server (`server.js`) bridges the RxJS observable and the browser using **Server-Sent Events (SSE)**. When the user clicks Start, the browser opens a persistent `EventSource` connection to `/report`; the server subscribes to `getFSReport` and forwards each `scan` emission as an SSE `update` event. The browser receives each event and redraws the bar chart and file counter in real time. When the scan completes, the server sends a `complete` event and closes the connection.

**Stop.** Clicking Stop sends a `POST /stop` to the server, which calls `subscription.unsubscribe()` on the active RxJS stream. This immediately halts the traversal mid-scan. The `EventSource` is also closed on the browser side so no further events are processed.

**Throttling.** Because `scan` emits on every single file, large directories would flood the browser with hundreds of events per second, causing the UI to lag. A `throttleTime(100, undefined, { leading: true, trailing: true })` operator is inserted in the server's subscription pipeline, capping updates to at most one per 100 ms. The `trailing: true` option guarantees the final totals are always delivered even if the scan completes inside a silent window.

---

## 4. Comparison Summary

| Aspect | Event-Loop | Reactive | Virtual Threads |
|---|---|---|---|
| Language | JavaScript (Node.js) | JavaScript (Node.js) | Java 21 |
| Concurrency mechanism | `Promise.all` | `mergeMap` | Virtual thread per directory |
| Shared state safety | Not needed (single-threaded) | Not needed (single-threaded) | `AtomicLong` / `AtomicLongArray` |
| Live updates | No (final only) | Yes (`scan` emits per file) | No (future resolves at end) |
| Code style | Imperative async | Declarative / functional | Sequential blocking |
| Error handling | `try/catch` + error list | Observable `error` channel | `try/catch` in virtual thread |