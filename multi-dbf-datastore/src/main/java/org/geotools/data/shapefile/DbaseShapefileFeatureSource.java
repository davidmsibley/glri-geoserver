package org.geotools.data.shapefile;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.geotools.data.EmptyFeatureReader;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.ReTypeFeatureReader;
import static org.geotools.data.shapefile.DbaseShapefileDataStore.KEY_FIELD_INDEX;
import static org.geotools.data.shapefile.ShapefileFeatureSource.LOGGER;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.geotools.data.shapefile.fid.IndexedFidReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.index.CloseableIterator;
import org.geotools.data.shapefile.index.Data;
import org.geotools.data.shapefile.index.TreeException;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.store.ContentEntry;
import org.geotools.factory.Hints;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureTypes;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.ScreenMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;

/**
 *
 * @author dmsibley
 */
public class DbaseShapefileFeatureSource extends ShapefileFeatureSource {

	private Set<String> shapefileAttributeNames;
	private Set<String> joinedDBaseAttributeNames;
//	private Map<Object, Integer> fieldIndexMap;
	
	private FieldIndexedDbaseFileReader dbReader;
	
	public DbaseShapefileFeatureSource(FieldIndexedDbaseFileReader dbReader, ContentEntry entry, ShpFiles shpFiles) {
		super(entry, shpFiles);
		
		this.dbReader = dbReader;
	}


	
	@Override
	protected List<AttributeDescriptor> readAttributes() throws IOException {
		List<AttributeDescriptor> shapefileAttributeDescriptors = super.readAttributes();

		shapefileAttributeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (AttributeDescriptor attributeDescriptor : shapefileAttributeDescriptors) {
			shapefileAttributeNames.add(attributeDescriptor.getLocalName());
		}

		List<AttributeDescriptor> dbaseFileAttributeDescriptors;

//		FieldIndexedDbaseFileReader dbaseReader = null;
//		try {
//			dbaseReader = createDbaseReader();

			DbaseFileHeader dbaseFileHeader = dbReader.getHeader();
			int dbaseFieldCount = dbaseFileHeader.getNumFields();
			dbaseFileAttributeDescriptors = new ArrayList<>(dbaseFieldCount - 1);

			AttributeTypeBuilder atBuilder = new AttributeTypeBuilder();

			for (int dbaseFieldIndex = 0; dbaseFieldIndex < dbaseFieldCount; ++dbaseFieldIndex) {
				String dbaseFieldName = dbaseFileHeader.getFieldName(dbaseFieldIndex);
				if (!shapefileAttributeNames.contains(dbaseFieldName)) {
					dbaseFileAttributeDescriptors.add(atBuilder.
							userData(KEY_FIELD_INDEX, dbaseFieldIndex).
							binding(dbaseFileHeader.getFieldClass(dbaseFieldIndex)).
							buildDescriptor(dbaseFileHeader.getFieldName(dbaseFieldIndex)));
				}
			}
//		} finally {
//			if (dbaseReader != null) {
//				try {
//					dbaseReader.close();
//				} catch (IOException ignore) {
//				}
//			}
//		}

		joinedDBaseAttributeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (AttributeDescriptor attributeDescriptor : dbaseFileAttributeDescriptors) {
			joinedDBaseAttributeNames.add(attributeDescriptor.getLocalName());
		}

		List<AttributeDescriptor> attributeDescriptors = new ArrayList<>(
				shapefileAttributeDescriptors.size()
				+ dbaseFileAttributeDescriptors.size());
		attributeDescriptors.addAll(shapefileAttributeDescriptors);
		attributeDescriptors.addAll(dbaseFileAttributeDescriptors);

		return attributeDescriptors;
	}

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query q)
            throws IOException {
        SimpleFeatureType resultSchema = getResultSchema(q);
        SimpleFeatureType readSchema = getReadSchema(q);
        GeometryFactory geometryFactory = getGeometryFactory(q);

        // grab the target bbox, if any
        Envelope bbox = new ReferencedEnvelope();
        if (q.getFilter() != null) {
            bbox = (Envelope) q.getFilter().accept(ExtractBoundsFilterVisitor.BOUNDS_VISITOR, bbox);
        }

        // see if we can use indexing to speedup the data access
        Filter filter = q != null ? q.getFilter() : null;
        IndexManager indexManager = getDataStore().indexManager;
        CloseableIterator<Data> goodRecs = null;
        if (getDataStore().isFidIndexed() && filter instanceof Id && indexManager.hasFidIndex(false)) {
            Id fidFilter = (Id) filter;
            List<Data> records = indexManager.queryFidIndex(fidFilter);
            if (records != null) {
                goodRecs = new CloseableIteratorWrapper<Data>(records.iterator());
            }
        } else if (getDataStore().isIndexed() && !bbox.isNull()
                && !Double.isInfinite(bbox.getWidth()) && !Double.isInfinite(bbox.getHeight())) {
            try {
                if(indexManager.isSpatialIndexAvailable() || getDataStore().isIndexCreationEnabled()) {
                    goodRecs = indexManager.querySpatialIndex(bbox);
                }
            } catch (TreeException e) {
                throw new IOException("Error querying index: " + e.getMessage());
            }
        }
        // do we have anything to read at all? If not don't bother opening all the files
        if (goodRecs != null && !goodRecs.hasNext()) {
            LOGGER.log(Level.FINE, "Empty results for " + resultSchema.getName().getLocalPart()
                    + ", skipping read");
            goodRecs.close();
            return new EmptyFeatureReader<SimpleFeatureType, SimpleFeature>(resultSchema);
        }
        
        // get the .fix file reader, if we have a .fix file
        IndexedFidReader fidReader = null;
        if (getDataStore().isFidIndexed() && filter instanceof Id && indexManager.hasFidIndex(false)) {
            fidReader = new IndexedFidReader(shpFiles);
        }

        // setup the feature readers
        ShapefileSetManager shpManager = getDataStore().shpManager;
        ShapefileReader shapeReader = shpManager.openShapeReader(geometryFactory, goodRecs != null);
        DbaseFileReader dbfReader = null;
        List<AttributeDescriptor> attributes = readSchema.getAttributeDescriptors();
        if (attributes.size() < 1
                || (attributes.size() == 1 && readSchema.getGeometryDescriptor() != null)) {
            LOGGER.fine("The DBF file won't be opened since no attributes will be read from it");
        } else {
            dbfReader = dbReader;
        }
        ShapefileFeatureReader reader;
        if (goodRecs != null) {
            reader = new IndexedShapefileFeatureReader(readSchema, shapeReader, dbfReader, fidReader, 
                    goodRecs);
        } else {
            reader = new ShapefileFeatureReader(readSchema, shapeReader, dbfReader, fidReader);
        }
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            reader.setFilter(filter);
        }

        // setup the target bbox if any, and the generalization hints if available
        if (q != null) {
            if (bbox != null && !bbox.isNull()) {
                reader.setTargetBBox(bbox);
            }

            Hints hints = q.getHints();
            if (hints != null) {
                Number simplificationDistance = (Number) hints.get(Hints.GEOMETRY_DISTANCE);
                if (simplificationDistance != null) {
                    reader.setSimplificationDistance(simplificationDistance.doubleValue());
                }
                reader.setScreenMap((ScreenMap) hints.get(Hints.SCREENMAP));

                if (Boolean.TRUE.equals(hints.get(Hints.FEATURE_2D))) {
                    shapeReader.setFlatGeometry(true);
                }
            }

        }

        // do the retyping
        if(!FeatureTypes.equals(readSchema, resultSchema)) {
           return new ReTypeFeatureReader(reader, resultSchema);
        } else {
            return reader;
        }
    }

}
