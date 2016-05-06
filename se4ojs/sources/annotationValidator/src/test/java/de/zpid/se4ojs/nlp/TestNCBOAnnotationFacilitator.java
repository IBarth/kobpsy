package de.zpid.se4ojs.nlp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;
import org.mockito.Mockito;
import org.zpid.se4ojs.annotation.ncbo.NcboAnnotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.zpid.se4ojs.nlp.type.OntologyConcept;
import de.zpid.se4ojs.nlp.type.OntologyConcept_Type;


public class TestNCBOAnnotationFacilitator {

	private static final String COVERED_TEXT_1 = "The cat ate cat food.";
	private static final Integer ANNOT_ABS_BEGIN_POS = 100;
	private static final String TYPE_SYSTEM_DESC_PATH = "src/main/resources/desc/type/ConceptAnnotation";
	private JCas jcas;
	private Map<Pair<Integer, Integer>, String> replacements;
	private NCBOAnnotationFacilitator facilitator = new NCBOAnnotationFacilitator("AnOntology", jcas);
	
	@Test
	public void testFilterText() {
		jcas = mock(JCas.class);
		Annotation annotation = mock(Annotation.class);
		when(annotation.getCoveredText()).thenReturn(COVERED_TEXT_1);
		when(annotation.getBegin()).thenReturn(ANNOT_ABS_BEGIN_POS);
		when(annotation.getEnd()).thenReturn(ANNOT_ABS_BEGIN_POS + COVERED_TEXT_1.length());

		replacements = createReplacementMap(COVERED_TEXT_1,
				new String[] {"The", "cat", "food"}, new String[] {"A", "mammal", "strawberrys"});

		String filteredText = facilitator.filterText(annotation, replacements);
		assertEquals("Substition failed: ", "A mammal ate mammal strawberrys.", filteredText);
		
		//check if the beginning of the string is correctly retained if the substitutions don't start there.
		replacements = createReplacementMap(COVERED_TEXT_1,
				new String[] {"cat"}, new String[] {"x"});
		filteredText = facilitator.filterText(annotation, replacements);
		assertEquals("Substition failed: ", "The x ate x food.", filteredText);
	}
	
	@Test
	public void testSetAnnotationSpan() throws UIMAException {
		JCas jCas = JCasFactory.createJCas(TYPE_SYSTEM_DESC_PATH);
		JsonNode annotationInfo = mock(ArrayNode.class);
		JsonNode inf1 = mock(JsonNode.class);
		@SuppressWarnings("unchecked")
		Iterator<JsonNode> iterator = mock(Iterator.class);
		when(annotationInfo.iterator()).thenReturn(iterator);
		when(iterator.hasNext()).thenReturn(true, true, false);
		when(iterator.next()).thenReturn(inf1);
		when(inf1.get("text")).thenReturn(new TextNode("cat"), new TextNode("cat"), new TextNode("food"));
		
//		OntologyConcept concept = mock(OntologyConcept.class);
		OntologyConcept concept = new OntologyConcept(jCas);
//		when(concept.clone()).thenReturn(mock(OntologyConcept.class));
		replacements = createReplacementMap(COVERED_TEXT_1,
				new String[] {"cat"}, new String[] {"mammal"});
		List<OntologyConcept> concepts =
				facilitator.setAnnotationSpan(COVERED_TEXT_1, ANNOT_ABS_BEGIN_POS,
						annotationInfo, concept, replacements);
		
		assertEquals("Wrong no. of annotated concepts: ", 2, concepts.size());
		for (OntologyConcept c : concepts) {
			
		}
		
		
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testCallNcboAnnotator() throws UnsupportedEncodingException, UIMAException {
		NcboAnnotator annotator = new NcboAnnotator("MESH, APAONTO");
		JsonNode results = annotator.callAnnotator("cat cat");
		for (JsonNode result : results){
			JsonNode annoInfos = result.get("annotations");
			JCas jCas = JCasFactory.createJCas(TYPE_SYSTEM_DESC_PATH);
			NCBOAnnotationFacilitator fac = new NCBOAnnotationFacilitator("", jcas);
			fac.setAnnotationSpan("cat cat", 0, annoInfos, new OntologyConcept(jCas), Collections.EMPTY_MAP);
			assertEquals("Wrong number of annotations returned by annotation service", 	2, annoInfos.size());
		}
	}
	private Map<Pair<Integer, Integer>, String> createReplacementMap(String originalText,
			String[] toReplace, String[] substitutes) {
		
		Map<Pair<Integer, Integer>, String> replacements = new HashMap<>();
		assertEquals("Unequal no. of strings to replace and substitutes",
				toReplace.length, substitutes.length);
		for (int i = 0; i < toReplace.length; i++) {
			int idx = 0;
			while (idx != -1) {
				if (idx < originalText.length()) {
					idx = originalText.indexOf(toReplace[i], idx);
					if (idx != -1) {
						replacements.put(new ImmutablePair<Integer,Integer>(
							idx + ANNOT_ABS_BEGIN_POS, idx + ANNOT_ABS_BEGIN_POS + toReplace[i].length()), substitutes[i]);
						idx = idx + 1;
					}
				} else {
					idx = -1;
				}
			}
		}
		return replacements;
	}
}
