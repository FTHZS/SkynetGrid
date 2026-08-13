# SkynetGrid

A distributed remote-administration system written in Java — a **Server** hub, lightweight **Node** clients, and a Swing **GUI** admin console, communicating over a custom TCP/UDP protocol.

Built to learn real networking (LAN discovery, leader election, socket protocols, concurrent I/O) by solving an actual problem instead of a toy one, and deployed — with school approval — on a computer lab, where it evolved over 40+ iterations into a full lab-administration console.

> **Context:** this was deployed only on institution-owned lab machines, with sign-off from lab staff, for lab administration purposes. It is not intended for use on machines you don't have authorization to manage.

---

## What it does

- **Auto-discovery & self-hosting hub** — Nodes find the Server over LAN broadcast (UDP); if none is running, a node elects itself and boots the Server locally. No manual server setup on any given machine.
- **Live screen viewing** — Nodes stream their screen to the console over TCP, with perceptual-hash change detection so idle screens don't waste bandwidth, and repaint rate that scales with whether the viewer window is focused.
- **Remote input control** — Mouse and keyboard events captured on the console are relayed to the target node and replayed there via `java.awt.Robot`.
- **Remote file management** — A full file explorer (browse, download, upload, rename, delete, zip-and-download folders) over the same connection, plus broadcast file/folder push to many nodes at once.
- **Remote command execution** — Run arbitrary terminal commands on a target node and get the output routed back to the console.
- **Screen recording** — Capture a live view to a hand-written AVI (RIFF/MJPEG) container, no external codec dependencies.
- **Self-healing deployment** — Runs as a `systemd` user service so it starts on boot and restarts on failure; nodes periodically back up their own install and can restore/relocate it if the install directory goes missing, since it's meant to run continuously as an always-on service rather than something toggled casually.

## Architecture

```
        ┌────────────┐        UDP discovery/broadcast        ┌────────────┐
        │   Node A   │ ───────────────────────────────────▶  │   Node B   │
        └─────┬──────┘                                       └─────┬──────┘
              │  TCP control + file port                            │
              ▼                                                     ▼
                          ┌──────────────────────┐
                          │        Server         │
                          │  (elected by a Node)  │
                          └──────────┬─────────────┘
                                     │
                          ┌──────────▼─────────────┐
                          │      GUI console        │
                          │ (runs its own admin Node)│
                          └──────────────────────────┘
```

- Any machine can run just a `Node` (managed endpoint), or a `Node` + `GUI` (admin console) — the console starts its own privileged Node internally rather than talking to the network as a separate role.
- The **Server** isn't a fixed machine — the first node to fail discovery locks an election port and boots the hub itself. Later nodes just connect to whoever's already running it.
- Control messages, screen frames, and file transfers each run on separate connections so a large file transfer doesn't stall a live view or an input event.
- Async request/response calls (`CompletableFuture`, keyed per request) let a node fire a query and keep handling other traffic while it waits on a reply.

## Tech

Java · Swing · raw TCP/UDP sockets · `java.awt.Robot` · `systemd` (user services) · hand-rolled binary protocol · hand-rolled AVI container writer

## What I'd do differently

- The console (`GUI.java`) grew to ~2,500 lines of mostly static methods handling UI, networking, and business logic together — extracting a real message/command abstraction earlier (instead of hand-parsed pipe-delimited strings) would have made every later feature cheaper to add.
- Auth is a shared local password checked client-side, not something the Server independently verifies — fine for a low-stakes lab environment, but not a pattern I'd reuse anywhere the stakes are higher.
- No real version control during early development — 40+ folder-per-version snapshots instead of git history, including one abandoned experimental branch. Later projects use git from day one.

## Project history

Started as a simple way to grab my own project files across lab machines without re-authenticating to cloud storage each time (`Send File` was one of the first features). Grew into a full lab-administration console as I kept learning more networking concepts and had lab-staff buy-in to extend its scope.

---

*Built by [Abhinav Biju](https://abhinavbijuportfolio.onrender.com) — first-year CSE @ VIT Chennai.*
