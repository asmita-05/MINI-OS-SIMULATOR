# Mini OS Simulator

Mini OS Simulator is a Java-based operating system simulation project with both a graphical interface and a command-line interface. It brings together core OS concepts in one place so you can experiment with process management, CPU scheduling, memory allocation, a virtual file system, and inter-process communication.

## Features

- Process creation and lifecycle management
- CPU scheduling simulation
- Memory allocation and memory map visualization
- Virtual file system commands such as `mkdir`, `touch`, `cat`, `rm`, and `mv`
- Inter-process communication through messages and channels
- GUI mode for interactive exploration
- CLI mode for command-driven testing and demos

## Scheduling Support

The simulator includes these scheduling algorithms:

- FCFS
- SJF
- SRTF
- Round Robin

## Memory Support

The simulator supports contiguous memory allocation with:

- First Fit
- Best Fit
- Worst Fit

## Project Structure

```text
mini-os-sim/
|-- mini-os-sim/
|   |-- src/main/java/   Java source files
|   |-- out/             Compiled class files
|   `-- sources.txt      Source file list
`-- README.md
```

## Requirements

- Java 17 or later recommended
- Windows, macOS, or Linux with `javac` and `java` available in the terminal

## Run the Project

This project does not currently include a Maven or Gradle build file, so you can compile and run it directly with `javac` and `java`.

1. Open a terminal in:

```text
C:\Users\ashas\OneDrive\Desktop\mini-os-sim\mini-os-sim
```

2. Compile the source files:

```bash
javac -d out src/main/java/Main.java src/main/java/os/cli/*.java src/main/java/os/filesystem/*.java src/main/java/os/gui/*.java src/main/java/os/ipc/*.java src/main/java/os/memory/*.java src/main/java/os/process/*.java src/main/java/os/scheduler/*.java src/main/java/os/utils/*.java
```

3. Run the GUI version:

```bash
java -cp out Main
```

4. Run the CLI version:

```bash
java -cp out Main --cli
```

## Example CLI Commands

```text
help
status
demo
sched set rr 4
sched run
mem init 1024 firstfit
proc add 1 0 5 2
ipc send 1 2 hello
```

## Learning Areas Covered

This simulator is useful for exploring:

- Process scheduling
- Memory management
- File system operations
- IPC mechanisms
- GUI-based systems visualization in Java Swing

## Repository

GitHub repository:

[https://github.com/asmita-05/MINI-OS-SIMULATOR](https://github.com/asmita-05/MINI-OS-SIMULATOR)
