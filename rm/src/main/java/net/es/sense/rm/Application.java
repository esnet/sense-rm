package net.es.sense.rm;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.ExecutionException;

@Slf4j
@SpringBootApplication(scanBasePackages={"net.es.sense.rm"})
//@EnableSwagger2
@EnableTransactionManagement
public class Application {
  // Command line parameters.
  public static final String ARGNAME_PIDFILE = "pidFile";
  public static final String SYSTEM_PROPERTY_PIDFILE = "pidFile";

  // Keep running while true.
  private static boolean keepRunning = true;

  /**
   * This is the Springboot main for this application.
   *
   * @param args
   * @throws java.util.concurrent.ExecutionException
   * @throws java.lang.InterruptedException
   */
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    log.info("[SENSE-N-RM] Starting...");

    // Load the command line options into appropriate system properties.
    try {
      processOptions(args);
    } catch (ParseException | IOException ex) {
      System.err.println("Failed to load command line options: " + ex.getMessage());
      System.exit(1);
      return;
    }

    ApplicationContext context = SpringApplication.run(Application.class, args);

    // Dump some runtime information.
    RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    log.info("Name: {}, {}", context.getApplicationName(), mxBean.getName());
    log.info("Pid: {}", ProcessHandle.current().pid());
    log.info("Uptime: {} ms", mxBean.getUptime());
    log.info("BootClasspath: {}", mxBean.getBootClassPath());
    log.info("Classpath: {}", mxBean.getClassPath());
    log.info("Library Path: {}", mxBean.getLibraryPath());
    for (String argument : mxBean.getInputArguments()) {
      log.info("Input Argument: {}", argument);
    }

    // Listen for a shutdown event so we can clean up.
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        log.info("[SENSE-N-RM] Shutting down RM...");
        Application.setKeepRunning(false);
      }
    });

    // Loop until we are told to shutdown.
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    while (keepRunning) {
      log.info("[SENSE-N-RM] {}", memStats(memoryBean));
      Thread.sleep(10000);
    }

    log.info("[SENSE-N-RM] Shutdown complete with uptime: {} ms", mxBean.getUptime());
    System.exit(0);
  }

  private static final int MEGABYTE = (1024 * 1024);

  private static String memStats(MemoryMXBean memoryBean) {
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    long maxMemory = heapUsage.getMax() / MEGABYTE;
    long usedMemory = heapUsage.getUsed() / MEGABYTE;
    return "Memory use :" + usedMemory + "M/" + maxMemory + "M";
  }

  /**
   * Returns a boolean indicating whether the PCE should continue running (true) or should terminate (false).
   *
   * @return true if the PCE should be running, false otherwise.
   */
  public static boolean isKeepRunning() {
    return keepRunning;
  }

  /**
   * Set whether the PCE should be running (true) or terminated (false).
   *
   * @param keepRunning true if the PCE should be running, false otherwise.
   */
  public static void setKeepRunning(boolean keepRunning) {
    Application.keepRunning = keepRunning;
  }

  /**
   * Process any command line arguments and set up associated system properties for runtime components.
   *
   * @param args
   * @throws ParseException
   * @throws IOException
   */
  private static void processOptions(String[] args) throws ParseException, IOException {
    // Parse the command line options.
    CommandLineParser parser = new DefaultParser();

    Options options = getOptions();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Error: You did not provide the correct arguments, see usage below.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar sense-n-rm.jar [-pidFile <filename>]", options);
      throw e;
    }

    // Write the process id out to file if specified.
    processPidFile(cmd);
  }

  /**
   * Build supported command line options for parsing of parameter input.
   *
   * @return List of supported command line options.
   */
  private static Options getOptions() {
    // Create Options object to hold our command line options.
    Options options = new Options();
    Option pidFileOption = new Option(ARGNAME_PIDFILE, true, "The file in which to write the process pid");
    pidFileOption.setRequired(false);
    options.addOption(pidFileOption);
    return options;
  }

  /**
   * Processes the "pidFile" command line and system property option.
   *
   * @param cmd Commands entered by the user.
   * @throws IOException If there is an issue creating the PID file.
   */
  private static void processPidFile(CommandLine cmd) throws IOException {
    // Get the application base directory.
    String pidFile = cmd.getOptionValue(ARGNAME_PIDFILE, System.getProperty(SYSTEM_PROPERTY_PIDFILE));
    pidFile = cmd.getOptionValue(ARGNAME_PIDFILE, pidFile);
    long pid = ProcessHandle.current().pid();
    if (pidFile == null || pidFile.isEmpty() || pid == -1) {
      return;
    }

    BufferedWriter out = null;
    try {
      FileWriter fstream = new FileWriter(pidFile, false);
      out = new BufferedWriter(fstream);
      out.write(String.valueOf(pid));
    } catch (IOException e) {
      System.err.printf("Error: %s\n", e.getMessage());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
