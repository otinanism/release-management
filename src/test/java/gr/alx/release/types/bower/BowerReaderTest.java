package gr.alx.release.types.bower;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.alx.release.configuration.Configurator;
import gr.alx.release.types.FileReader;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by alx on 10/9/2016.
 */
public class BowerReaderTest {

    private BowerReader cut;
    private FileReader gr;

    @Before
    public void setUp() throws IOException {
        cut = new BowerReader(new ObjectMapper());
        gr = new FileReader(new Configurator().getConfiguration("configuration.yml"));
    }

    @Test
    public void shouldGetAllPaths() throws IOException {
        List<Path> packagePaths = cut.getAllPaths(gr.getAllPaths());

        assertThat(packagePaths.size()).isEqualTo(3);
    }

    @Test
    public void shouldReadPackageFile() throws IOException {

        BowerFileRepresentation packageJson = cut.readFile(cut.getAllPaths(gr.getAllPaths()).get(0));

        assertThat(packageJson.getVersion()).isEqualTo("0.0.2");
    }
}
