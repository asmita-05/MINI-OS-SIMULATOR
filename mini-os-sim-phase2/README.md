# Mini OS Simulator — Phase 2 (70% Complete)

> **Phase 2 of 3** — Core features implemented and working.
> Advanced features (SRTF, MLQ, Paging, Compare) coming in Phase 3.

---

## What Is Done (Phase 2 — 70%)

| Subsystem | Status | Details |
|-----------|--------|---------|
| Process Management | ✅ Complete | PCB, 5-state model, create/list/remove/info |
| FCFS Scheduling | ✅ Complete | Non-preemptive, Gantt chart, all metrics |
| SJF Scheduling | ✅ Complete | Non-preemptive, shortest burst first |
| Round Robin | ✅ Complete | Configurable time quantum |
| Priority (NP) | ✅ Complete | Non-preemptive priority scheduling |
| First-Fit Memory | ✅ Complete | Contiguous allocation + coalescing + visual map |
| Virtual Filesystem | ✅ Complete | mkdir, touch, write, cat, rm, ls, cd, pwd |
| Swing GUI | ✅ Complete | 4-panel dark-theme GUI with Gantt chart |
| CLI Shell | ✅ Complete | Interactive ANSI shell |

---

## Coming in Phase 3 (Final)

| Feature | Status |
|---------|--------|
| SRTF — Preemptive SJF | 🔲 Phase 3 |
| Priority Preemptive | 🔲 Phase 3 |
| MLQ — Multilevel Queue | 🔲 Phase 3 |
| IO-RR — I/O Aware Round Robin | 🔲 Phase 3 |
| Best-Fit Memory | 🔲 Phase 3 |
| Worst-Fit Memory | 🔲 Phase 3 |
| Paging Memory Model | 🔲 Phase 3 |
| Compare All Algorithms | 🔲 Phase 3 |
| Batch Script Runner (.ossim) | 🔲 Phase 3 |
| mv / find / tree / append | 🔲 Phase 3 |

---

## Build & Run (Windows)

```
cd C:\mini-os-sim
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -d out @sources.txt
echo Main-Class: Main > MANIFEST.MF
cd out && jar cfm ..\mini-os-simulator.jar ..\MANIFEST.MF . && cd ..
java -jar mini-os-simulator.jar
```

---

## Requirements
- JDK 17 or higher
- Windows / Mac / Linux terminal
