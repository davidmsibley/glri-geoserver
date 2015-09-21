package org.geotools.data.shapefile;

import java.io.IOException;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.geotools.data.shapefile.files.ShpFileType;
import static org.geotools.data.shapefile.files.ShpFileType.DBF;
import org.geotools.data.shapefile.files.ShpFiles;

/**
 *
 * @author dmsibley
 */
public class DbaseShapefileSetManager extends ShapefileSetManager {

	public DbaseShapefileSetManager(ShpFiles shpFiles, ShapefileDataStore store) {
		super(shpFiles, store);
	}

//	@Override
//	protected DbaseFileReader openDbfReader(boolean indexed) throws IOException {
//		if (shpFiles.get(ShpFileType.DBF) == null) {
//            return null;
//        }
//
//        if (shpFiles.isLocal() && !shpFiles.exists(DBF)) {
//            return null;
//        }
//
//        try {
//            if (indexed) {
//                return new FieldIndexedDbaseFileReader(shpFiles, store.isMemoryMapped(),
//                        store.getCharset(), store.getTimeZone());
//            } else {
//                return new DbaseFileReader(shpFiles, store.isMemoryMapped(), store.getCharset(),
//                        store.getTimeZone());
//            }
//        } catch (IOException e) {
//            // could happen if dbf file does not exist
//            return null;
//        }
//	}
	
	
}
