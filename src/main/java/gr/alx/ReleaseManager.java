package gr.alx;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alx on 10/2/2016.
 */
@Component
@Slf4j
public class ReleaseManager {

    public static final String RELEASE = "release";
    private static final List allowedActions = Arrays.asList(RELEASE, "bump");
    public static final String BUILD = "build";
    private static final List allowedBumpTypes = Arrays.asList("major", "minor", BUILD, "prod", "snapshot");
    private static final String INVALID_VERSION_FORMAT = "Invalid version format. The allowed format is of the form: " +
            "ddd.ddd.ddd.-SNAPSHOT";

    private ConsoleReader console;
    private PomReader pomReader;
    private PomWriter pomWriter;

    public ReleaseManager() {
        try {
            console = new ConsoleReader();
            pomReader = new PomReader();
            pomWriter = new PomWriter();
        } catch (IOException e) {
            log.error("An error occurred while initialising ConsoleReader.", e);
        }
    }

    public void run(String... args) {
        try {
            console.setPrompt("rls> ");
            String line;
            while ((line = console.readLine()) != null) {
                if ("quit".equalsIgnoreCase(line) || "exit".equalsIgnoreCase(line)) {
                    break;
                } else if (Arrays.asList(line.split(" ")).size() == 2) {
                    doRelease(line);
                } else {
                    console.println("Allowed actions are: " + allowedActions.toString());
                }
            }
        } catch (IOException e) {
            log.error("An error occurred while running the release process", e);
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
                log.error("An error occurred while finalising the release process.", e);
            }
        }
    }

    private void doRelease(String line) throws IOException {
        List<String> arguments = Arrays.asList(line.split(" "));
        String action = arguments.get(0);
        String version = arguments.get(1);

        if (!allowedActions.contains(action)) {
            console.println("Allowed actions are: " + allowedActions.toString());
        } else if ("bump".equalsIgnoreCase(action)) {
            doAutomaticVersion(version);
        } else if (RELEASE.equalsIgnoreCase(action)) {
            doManualVersion(version);
        }
    }

    void doAutomaticVersion(String type) throws IOException {
        if (!allowedBumpTypes.contains(type)) {
            console.println("Allowed types are: " + allowedBumpTypes.toString());
        } else {
            List<Path> pomPaths = pomReader.getAllPomPaths();
            pomPaths.forEach(path -> bumpVersionInPom(path, type));
        }
    }

    void bumpVersionInPom(Path path, String type) {
        Model model = pomReader.readPomFile(path);
        String oldVersion = model.getVersion();
        String newVersion = bumpUpVersion(oldVersion, type);
        model.setVersion(newVersion);
        String writeMessage = pomWriter.writeNewVersion(path, oldVersion, model);
        printInConsole(writeMessage);
    }

    void doManualVersion(String version) throws IOException {
        if (!validVersion(version)) {
            console.println(INVALID_VERSION_FORMAT);
        } else {
            List<Path> pomPaths = pomReader.getAllPomPaths();
            pomPaths.forEach(path -> updateVersionInPom(path, version));
        }
    }

    void updateVersionInPom(Path path, String newVersion) {
        Model model = pomReader.readPomFile(path);
        String oldVersion = model.getVersion();
        model.setVersion(newVersion);
        String writeMessage = pomWriter.writeNewVersion(path, oldVersion, model);
        printInConsole(writeMessage);
    }

    boolean validVersion(String version) {
        String validVersionRegEx = "\\d(\\d)?(\\d)?.\\d(\\d)?(\\d)?.\\d(\\d)?(\\d)?(-SNAPSHOT)?";
        return version.matches(validVersionRegEx);
    }

    Version splitVersion(String version) {
        List<String> versionParts = Arrays.asList(version.split("\\.|-"));
        if (versionParts.size() != 3 && versionParts.size() != 4) {
            throw new IllegalArgumentException("Version is not valid: " + version);
        }
        return new Version(
                Integer.parseInt(versionParts.get(0)),
                Integer.parseInt(versionParts.get(1)),
                Integer.parseInt(versionParts.get(2)),
                versionParts.size() == 4
        );
    }

    String bumpUpVersion(String pomVersion, String type) {
        Version version = splitVersion(pomVersion);
        switch (type) {
            case "major":
                version.setMajor(version.getMajor() + 1);
                break;
            case "minor":
                version.setMinor(version.getMinor() + 1);
                break;
            case BUILD:
                version.setBuild(version.getBuild() + 1);
                break;
            case "prod":
                version.setSnapshot(false);
                break;
            case "snapshot":
                version.setSnapshot(true);
                break;
            default:
                break;
        }
        return version.toString();
    }

    private void printInConsole(String writeMessage) {
        try {
            console.println(writeMessage);
        } catch (IOException e) {
            log.error("An error occurred while printing in the console", e);
        }
    }
}

