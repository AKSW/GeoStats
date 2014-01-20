package eu.geoknow.athenarc.triplegeo;

import java.io.IOException;

import org.aksw.disambiguation.UriResolver;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureImpl;

import com.hp.hpl.jena.rdf.model.Model;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import eu.geoknow.athenarc.triplegeo.shape.ShpFileLoader;
import eu.geoknow.athenarc.triplegeo.utils.UtilsLib;

public class FederalStateShpFileLoader extends DefaultShapeFileLoader {
	
	public FederalStateShpFileLoader(Model model) {
		
		super.featureAttribute = "ID_1";
		super.featureClass = "TYPE_1";
		super.featureName = "NAME_1";
		super.path = "/Users/gerb/Development/workspaces/data/geostats/geometries/germany/DEU_adm/";
		super.fileName = "DEU_adm1";
		super.model = model;
		super.areaType = "state";
		super.getShapeFileFeatureCollection();
	}

	@Override
	public void generateRDF() throws IOException, InterruptedException {
		
		FeatureIterator iterator = this.featureCollection.features();
		int position = 0;
		long t_start = System.currentTimeMillis();
		long dt = 0;
		try {

			System.out.println(UtilsLib.getGMTime() + " Started processing features...");

			while (iterator.hasNext()) {
				SimpleFeatureImpl feature = (SimpleFeatureImpl) iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();

				String name = feature.getAttribute(this.featureName).toString();
				String originalName = feature.getAttribute(this.featureName).toString();

				String uri = UriResolver.getInstance().getUri(name, "Kreis " + feature.getAttribute(this.featureName).toString());
				UriResolver.getInstance().queryExtra(uri, areaType, model);
				
				System.out.println(originalName + ": " + uri);

				String countryID	= feature.getAttribute("ID_0").toString();
				String stateID		= feature.getAttribute("ID_1").toString();
				String adminID		= feature.getAttribute("ID_2") != null ? feature.getAttribute("ID_2").toString() : null;
				String districtID	= feature.getAttribute("ID_3") != null ? feature.getAttribute("ID_3").toString() : null;

				RdfExport.insertGadmTriples(model, uri, countryID, stateID, adminID, districtID);

				// Process non-spatial attributes for name and type
				RdfExport.handleNonGeometricAttributes(model, feature, uri, originalName, language);

				if (feature.getAttribute(this.featureAttribute) != null) {
					featureAttribute = feature.getAttribute(this.featureAttribute).toString();
				}

				// Type according to application schema
				RdfExport.insertResourceTypeResource(model, uri, "http://dbpedia.org/ontology/FederalState");

				// Type according to GeoSPARQL feature
				RdfExport.insertResourceTypeResource(model, uri, "http://www.opengis.net/ont/geosparql#" + Constants.FEATURE);

				RdfExport.insertResourceHierachie(model, uri, feature.getAttribute("NAME_2"), feature.getAttribute("NAME_1"), feature.getAttribute("NAME_0"));

				// changes dg
				// insertLabelResource(configuration.nsUri + aux,
				// featureAttribute, configuration.defaultLang);

				MultiPolygon multiPolygon = (MultiPolygon) geometry;
				for (int i = 0; i < multiPolygon.getNumGeometries(); ++i)
					RdfExport.insertPolygon(model, uri, multiPolygon.getGeometryN(i));

				++position;
			}
		}
		catch ( Exception e ) { e.printStackTrace(); }
		finally {
			iterator.close();
		}

		dt = System.currentTimeMillis() - t_start;
		System.out.println(UtilsLib.getGMTime() + " Parsing completed for " + position + " records in " + dt + " ms.");
		System.out.println(UtilsLib.getGMTime() + " Starting to write triplets to file...");

		// Count the number of statements
		long numStmt = model.listStatements().toSet().size();

		dt = System.currentTimeMillis() - t_start;
		System.out.println(UtilsLib.getGMTime() + " Process concluded in " + dt + " ms. " + numStmt + " triples successfully.");
	}
}