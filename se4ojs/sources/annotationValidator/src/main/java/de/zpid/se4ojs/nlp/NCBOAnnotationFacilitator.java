package de.zpid.se4ojs.nlp;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.zpid.se4ojs.annotation.ncbo.NcboAnnotator;

import com.fasterxml.jackson.databind.JsonNode;

import de.zpid.se4ojs.nlp.type.OntologyConcept;

/**
 * <p>
 * Annotates text with Ontology Concepts utilizing {@link NcboAnnotator}.
 * </p>
 * <p>
 * An {@link Annotation} together with the ontologies to annotate
 * the text covered by the annotation with are passed in.
 * It is also possible to pass in a filter: Items within the text are
 * replaced by specified substitutes before the text is passed to the @link {@link NCBOAnnotator}.
 * </p>
 * 
 * TODO: Read out & store concept synonyms and parent concepts.
 * TODO: Extend OntologyConcept type: store parent as nested OntologyConcept. 
 * 
 * @author barth
 *
 */
public class NCBOAnnotationFacilitator {
	
	private Logger log = Logger.getLogger(NCBOAnnotationFacilitator.class);

	private JCas jcas;
	
	private NcboAnnotator ncboAnnotator;

	public NCBOAnnotationFacilitator(String ontologies, JCas jcas) {
		this.ncboAnnotator = new NcboAnnotator(ontologies);
		this.jcas = jcas;
	}

	public List<OntologyConcept> mapConcepts(Annotation annotation,
			Map<Pair<Integer, Integer>, String> replacementMap) {
		
		List<OntologyConcept> concepts = new ArrayList<>();
		try {
			String filteredText = filterText(annotation, replacementMap);
			JsonNode results = ncboAnnotator.callAnnotator(filteredText);
			if (results != null) {
				concepts = extractConcepts(annotation, results, replacementMap);
			} else {
				log.error("NCBOAnnotator: Results are null!. : Text: " + filteredText);
			}
	     
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return concepts;
	}

	private List<OntologyConcept> extractConcepts(Annotation annotation, JsonNode results,
			Map<Pair<Integer, Integer>, String> replacementMap) {
		List<OntologyConcept> concepts = new ArrayList<>();
		   for (JsonNode result : results) {
	        	OntologyConcept concept = new OntologyConcept(jcas);
	            JsonNode classDetails = NcboAnnotator.jsonToNode(
	            		NcboAnnotator.get(result.get("annotatedClass").get("links").get("self").asText()));

	            if (classDetails != null) {
	                String conceptId = ncboAnnotator.getClassDetail(classDetails, "@id");
	                concept.setConceptId(conceptId);
	                String prefLabel = ncboAnnotator.getClassDetail(classDetails, "prefLabel");
	                concept.setConceptLabel(prefLabel);
	                String ontology =  classDetails.get("links").get("ontology").asText();
	                concept.setOntology(ontology);
	                JsonNode definitions = classDetails.get("definition");
	                StringBuilder description = new StringBuilder();
	                for (JsonNode def : definitions) {
	                	description.append(def);
	                }
	                concept.setDescription(description.toString());
	            }
	            
				JsonNode matchInfo = result.get("annotations");
				concepts.addAll(setAnnotationSpan(annotation.getCoveredText(),
						annotation.getBegin(), matchInfo, concept, replacementMap));
	        }
		   return concepts;
	}

	List<OntologyConcept> setAnnotationSpan(String annotans, int docStartPos,
			JsonNode annotationInfo, OntologyConcept concept, Map<Pair<Integer, Integer>, String> replacementMap) {
		
		List<OntologyConcept> concepts = new ArrayList<>();
		String text = annotans.toLowerCase();
		if (annotationInfo != null) {
			JsonNode inf = annotationInfo.iterator().next();
        	String matchedWords = inf.get("text").asText().toLowerCase();
        	int startMatch = 0;
        	while (startMatch != -1) {
        		startMatch = text.indexOf(matchedWords, startMatch);
        		if (startMatch != -1) {
            		concept = (OntologyConcept) concept.clone();
                	concept.setBegin(docStartPos + startMatch);
                	concept.setEnd(docStartPos + startMatch + matchedWords.length());
                	concepts.add(concept);
        			++startMatch;
        		}
        	}
		}
		return concepts;
	}

	String filterText(Annotation annotation,
			Map<Pair<Integer, Integer>, String> replacementMap) {
		
		String annotans = annotation.getCoveredText();
		Set<Pair<Integer, Integer>> tokenBoundaries = replacementMap.keySet();
		StringBuilder sb = new StringBuilder();
		int offset = 0;
		for (Pair<Integer, Integer> tokenBoundary : tokenBoundaries) {
			int tokenBeginRelToSentence = tokenBoundary.getLeft() - annotation.getBegin();
			sb.append(annotans.substring(offset, tokenBeginRelToSentence));
			offset = tokenBeginRelToSentence;
			sb.append(replacementMap.get(tokenBoundary));
			int tokenEndRelToSentence = tokenBoundary.getRight() - annotation.getBegin();
			offset = tokenEndRelToSentence;
		}
		sb.append(annotans.substring(offset, annotans.length()));
		return sb.toString();
	}

}
