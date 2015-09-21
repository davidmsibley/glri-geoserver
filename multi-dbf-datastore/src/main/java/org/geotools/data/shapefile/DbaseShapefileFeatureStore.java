package org.geotools.data.shapefile;

import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.store.ContentEntry;

/**
 *
 * @author dmsibley
 */
public class DbaseShapefileFeatureStore extends ShapefileFeatureStore {

	public DbaseShapefileFeatureStore(ContentEntry entry, ShpFiles files) {
		super(entry, files);
		this.delegate = new DbaseShapefileFeatureSource(entry, files);
        this.hints = delegate.getSupportedHints();
	}

}
