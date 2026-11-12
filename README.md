# test-output-buffer

JUnit Platform extension for CI: buffers everything a test writes to the console and **only prints it when the test
fails**. Output of passing tests is discarded.

- Covers SLF4J console logging (Logback, SLF4J Simple) and direct `System.out` / `System.err` calls.
- `System.out` / `System.err` lines are routed through SLF4J (logger = calling class, `INFO` / `ERROR`), so all
  output appears in the regular log format with correct timestamp, thread and MDC.
- Works for JUnit 5/6 (Jupiter) and JUnit 4 (Vintage), also mixed in one module, and with parallel execution.
- Output of a failing test also lands in that test's `<system-out>` in the Surefire XML report.
- Everything stays in memory; no files are written.

Requires JUnit Platform 1.8+ and Java 17+.

## Enabling it (parent POM, CI only)

This project is built and released **separately** from the monorepo. It must not be a module of the monorepo or
inherit from its parent: every module would inherit the dependency on it, including itself (reactor cycle).

The jar is added to Surefire's test classpath only, not as a project dependency, so it does not show up in the
dependency tree, enforcer rules or `dependency:analyze` (requires Surefire 3.2+):

```xml
<properties>
  <test-output-buffer.version>1.0.0</test-output-buffer.version>
</properties>

<profiles>
  <profile>
    <id>ci-quiet-tests</id>
    <activation><property><name>env.CI</name></property></activation>
    <build>
      <pluginManagement>
        <plugins>
          <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <additionalClasspathDependencies>
                <additionalClasspathDependency>
                  <groupId>com.example.testing</groupId>
                  <artifactId>test-output-buffer</artifactId>
                  <version>${test-output-buffer.version}</version>
                </additionalClasspathDependency>
              </additionalClasspathDependencies>
            </configuration>
          </plugin>
        </plugins>
      </pluginManagement>
    </build>
  </profile>
</profiles>
```

Leave `redirectTestOutputToFile` at its default (`false`).

## Configuration (system properties)

Pass these through Surefire `systemPropertyVariables`.

| Property | Default | |
|---|---|---|
| `testoutput.buffer.enabled` | `true` | Kill switch, e.g. when debugging a CI build |
| `testoutput.buffer.stdout.level` | `INFO` | Level for `System.out` lines |
| `testoutput.buffer.stderr.level` | `ERROR` | Level for `System.err` lines |
| `testoutput.buffer.route-to-slf4j` | `true` | `false` buffers `System.out`/`err` lines unchanged |
| `testoutput.buffer.logging-packages` | `ch.qos.logback.,org.slf4j.simple.,org.slf4j.impl.SimpleLogger,org.apache.logging.log4j.` | Writers whose console output is already formatted |
| `testoutput.buffer.max-bytes-per-test` | `8388608` | Oldest output is dropped beyond this, with a truncation marker |
| `testoutput.buffer.flush-on-aborted` | `false` | Also print output of aborted tests (failed assumptions) |

## How it works

- `System.out` / `System.err` are replaced when the test plan starts and restored when it finishes.
- Each write is classified with a `StackWalker`:
  - output from a logging framework is buffered unchanged;
  - other output is collected into lines and logged through SLF4J. The logging framework writes the line back to
    the console, where it is buffered like any other log output.
- Output is buffered per test or container:
  - when a test fails, the output of its containers (`@BeforeAll`, Spring context startup) is printed first,
    followed by the test's own output;
  - output from threads a test started (executors) is attributed to that test.
- On a failure, the output is printed just before Surefire records the result, so it ends up in that test's
  `<system-out>`. This uses a package-private JUnit callback (`CompositeTestExecutionListener.EagerTestExecutionListener`,
  unchanged from Platform 1.8 to 6.0) through `org.junit.platform.launcher.core.OutputBufferingEagerListener`. If that
  class cannot be loaded (for example tests running on the module path), a plain listener is used: the console output
  is unchanged, but in the XML report the output is attached to the next test.

## Limitations

- Only **console** output is buffered. File appenders are not affected.
- Logging configurations that hold on to the console stream bypass the buffer, for example
  `org.slf4j.simpleLogger.cacheOutputStream=true`, or a Log4j2 `ConsoleAppender` without `follow="true"`.
- Output from threads not started by a test (thread pools created earlier, embedded servers, async appenders) is
  attributed on a best-effort basis: to the only running test; with parallel tests, to the deepest container they
  share; otherwise it is printed at the end of the run only when something failed.
- If the logging level for a `System.out` line is disabled (for example root at `WARN`), the line is buffered
  unformatted instead of being dropped.
- Spring context startup is logged during the first test that uses the context. If that test passes and a later test
  fails, the startup log is not shown.
- Output after the test plan finished (JVM shutdown hooks, such as closing cached Spring contexts) is not buffered.

## Build

`mvn verify` runs the unit tests and the integration tests in `src/it`:

- JUnit 4 and 5 in one module, with Logback, including XML report attribution
- SLF4J Simple
- Parallel Jupiter execution
- Spring Boot 4 (JUnit 6)
