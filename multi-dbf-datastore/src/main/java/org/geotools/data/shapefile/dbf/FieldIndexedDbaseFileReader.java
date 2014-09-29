// NOTE:  must remain in org.geotools.data.shapefile.dbf due to access of
// access of package protected variables in super class 
package org.geotools.data.shapefile.dbf;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.resources.NIOUtilities;

public class FieldIndexedDbaseFileReader extends DbaseFileReader {
        
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(FieldIndexedDbaseFileReader.class);
    
    public FieldIndexedDbaseFileReader(FileChannel fileChannel) throws IOException {
        super(fileChannel, true, ShapefileDataStore.DEFAULT_STRING_CHARSET);
    }
    
    public FieldIndexedDbaseFileReader(FileChannel fileChannel, boolean useMemoryMappedBuffer) throws IOException {
        super(fileChannel, useMemoryMappedBuffer, ShapefileDataStore.DEFAULT_STRING_CHARSET, TimeZone.getDefault());
    }
    
    public FieldIndexedDbaseFileReader(FileChannel fileChannel, boolean useMemoryMappedBuffer, Charset stringCharset)
            throws IOException {
        super(fileChannel, useMemoryMappedBuffer, stringCharset, TimeZone.getDefault());
    }

    public FieldIndexedDbaseFileReader(FileChannel fileChannel, boolean useMemoryMappedBuffer, Charset stringCharset, TimeZone timeZone)
            throws IOException {
        super(fileChannel, useMemoryMappedBuffer, stringCharset, timeZone);
    }
	
	/**
	 * Jump to the correct record based on a record number, which is ONE based.
	 * 
	 * Everman:  In Tom K.'s original code, I think the intention was that this
	 * be 0 based, however, it was inconsistent in that usage, which resulted
	 * in bad and skipped values for the first and last records.
	 * 
	 * To ensure a complete break from the old and inconsistent code, I'm changing
	 * the method name from (the zero implying) setCurrentRecordByIndex to
	 * setCurrentRecordByNumber.
	 * 
	 * According to Tom's notes, he copied this from IndexedDBaseFileReader.goTo(...)
	 * however, it doesn't look much like that code.
	 * 
	 * 
	 * 
	 * @param recordNumber One based record number.
	 * @throws IOException
	 * @throws UnsupportedOperationException 
	 */
    public void setCurrentRecordByNumber(int recordNumber) throws IOException, UnsupportedOperationException {
        if (recordNumber > header.getNumRecords()) {
            throw new IllegalArgumentException("The recordNumber was greater than the recordCount");
        } else if (recordNumber < 1) {
			throw new IllegalArgumentException("The recordNumber is ONE based, but a call was made with a smaller value");
		}
        
        long newPosition = this.header.getHeaderLength()
                + this.header.getRecordLength() * (long) (recordNumber - 1);

        if (this.useMemoryMappedBuffer) {
            if(newPosition < this.currentOffset || (this.currentOffset + buffer.limit()) < (newPosition + header.getRecordLength())) {
                NIOUtilities.clean(buffer);
                FileChannel fc = (FileChannel) channel;
                if(fc.size() > newPosition + Integer.MAX_VALUE) {
                    currentOffset = newPosition;
                } else {
                    currentOffset = fc.size() - Integer.MAX_VALUE;
                }
                buffer = fc.map(FileChannel.MapMode.READ_ONLY, currentOffset, Integer.MAX_VALUE);
                buffer.position((int) (newPosition - currentOffset));
            } else {
                buffer.position((int) (newPosition - currentOffset));
            }
        } else {
            if (this.currentOffset <= newPosition
                    && this.currentOffset + buffer.limit() >= newPosition) {
                buffer.position((int) (newPosition - this.currentOffset));
                //System.out.println("Hit");
            } else {
                //System.out.println("Jump");
                FileChannel fc = (FileChannel) this.channel;
                fc.position(newPosition);
                this.currentOffset = newPosition;
                buffer.limit(buffer.capacity());
                buffer.position(0);
                fill(buffer, fc);
                buffer.position(0);
            }
        }
    }
    
    public boolean setCurrentRecordByValue(Object value) throws IOException {
        if (indexMap == null || indexMap.isEmpty()) {
            throw new IllegalArgumentException("index not created");
        }
        if (!indexMap.containsKey(value)) {
            return false;
        }
        setCurrentRecordByNumber(indexMap.get(value));
        return true;
    }
    
    Map<Object, Integer> indexMap = new HashMap<Object, Integer>();
    public void buildFieldIndex(int fieldIndex) throws IOException {
        int fieldCount = header.getNumFields();
        if (!(fieldIndex < fieldCount)) {
            throw new IllegalArgumentException("fieldIndex " + fieldIndex +  " >= " + fieldCount);
        }
        setCurrentRecordByNumber(1);
        indexMap.clear();
        Object[] values = new Object[0];
        for (int recordNumber = 1; hasNext(); ++recordNumber) {
            read(); // required when using readField
            Object value = readField(fieldIndex);
            if (indexMap.put(value, recordNumber) != null) {
                //throw new IllegalStateException("Record values at for field " + header.getFieldName(fieldIndex) + " are not unique, " + value + " already indexed.");
                /*  TODO:  don't want to be this lenient, there should be some checking
                 *  if the join column values in the source DBF to guarantee we
                 *  aren't attempt to join on an value that's non-unique...
                 */
//                LOGGER.log(
//                        Level.WARNING,
//                        "Record values for field {0} are not unique, {1} already indexed.  Will use last record encountered",
//                        new Object[] {
//                            header.getFieldName(fieldIndex),
//                            value
//                        });
            }
        }
    }
    
    public void buildFieldIndex(String fieldName) throws IOException {
        for (int fieldIndex = 0, fieldCount = header.getNumFields(); fieldIndex < fieldCount; ++fieldIndex) {
            if (header.getFieldName(fieldIndex).equalsIgnoreCase(fieldName)) {
                buildFieldIndex(fieldIndex);
                return;
            }
        }
        throw new IllegalArgumentException("field " + fieldName + " not found in dbf");
    }
    
    public Map<Object, Integer> getFieldIndex() {
        return indexMap;
    }
    
    public void setFieldIndex(Map<Object, Integer> index) {
        if (index == null || index.isEmpty()) {
            throw new IllegalArgumentException("index is null or empty");
        }
//        if (index.size() != header.getNumRecords()) {
//            throw new IllegalArgumentException("index size greater than record count");
//        }
        // TODO:  don't want to be this lenient...  see notes in buildFileIndes(int)
        if (index.size() > header.getNumRecords()) {
            throw new IllegalArgumentException("index size greater than record count");
        } else if (index.size() < header.getNumRecords()) {
            LOGGER.log(
                Level.WARNING,
                "index count <  record count.  Most likely due to index creation on field w/ non-unique values.");
        }
        this.indexMap = index;
    }
}
