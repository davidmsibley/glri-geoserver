package org.geotools.data.shapefile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import static org.geotools.data.shapefile.DbaseShapefileDataStore.KEY_FIELD_INDEX;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.store.ContentEntry;
import org.geotools.feature.AttributeTypeBuilder;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author dmsibley
 */
public class DbaseShapefileFeatureSource extends ShapefileFeatureSource {

	public DbaseShapefileFeatureSource(ContentEntry entry, ShpFiles shpFiles) {
		super(entry, shpFiles);
	}

//	@Override
//	protected List<AttributeDescriptor> readAttributes() throws IOException {
//		List<AttributeDescriptor> shapefileAttributeDescriptors = super.getSchema().getAttributeDescriptors();
//
//		shapefileAttributeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
//		for (AttributeDescriptor attributeDescriptor : shapefileAttributeDescriptors) {
//			shapefileAttributeNames.add(attributeDescriptor.getLocalName());
//		}
//
//		List<AttributeDescriptor> dbaseFileAttributeDescriptors;
//
//		FieldIndexedDbaseFileReader dbaseReader = null;
//		try {
//			dbaseReader = createDbaseReader();
//
//			DbaseFileHeader dbaseFileHeader = dbaseReader.getHeader();
//			int dbaseFieldCount = dbaseFileHeader.getNumFields();
//			dbaseFileAttributeDescriptors = new ArrayList<>(dbaseFieldCount - 1);
//
//			AttributeTypeBuilder atBuilder = new AttributeTypeBuilder();
//
//			for (int dbaseFieldIndex = 0; dbaseFieldIndex < dbaseFieldCount; ++dbaseFieldIndex) {
//				String dbaseFieldName = dbaseFileHeader.getFieldName(dbaseFieldIndex);
//				if (!shapefileAttributeNames.contains(dbaseFieldName)) {
//					dbaseFileAttributeDescriptors.add(atBuilder.
//							userData(KEY_FIELD_INDEX, dbaseFieldIndex).
//							binding(dbaseFileHeader.getFieldClass(dbaseFieldIndex)).
//							buildDescriptor(dbaseFileHeader.getFieldName(dbaseFieldIndex)));
//				}
//			}
//		} finally {
//			if (dbaseReader != null) {
//				try {
//					dbaseReader.close();
//				} catch (IOException ignore) {
//				}
//			}
//		}
//
//		joinedDBaseAttributeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
//		for (AttributeDescriptor attributeDescriptor : dbaseFileAttributeDescriptors) {
//			joinedDBaseAttributeNames.add(attributeDescriptor.getLocalName());
//		}
//
//		List<AttributeDescriptor> attributeDescriptors = new ArrayList<>(
//				shapefileAttributeDescriptors.size()
//				+ dbaseFileAttributeDescriptors.size());
//		attributeDescriptors.addAll(shapefileAttributeDescriptors);
//		attributeDescriptors.addAll(dbaseFileAttributeDescriptors);
//
//		return attributeDescriptors;
//	}

}
