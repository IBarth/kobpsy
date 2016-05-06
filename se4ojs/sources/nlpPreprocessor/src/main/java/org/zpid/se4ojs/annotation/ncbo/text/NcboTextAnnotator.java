package org.zpid.se4ojs.annotation.ncbo.text;

import java.io.UnsupportedEncodingException;

import org.ontoware.rdf2go.model.Model;
import org.zpid.se4ojs.annotation.ncbo.NcboAnnotator;

import com.fasterxml.jackson.databind.JsonNode;

public class NcboTextAnnotator extends NcboAnnotator {

	public static final String ontologies = "MESH";
	public NcboTextAnnotator(String ontologies) {
		super(ontologies);
	}

	public void annotateParagraph(String paragraphText) {
		try {
			super.annotateText(null, paragraphText, null);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void rdfizeAnnotations(Model model, JsonNode results, String textStructElementUri) {
        for (JsonNode result : results) {
            // Get the details for the class that was found in the annotation and print
            JsonNode classDetails = super.jsonToNode(super.get(result.get("annotatedClass").get("links").get("self").asText()));
			JsonNode annotationInfo = result.get("annotations");
            if (classDetails != null) {
                String conceptId = getClassDetail(classDetails, "@id");
                String prefLabel = getClassDetail(classDetails, "prefLabel");
                String conceptBrowserUrl = getClassDetail(classDetails, "links" , "ui");

            }
        }
		
	}



	
}
