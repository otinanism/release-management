package gr.alx.release;

import java.nio.file.Path;

/**
 * Created by TRIFYLLA on 7/10/2016.
 */
public interface Writer {

    String writeNewVersion(Path path, String oldVersion, FileRepresentation model);
}
