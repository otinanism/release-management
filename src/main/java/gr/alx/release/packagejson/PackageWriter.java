package gr.alx.release.packagejson;

import gr.alx.release.FileRepresentation;
import gr.alx.release.JsonWriterHelper;
import gr.alx.release.Writer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by TRIFYLLA on 7/10/2016.
 */
@Slf4j
public class PackageWriter implements Writer {
    @Override
    public String writeNewVersion(Path path, String oldVersion, FileRepresentation model) throws IOException {
        List<String> newLines = new ArrayList<>();
        String versionWithoutSnapshot = JsonWriterHelper.stripSnapshot(model.getVersion());
        List<String> lines = Files.lines(path).collect(toList());
        boolean updated = false;
        for (String line : lines) {
            String updatedLine = line;
            if (!updated && line.contains("\"version\":")) {
                updatedLine = "  \"version\": \"" + versionWithoutSnapshot + "\",";
                updated = true;
            }
            newLines.add(updatedLine);
        }
        Files.write(path, newLines);
        return "Updating package.json version for artifact: " + model.getArtifactId()
                + " from: " + oldVersion
                + " to: " + versionWithoutSnapshot;
    }
}
