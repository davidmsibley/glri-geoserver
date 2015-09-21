package gov.usgs.cida.geotools.datastore;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStore;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author isuftin
 */
public class DbaseShapefileDataStoreFactoryTest {

	public DbaseShapefileDataStoreFactoryTest() {
	}

	File tempDir = null;
	DbaseShapefileDataStoreFactory factory = null;

	@Before
	public void doSetup() throws Exception {
		// Copy shapefile directory to a temp location
		URL shapeDirUrl = this.getClass().getClassLoader().getResource("shapefiles");
		File shapeDirFileObj = new File(shapeDirUrl.getFile());
		tempDir = Files.createTempDirectory(null).toFile();
		FileUtils.copyDirectoryToDirectory(shapeDirFileObj, tempDir);

		// Create the factory
		factory = new DbaseShapefileDataStoreFactory();
	}

	@Test
	public void testFactoryGetsCreated() throws Exception {
		assertThat("Factory was null", factory, is(notNullValue()));
	}

	@Test
	public void testFactoryGetsProperDisplayName() throws Exception {
		assertThat("Factory display name was incorrect", factory.getDisplayName().toLowerCase().contains("joining"), is(true));
	}

	@Test
	public void testFactoryGetsProperDescription() throws Exception {
		assertThat("Factory description was incorrect", factory.getDisplayName().toLowerCase().contains("joining"), is(true));
	}

	@Test
	public void testFactoryIsAvailableIsAlwaysTrue() throws Exception {
		assertThat("Factory availability was not true", factory.isAvailable(), is(true));
	}

	@Test
	public void testFactoryParameters() throws Exception {
		DataAccessFactory.Param[] resultParams = factory.getParametersInfo();
		assertThat("Params length was incorrect", resultParams.length, is(equalTo(4)));
		long namespaceCount = Arrays.stream(resultParams)
				.filter(p -> p.key.equals("namespace"))
				.count();
		assertThat("Default params did not have a namespace param", namespaceCount, is(equalTo(1l)));

		long dbaseFileCount = Arrays.stream(resultParams)
				.filter(p -> p.key.equals("dbase_file"))
				.count();
		assertThat("Default params did not have a dbase_file param", dbaseFileCount, is(equalTo(1l)));

		long shapefileCount = Arrays.stream(resultParams)
				.filter(p -> p.key.equals("shapefile"))
				.count();
		assertThat("Default params did not have a shapefile param", shapefileCount, is(equalTo(1l)));

		long dbaseFieldCount = Arrays.stream(resultParams)
				.filter(p -> p.key.equals("dbase_field"))
				.count();
		assertThat("Default params did not have a dbaseField param", dbaseFieldCount, is(equalTo(1l)));
	}

	@Test
	public void testFactoryCanProcess() throws Exception {
		Map<String, Serializable> params = new HashMap<>();
		params.put("namespace", new URI("http://cida.usgs.gov/test"));
		params.put("dbase_file", new URL("file://" + new File(tempDir, String.format("%sshapefiles%sstates%sstates.dbf", File.separatorChar, File.separatorChar, File.separatorChar)).getPath()));
		params.put("shapefile", new URL("file://" + new File(tempDir, String.format("%sshapefiles%sstates_basic%sstates.shp", File.separatorChar, File.separatorChar, File.separatorChar)).getPath()));
		params.put("dbase_field", "STATE_ABBR");
		boolean canProcess = factory.canProcess(params);
		assertThat("Factory can not process", canProcess);
	}

	@Test
	public void testFactoryCanCreateDataStore() throws Exception {
		Map<String, Serializable> params = new HashMap<>();
		params.put("namespace", new URI("http://cida.usgs.gov/test"));
		params.put("dbase_file", new URL("file://" + new File(tempDir, String.format("%sshapefiles%sstates%sstates.dbf", File.separatorChar, File.separatorChar, File.separatorChar)).getPath()));
		params.put("shapefile", new URL("file://" + new File(tempDir, String.format("%sshapefiles%sstates_basic%sstates.shp", File.separatorChar, File.separatorChar, File.separatorChar)).getPath()));
		params.put("dbase_field", "STATE_ABBR");
		DataStore ds = null;
		try {
			ds = factory.createDataStore(params);
			assertThat("DataStore is null", ds, is(notNullValue()));
		} finally {
			if (ds != null) {
				ds.dispose();
			}
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testFactoryCannotCreateNewDataStore() throws Exception {
		factory.createNewDataStore(null);
		assertThat("Factory availability was not true", factory.isAvailable(), is(true));
	}

	@After
	public void doTeardown() throws Exception {

		try {
			FileUtils.forceDelete(tempDir);
		} catch (IOException ioe) {
			// No worries
		}
	}

}
