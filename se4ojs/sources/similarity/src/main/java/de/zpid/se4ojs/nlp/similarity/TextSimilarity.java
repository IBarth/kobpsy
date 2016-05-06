package de.zpid.se4ojs.nlp.similarity;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.TypeSystemUtil;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import dkpro.similarity.algorithms.lexical.uima.ngrams.WordNGramContainmentResource;
import dkpro.similarity.uima.annotator.SimilarityScorer;
import dkpro.similarity.uima.api.type.ExperimentalTextSimilarityScore;

public class TextSimilarity {;

	static final String VIEW_1_NAME = "conceptView";
	static final String VIEW_2_NAME = "wordNetView";
	private static final int MAX_N_GRAM_SIZE = 3;
	private JCas jcas;
	private Logger log = Logger.getLogger(TextSimilarity.class);
	
	public TextSimilarity(JCas conceptCas, JCas wordNetCas) {
		this.jcas = createCombinedJCas(conceptCas, wordNetCas);
	}

	/**
	 * Creates a JCas as expected by {@link SimilarityScorer} that holds the passed in JCases as individual views.
	 * 
	 * @param conceptDefCas the cas containing the annotations of the ontology concept definition of a term.
	 * @param wordNetDefCas the jcas containing the annotations of the WordNet definition of a term.
	 * @return the combined JCas.
	 */
	public JCas createCombinedJCas(JCas conceptDefCas, JCas wordNetDefCas) {
		JCas jcas = null;
		if (DocumentMetaData.get(conceptDefCas.getCas())  == null) {
			log.error("Doc meta data is null");
			if (StringUtils.isEmpty(conceptDefCas.getDocumentText())) {
				log.warn("Doc text empty in concept cas.");
			}
		} else {		
			try {
				TypeSystemDescription typeSystemDescription = 
						TypeSystemUtil.typeSystem2TypeSystemDescription(
								conceptDefCas.getTypeSystem());
				jcas = JCasFactory.createJCas(typeSystemDescription);
				jcas.setDocumentLanguage("en");
	
				CasCopier casCopier = new CasCopier(conceptDefCas.getCas(), jcas.getCas(), true);
	
				DocumentMetaData.copy(conceptDefCas, jcas);
				casCopier.copyCasView(conceptDefCas.getCas(), VIEW_1_NAME, true);
				DocumentMetaData.create(jcas.getView(VIEW_1_NAME));
				casCopier = new CasCopier(wordNetDefCas.getCas(), jcas.getCas(), true);
				casCopier.copyCasView(wordNetDefCas.getCas(), VIEW_2_NAME, true);
				DocumentMetaData.create(jcas.getView(VIEW_2_NAME));
			} catch (UIMAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return jcas;
	}

	public double scoreTextSimilarity() {
		double score = 0;
		for (int i = 1; i <= MAX_N_GRAM_SIZE; i++) {
			try {
				AnalysisEngine similarityScorer = getSimilarityScorer(i);
				SimplePipeline.runPipeline(jcas, similarityScorer);
				Collection<ExperimentalTextSimilarityScore> scores = JCasUtil.select(jcas, ExperimentalTextSimilarityScore.class);
 				for (ExperimentalTextSimilarityScore singleScore : scores) {
					log.info("\t\t intermediate score: " + singleScore.getScore() + "nGram size: " + i);
					score += singleScore.getScore() * i;
				}
			} catch (ResourceInitializationException | AnalysisEngineProcessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return score;
	}
	
	private static AnalysisEngine getSimilarityScorer(int i) throws ResourceInitializationException {
		AnalysisEngine scorer = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
				SimilarityScorer.class, SimilarityScorer.PARAM_NAME_VIEW_1, VIEW_1_NAME,
				SimilarityScorer.PARAM_NAME_VIEW_2, VIEW_2_NAME,
				SimilarityScorer.PARAM_TEXT_SIMILARITY_RESOURCE,
					ExternalResourceFactory.createExternalResourceDescription(
						WordNGramContainmentResource.class,
						WordNGramContainmentResource.PARAM_N, String.valueOf(i))));

		return scorer;
	}
}

