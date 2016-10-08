package gr.alx.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.alx.release.packagejson.PackageReader;
import gr.alx.release.packagejson.PackageWriter;
import gr.alx.release.pom.PomReader;
import gr.alx.release.pom.PomWriter;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Central point of the application which manages the whole release process.
 * Created by alx on 10/2/2016.
 */
@Component
@Slf4j
public class ReleaseManager {

    private static final String RELEASE = "release";
    private static final List allowedActions = Arrays.asList(RELEASE, "bump");
    private static final String BUILD = "build";
    private static final List allowedBumpTypes = Arrays.asList("major", "minor", BUILD, "prod", "snapshot");
    private static final String INVALID_VERSION_FORMAT = "Invalid version format. The allowed format is of the form: " +
            "ddd.ddd.ddd.-SNAPSHOT";

    private ConsoleReader console;
    private final List<FileHandler> fileHandlers = new ArrayList<>();

    /**
     * Initialisation constructor which initialise all dependent classes
     */
    public ReleaseManager() {
        try {
            console = new ConsoleReader();
            fileHandlers.addAll(
                    Arrays.asList(
                            new FileHandler(new PomReader(), new PomWriter())
                            ,
                            new FileHandler(new PackageReader(new ObjectMapper()), new PackageWriter())
                    )
            );
        } catch (IOException e) {
            log.error("An error occurred while initialising ConsoleReader.", e);
        }
    }

    /**
     * This is the entry point method.
     *
     * @param args parameters (if any) passed by the user.
     */
    public void run(String... args) {
        try {
            console.setPrompt("$ ");
            String line;
            while ((line = console.readLine()) != null) {
                if ("quit".equalsIgnoreCase(line) || "exit".equalsIgnoreCase(line)) {
                    break;
                } else if (Arrays.asList(line.split(" ")).size() == 2) {
                    doRelease(line);
                } else {
                    printInConsole("Allowed actions are: " + allowedActions.toString());
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

    /**
     * Parse user arguments and perform manual or automatic release actions.
     *
     * @param command the command entered by the user
     */
    public void doRelease(String command) {
        List<String> arguments = Arrays.asList(command.split(" "));
        String action = arguments.get(0);
        String version = arguments.get(1);

        if (!allowedActions.contains(action)) {
            printInConsole("Allowed actions are: " + allowedActions.toString());
        } else if ("bump".equalsIgnoreCase(action)) {
            doAutomaticVersion(version);
        } else if (RELEASE.equalsIgnoreCase(action)) {
            fileHandlers.forEach(fileHandler -> doManualVersion(version, fileHandler));
        }
    }

    void doAutomaticVersion(String type) {
        if (!allowedBumpTypes.contains(type)) {
            printInConsole("Allowed types are: " + allowedBumpTypes.toString());
        } else {
            fileHandlers.forEach(handler -> {
                List<Path> paths = null;
                try {
                    paths = handler.getReader().getAllPaths();
                    String newVersion = generateNewVersionFromPath(paths.get(0), type, handler.getReader());
                    paths.forEach(path -> updateVersionInFile(path, newVersion, handler));
                } catch (IOException | XmlPullParserException e) {
                    String error = "An error occurred during version update";
                    printInConsole(error);
                    log.error(error, e);
                }
            });
        }
    }

    void doManualVersion(String version, FileHandler fileHandler) {
        if (!validVersion(version)) {
            printInConsole(INVALID_VERSION_FORMAT);
        } else {
            try {
                fileHandler.getReader().getAllPaths()
                        .forEach(path -> updateVersionInFile(path, version, fileHandler));
            } catch (IOException e) {
                log.error("An error occurred during version update", e);
            }
        }
    }

    void updateVersionInFile(Path path, String newVersion, FileHandler fileHandler) {
        FileRepresentation model = null;
        try {
            model = fileHandler.getReader().readFile(path);
        } catch (IOException | XmlPullParserException e) {
            String error = "An error occurred during reading the file: " + path;
            printInConsole(error);
            log.error(error, e);
        }
        if (model != null) {
            String oldVersion = model.getVersion();
            model.setVersion(newVersion);
            String writeMessage = fileHandler.getWriter().writeNewVersion(path, oldVersion, model);
            printInConsole(writeMessage);
        }
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

    private String generateNewVersionFromPath(Path path, String type, Reader reader) throws IOException, XmlPullParserException {
        FileRepresentation model = reader.readFile(path);
        return bumpUpVersion(model.getVersion(), type);
    }

    void printInConsole(String writeMessage) {
        try {
            console.println(writeMessage);
        } catch (Exception e) {
            log.error("An error occurred while printing in the console the message: " + writeMessage, e);
        }
    }
}

