package gov.usgs.cida.geotools.datastore;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

/**
 *
 * @author isuftin
 */
public class DbaseShapefileDataStoreTest {

	public DbaseShapefileDataStoreTest() {
	}

	DbaseShapefileDataStore ds;
	File tempDir = null;

	@Before
	public void doSetup() throws Exception {
		// Copy shapefile directory to a temp location
		URL shapeDirUrl = this.getClass().getClassLoader().getResource("shapefiles");
		File shapeDirFileObj = new File(shapeDirUrl.getFile());
		tempDir = Files.createTempDirectory(null).toFile();
		FileUtils.copyDirectoryToDirectory(shapeDirFileObj, tempDir);

		// Create the factory
		DbaseShapefileDataStoreFactory factory = new DbaseShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<>();
		params.put("namespace", new URI("http://cida.usgs.gov/test"));
		params.put("dbase_file", new URL("file://" + new File(tempDir, String.format("%sshapefiles%sstates%sstates.dbf", File.separatorChar, File.separatorChar, File.separatorChar)).getPath()));
		params.put("shapefile", new URL("file://" + new File(tempDir, String.format("%sshapefiles%sstates_basic%sstates.shp", File.separatorChar, File.separatorChar, File.separatorChar)).getPath()));
		params.put("dbase_field", "STATE_ABBR");
		ds = (DbaseShapefileDataStore) factory.createDataStore(params);
	}

	@Test
	public void testGetAttributes() throws IOException {
		List<AttributeDescriptor> result = ds.readAttributes();
		assertThat("AttributeDescriptor was null", result, is(notNullValue()));
		assertThat("AttributeDescriptor was empty", result.size(), is(greaterThan(0)));
		assertThat("AttributeDescriptor was empty", result.size(), is(14));
	}

	@Test
	public void testGetFeatureReader() throws Exception {
		FeatureReader<SimpleFeatureType, SimpleFeature> result = null;
		List<AttributeDescriptor> attributes = ds.readAttributes();
		String[] properties = attributes.stream()
				.map(d -> {
					return d.getLocalName();
				})
				.toArray(size -> new String[size]);
		String typeName = ds.getSchema().getTypeName();
		Query query = new Query(typeName, Filter.INCLUDE, properties);
		try {
			result = ds.getFeatureReader(query, new DefaultTransaction());
			assertThat("Feature reader was null value", result, is(notNullValue()));
		} finally {
			if (result != null) {
				result.close();

			}
		}

	}

	@After
	public void doTeardown() throws Exception {
		if (ds != null) {
			ds.dispose();
		}
		try {
			FileUtils.forceDelete(tempDir);
		} catch (IOException ioe) {
			// No worries
		}
	}

}
