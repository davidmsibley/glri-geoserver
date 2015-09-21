package org.geotools.data.shapefile;

import gov.usgs.cida.geotools.datastore.QueryUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultFeatureReader;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.DbaseShapefileFeatureSource;
import org.geotools.data.shapefile.DbaseShapefileFeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

/**
 *
 * @author tkunicki
 */
public class DbaseShapefileDataStore extends ShapefileDataStore {

	public final static String KEY_FIELD_INDEX = "dbaseFieldIndex";

	private final URL dbaseFileURL;
	private final String shapefileJoinAttributeName;
//
//	private Set<String> shapefileAttributeNames;
//	private Set<String> joinedDBaseAttributeNames;
//	private Map<Object, Integer> fieldIndexMap;

	public DbaseShapefileDataStore(URI namespaceURI, URL dbaseFileURL, URL shapefileURL, String shapefileJoinAttributeName) throws MalformedURLException, IOException {

		super(shapefileURL);
		this.setNamespaceURI(namespaceURI.toString());
		this.setMemoryMapped(true);
		this.setCharset(ShapefileDataStore.DEFAULT_STRING_CHARSET);
		this.setBufferCachingEnabled(true);

		this.dbaseFileURL = dbaseFileURL;

		this.shapefileJoinAttributeName = shapefileJoinAttributeName;

		// NOTE: if this method is removed from constructor it should be synchronized...
//		createDbaseReader();
	}
	
	@Override
	public ContentFeatureSource getFeatureSource() throws IOException {
		ContentEntry entry = ensureEntry(getTypeName());
		if (shpFiles.isWritable()) {
			return new DbaseShapefileFeatureStore(entry, shpFiles);
		} else {
			return new DbaseShapefileFeatureSource(entry, shpFiles);
		}
	}
	
	

    // NOTE:  not synchronized because this is called in constructor,
	// synchronization of initialization of fileIndexMap is the concern...
//	private FieldIndexedDbaseFileReader createDbaseReader() throws IOException {
//		File dBaseFile = new File(dbaseFileURL.getFile());
//		FileChannel dBaseFileChannel = (new FileInputStream(dBaseFile)).getChannel();
//		FieldIndexedDbaseFileReader dbaseReader = new FieldIndexedDbaseFileReader(dBaseFileChannel);
//		if (fieldIndexMap == null) {
//			dbaseReader.buildFieldIndex(shapefileJoinAttributeName);
//			fieldIndexMap = Collections.unmodifiableMap(dbaseReader.getFieldIndex());
//		} else {
//			dbaseReader.setFieldIndex(fieldIndexMap);
//		}
//		return dbaseReader;
//	}



	@Override
	public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(final Query query, final Transaction tx) throws IOException {
		Query cleanedQuery = new Query(query);
//		if (requiresShapefileAttributes(cleanedQuery)) {
//			if (requiresJoinedDbaseAttributes(cleanedQuery)) {
				// make sure join attribute is in property list if we need to join!
		List<String> props = Arrays.asList(cleanedQuery.getPropertyNames());
		boolean hasJoinAttr = props.stream()
				.filter(el -> {
					return shapefileJoinAttributeName.equalsIgnoreCase(el);
				})
				.findAny().isPresent();
		if (!hasJoinAttr) {
			List<String> modProps = new ArrayList<String>(props);
			modProps.add(shapefileJoinAttributeName);
			cleanedQuery.setPropertyNames(modProps);
		}
//		} else {
//			try {
//				List<String> propertyNames = Arrays.asList(query.getPropertyNames());
//				SimpleFeatureType subTypeSchema = DataUtilities.createSubType(getSchema(), propertyNames.toArray(new String[0]));
//				return new DefaultFeatureReader(new DbaseAttributeReader(createDbaseReader(), subTypeSchema), subTypeSchema);
//			} catch (SchemaException ex) {
//				// hack
//				throw new IOException(ex);
//			}
//		}
		return super.getFeatureReader(cleanedQuery, tx);
	}
//
//    @Override
//    protected ShapefileAttributeReader getAttributesReader(boolean readDBF, Query query, String[] properties) throws IOException {
//        if (requiresJoinedDbaseAttributes(query)) {
//            int shapefileJoinAttributeIndex = indexOfIgnoreCase(properties, shapefileJoinAttributeName);
//            return new DbaseShapefileAttributeJoiningReader(super.getAttributesReader(true, query, properties), createDbaseReader(), shapefileJoinAttributeIndex);
//        } else {
//            return super.getAttributesReader(readDBF, query, properties);
//        }
//    }

//	private boolean requiresShapefileAttributes(Query query) {
//		return QueryUtil.requiresAttributes(query, shapefileAttributeNames);
//	}
//
//	private boolean requiresJoinedDbaseAttributes(Query query) {
//		return QueryUtil.requiresAttributes(query, joinedDBaseAttributeNames);
//	}

//    @Override
//    protected String createFeatureTypeName() {
//        String path = dbaseFileURL.getPath();
//        File file = new File(path);
//        String name = file.getName();
//        int suffixIndex = name.lastIndexOf("dbf");
//        if (suffixIndex > -1) {
//            name = name.substring(0, suffixIndex -1);
//        }
//        name = name.replace(',', '_'); // Are there other characters?
//        return name;
//    }
//    @Override
//    public ReferencedEnvelope getBounds(Query query) throws IOException {
//        return super.getBounds(query);
//    }
	@Override
	public void dispose() {
		super.dispose();
	}
}
