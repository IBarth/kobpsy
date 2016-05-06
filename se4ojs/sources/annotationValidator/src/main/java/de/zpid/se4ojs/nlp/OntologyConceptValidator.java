package de.zpid.se4ojs.nlp;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.wsd.si.SenseInventoryException;
import de.tudarmstadt.ukp.dkpro.wsd.si.wordnet.WordNetSenseKeySenseInventory;
import de.tudarmstadt.ukp.dkpro.wsd.type.WSDResult;
import de.zpid.se4ojs.nlp.similarity.TextSimilarity;
import de.zpid.se4ojs.nlp.type.OntologyConcept;
import de.zpid.se4ojs.nlp.wsd.WordSenseDisambiguator;
import net.sf.extjwnl.JWNLException;

/**
 *
 * 
 * TODO perform the concept validation by comparing
 * 	    - the definitions of the concepts to the definition of the best (e.g. WordNet) sense of the text occurrence
 *      - " " concepts' ancestors " 	"
 *      - the synonyms with the text occurrences' best senses synonyms
 *      
 * @author barth
 *
 */
public class OntologyConceptValidator extends JCasAnnotator_ImplBase {
	
	private static final int SCORE_MATCHED_WORD_COMPARED_TO_CONCEPT_DESCRIPTION = 1;
	
	private static Map<String,Double> scores;

	private Logger log = Logger.getLogger(OntologyConceptValidator.class);
	
	private WordNetSenseKeySenseInventory inventory;

	/**
	 * Loads the word net sense key inventory.
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		try {
			inventory = new WordNetSenseKeySenseInventory(
			        OntologyConceptValidator.class.getClassLoader().getResourceAsStream("extjwnl_properties.xml"));
			scores = new HashMap<>();
		} catch (JWNLException | IOException e) {
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		Collection<OntologyConcept> concepts = JCasUtil.select(jCas, OntologyConcept.class);
		for (OntologyConcept concept : concepts) {
			double score = 0;
			Double prevScore = scores.get(concept.getConceptId());
			log.info("conceptID: " + concept.getConceptId());
			if (prevScore  != null) {
				score = prevScore;
			} else {
				//disambiguate the words in the WordNet-Concept's description
				//and store it in a cas.
				WSDResult textWSDForConcept = getWSDFromTextContext(jCas, concept.getBegin());
				String wordNetConceptDescription = getWordNetConceptDescription(textWSDForConcept);
				JCas wordNetCas = disambiguateConceptDescription(jCas, wordNetConceptDescription, concept.getConceptId());
				//disambiguate the words in the Bioportal-Concept's description
				//and store it in a cas.
				JCas conceptCas = disambiguateConceptDescription(jCas, concept.getDescription(), concept.getConceptId());
				Set<String> conceptDescrSenseIds = collectConceptDescSenseIds(conceptCas);
				log.info("Starting scoring.");
				log.info("\tannotated text: " + concept.getCoveredText());
				score += scoreWordOccurrenceInConceptDefinition(conceptDescrSenseIds, textWSDForConcept);
				//TODO score = scoreConceptDefAndSentenceSimilarity();
				score += scoreBiopotalConceptAndWordNetConceptDefinitionSimilarity(conceptCas, wordNetCas);
				log.info("\tscore: " + score + " concept descr: " + conceptCas.getDocumentText());
				log.info("\t\t wordNet sense descr: " + wordNetCas.getDocumentText());
				scores.put(concept.getConceptId(), score);
				//TODO call text similarity algorithms to compare sentence where concept occurs with definition
			}
		}
	}
	
	/**
	 * Scores the similarity between the description of a bioportal concept (as stored in the conceptCas) and the
	 * definition of the WordNet sense description of the sense assigned to the word the concept as been mapped to.
	 * This WordNet sense has been selected on the basis of the word's context in the sentence.
	 *  
	 * TODO only score if wordNet sense exists. Mark scores (e.g. w. value -1) where this isn't the case.
	 * 
	 * @param conceptCas
	 * @param wordNetCas
	 * @return the score
	 */
	private double scoreBiopotalConceptAndWordNetConceptDefinitionSimilarity(JCas conceptCas, JCas wordNetCas) {
		if (conceptCas != null && wordNetCas != null) {
			TextSimilarity similarity = new TextSimilarity(conceptCas, wordNetCas);
			return similarity.scoreTextSimilarity();
		}
		return 0;
	}

	private String getWordNetConceptDescription(WSDResult textWSDForConcept) {
		if (textWSDForConcept != null) {
			log.info("word: " + textWSDForConcept.getCoveredText());
			try {
				log.info("WordNet Sense Def: " + inventory.getSenseDefinition(textWSDForConcept.getBestSense().getId()));
				log.info("WordNet Sense Description: " + inventory.getSenseDescription(textWSDForConcept.getBestSense().getId()));
				return inventory.getSenseDefinition(textWSDForConcept.getBestSense().getId());
			} catch (SenseInventoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return StringUtils.EMPTY;
	}

	/**
	 * Scores if the WordNet sense Ids identified within the concept description contain the
	 * best sense that has been assigned to the given word from the running text.
	 * @param conceptDescrSenseIds the WordNet sense Ids for the words occurring in the concept description
	 * @param textWSDForConcept the WordNet disambiguation result for a word that has been mapped to a concept.
	 *        The disambiguation has been performed with the context from the running text.
	 * @return the similarity score
	 */
	private int scoreWordOccurrenceInConceptDefinition(Set<String> conceptDescrSenseIds, WSDResult textWSDForConcept) {
		if (textWSDForConcept != null && textWSDForConcept.getBestSense() != null &&
				conceptDescrSenseIds.contains(textWSDForConcept.getBestSense().getId())) {
				return SCORE_MATCHED_WORD_COMPARED_TO_CONCEPT_DESCRIPTION;
		}
		return 0;
	}

	/**
	 * Workaround because for WSDResults the start and end positions have not been set by the WSDAlgorithm (see
	 * method "setItem".
	 * 
	 * @param jCas
	 * @return
	 */
	private WSDResult getWSDFromTextContext(JCas jCas, int startPos) {
		
		for (WSDResult wsdRes : JCasUtil.select(jCas, WSDResult.class)) {
			if(wsdRes.getWsdItem().getBegin() ==  startPos) {
				return wsdRes;
			}
		}
		return null;
	}

	/**
	 * Collects all the "best-sense IDs" of a concept description in a set.
	 * 
	 * @param conceptCas the cas containing the concept description annotations
	 * @return the best sense IDs collected in a set
	 */
	private Set<String> collectConceptDescSenseIds(JCas conceptCas) {

		Set<String> senseIDs = new HashSet<>();
		Collection<WSDResult> wsdResults = JCasUtil.select(conceptCas, WSDResult.class);
		for (WSDResult res : wsdResults) {
			senseIDs.add(res.getBestSense().getId());
		}
		return senseIDs;
	}


	/**
	 * Performs WSD ambiguation of the passed in text (e.g. a concept description).
	 * @param jCas
	 * @param conceptDescription
	 * @param id
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	public JCas disambiguateConceptDescription(JCas jCas, String conceptDescription, String id)
			throws AnalysisEngineProcessException {
		JCas newCas = null;
		try {
			newCas = JCasFactory.createJCas();
		} catch (UIMAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("concept descr text: " + conceptDescription);
		if (!StringUtils.isEmpty(conceptDescription)) {
			DocumentMetaData metaData = DocumentMetaData.create(newCas);
			metaData.setDocumentId(id);
			metaData.setDocumentTitle(id);
			newCas.setSofaDataString(conceptDescription, "text");
			newCas.setDocumentLanguage(jCas.getDocumentLanguage());
			AnalysisEngineDescription definitionDisambiguator = null;
			try {
				definitionDisambiguator = WordSenseDisambiguator.createAnnotationValidator(true, true, true);
				AnalysisEngine definitionDisambiguationEngine =
					AnalysisEngineFactory.createEngine(definitionDisambiguator, newCas.getViewName());
				definitionDisambiguationEngine.process(newCas);
			} catch (ResourceInitializationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return newCas;
	}
	
	public static void main (String[] args) throws ResourceInitializationException, UIMAException, IOException {
	       SimplePipeline.runPipeline(CollectionReaderFactory
                   .createReaderDescription(XmiReader.class, XmiReader.PARAM_SOURCE_LOCATION,
                           BioportalAnnotator.TEST_DIR + "/" + BioportalAnnotator.XMI_DUMP_DIR, XmiReader.PARAM_PATTERNS,
                           XmiReader.INCLUDE_PREFIX + "*.xmi"),
                   AnalysisEngineFactory.createEngineDescription(OntologyConceptValidator.class));
	}
}
