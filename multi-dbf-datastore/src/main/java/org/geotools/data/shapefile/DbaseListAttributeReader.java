package org.geotools.data.shapefile;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import org.geotools.data.AttributeReader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

/**
 *
 * @author tkunicki
 */
public class DbaseListAttributeReader implements AttributeReader {
    
    private final SimpleFeatureType featureType;
    private final List<FieldIndexedDbaseFileReader> dbaseReaderList;
    private final int attributeCount;
    
    private int[] dbaseReaderIndices;
    private int[] dbaseReaderFieldIndices;
    private FieldIndexedDbaseFileReader.Row[] dbaseReaderRows;
    
    private Set<Object> indexedValueSet;
    private Iterator<Object> indexedValueIterator;

    DbaseListAttributeReader(List<FieldIndexedDbaseFileReader> dbaseReaderList, SimpleFeatureType featureType) throws IOException {
        this.featureType = featureType;
        this.dbaseReaderList = dbaseReaderList;
        this.attributeCount = featureType.getAttributeCount();
        
        dbaseReaderRows = new FieldIndexedDbaseFileReader.Row[dbaseReaderList.size()];
        
        dbaseReaderIndices = new int[attributeCount];
        dbaseReaderFieldIndices = new int[attributeCount];
        for (int attributeIndex = 0; attributeIndex < attributeCount; ++attributeIndex) {
            AttributeDescriptor attributeDescriptor = featureType.getDescriptor(attributeIndex);
            Object dbaseReaderIndexObject = attributeDescriptor.getUserData().get(DbaseDirectoryShapefileDataStore.KEY_READER_INDEX);
            if (dbaseReaderIndexObject instanceof Integer) {
                dbaseReaderIndices[attributeIndex] = ((Integer)dbaseReaderIndexObject).intValue();
                Object dbaseReaderFieldIndexObject = attributeDescriptor.getUserData().get(DbaseDirectoryShapefileDataStore.KEY_FIELD_INDEX);
                if (dbaseReaderFieldIndexObject instanceof Integer) {
                    dbaseReaderFieldIndices[attributeIndex] = ((Integer)dbaseReaderFieldIndexObject).intValue();
                } else {
                    dbaseReaderFieldIndices[attributeIndex] = -1;
                }
            } else {
                dbaseReaderIndices[attributeIndex] = -1;
                dbaseReaderFieldIndices[attributeIndex] = -1;
            }
        }
        
        indexedValueSet = new TreeSet<Object>();
        for (FieldIndexedDbaseFileReader dbaseReader : dbaseReaderList) {
            indexedValueSet.addAll(dbaseReader.getFieldIndex().keySet());
        }
        indexedValueIterator = indexedValueSet.iterator();
    }

    @Override
    public int getAttributeCount() {
        return featureType.getAttributeCount();
    }

    @Override
    public AttributeDescriptor getAttributeType(int index) throws ArrayIndexOutOfBoundsException {
        return featureType.getDescriptor(index);
    }

    @Override
    public void close() throws IOException {
        for (FieldIndexedDbaseFileReader dbaseReader : dbaseReaderList) {
            if (dbaseReaderList != null) {
                try { dbaseReader.close(); } catch (IOException ignore) {}
            }
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        return indexedValueIterator.hasNext();
    }

    @Override
    public void next() throws IOException, IllegalArgumentException, NoSuchElementException {
        Object indexedValue = indexedValueIterator.next();
        for (int dbaseReaderIndex = 0, dbaseReaderCount = dbaseReaderList.size();
                dbaseReaderIndex < dbaseReaderCount;
                ++dbaseReaderIndex) {
            FieldIndexedDbaseFileReader dbaseReader = dbaseReaderList.get(dbaseReaderIndex);
            dbaseReaderRows[dbaseReaderIndex] = dbaseReader != null && dbaseReader.setCurrentRecordByValue(indexedValue) ?
                    dbaseReader.readRow() :
                    null;
            
        }
    }

    @Override
    public Object read(int index) throws IOException, ArrayIndexOutOfBoundsException {
        if (index < attributeCount) {
            FieldIndexedDbaseFileReader.Row dbaseReaderRow = dbaseReaderRows[dbaseReaderIndices[index]];
            if (dbaseReaderRow != null) {
                int dbaseReaderFieldIndex = dbaseReaderFieldIndices[index];
                if (dbaseReaderFieldIndex > -1) {
                    return dbaseReaderRow.read(dbaseReaderFieldIndex);
                }
            }
            return null;
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }
}
