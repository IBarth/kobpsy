package de.zpid.se4ojs.nlp.wsd;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Collection;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.dkpro.wsd.annotator.WSDAnnotatorCollectivePOS;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.DegreeCentralityWSD;
import de.tudarmstadt.ukp.dkpro.wsd.si.wordnet.resource.WordNetSenseKeySenseInventoryResource;
import de.tudarmstadt.ukp.dkpro.wsd.type.WSDItem;
import de.tudarmstadt.ukp.dkpro.wsd.type.WSDResult;
import de.tudarmsteadt.ukp.dkpro.wsd.graphconnectivity.algorithm.MyWSDResourceDegreeCentrality;
import de.tudarmstadt.ukp.dkpro.wsd.type.WSDResult_Type;
/**
 * @author barth
 */
public class WordSenseDisambiguator {
	static final String WORD_NET_PROPS_LOCATION =
			"/media/sf_shared/se4ojsNlp/se4ojs/sources/nlpPreprocessor/src/main/resources/extjwnl_properties.xml";
	private static final String SEARCH_DEPTH_DEGREE_CENTRALITY = "2";
	
    
    public static AnalysisEngineDescription createAnnotationValidator(boolean requiresSegmenter, boolean requiresTagger, 
    		boolean requiresLemmatizer) throws ResourceInitializationException {
    	
    	AggregateBuilder builder  = new AggregateBuilder();
    	if (requiresSegmenter) {
    		builder.add(createSegmenterDescription());
    	}
    	if (requiresTagger) {
    		builder.add(createTaggerDescription());
    	}
    	if (requiresLemmatizer) {
    		builder.add(createLemmatizerDescription());
    	}
    	builder.add(createWsdItemCreator());
    	builder.add(createDegreeCentralityDisambiguator());
    	
    	return builder.createAggregateDescription();
    }

//	void inspectPipeline(Iterable<JCas> pipeline) {
//		for (JCas jcas : pipeline) {
//			for (Sentence sentence : JCasUtil.select(jcas, Sentence. class)) {
//				for (WSDResult wsdResult : JCasUtil.selectCovered(jcas, WSDResult.class, sentence)) {
//					System.out.println("sense inventory: " + wsdResult.getSenseInventory());
//					System.out.println("best sense descr: " + wsdResult.getBestSense().getDescription());
//				}
//			}
////			Collection<Lemma> lemmata = JCasUtil.select(jcas, Lemma.class);
////			for (Lemma lemma : lemmata) {
////				System.out.println("lemma value: " + lemma.getValue());
////			}
//			Collection<WSDItem> wsdItems = JCasUtil.select(jcas, WSDItem.class);
//			String elementText = jcas.getSofaDataString();
//			System.out.println("element text: " + elementText);
//			for (WSDItem wsdItem : wsdItems) {
//				System.out.println("wsItem: " + wsdItem.getCoveredText());
//				System.out.println("pos: " + wsdItem.getPos());
//			}
//			Collection<WSDResult> wsdResults = JCasUtil.select(jcas, WSDResult.class);
//			for (WSDResult wsdResult : wsdResults) {
//				System.out.println("wsItem: " + wsdResult.getWsdItem().getCoveredText());
//				System.out.println("best sense description" + wsdResult.getBestSense().getDescription());
//			}
//		}
//		
//	}
	
	static AnalysisEngineDescription createWsdItemCreator() throws ResourceInitializationException {
		AnalysisEngineDescription wsdItemCreator = createEngineDescription(
				WSDItemizer.class);
		return wsdItemCreator;
	}

	static AnalysisEngineDescription createDegreeCentralityDisambiguator() throws ResourceInitializationException {
		ExternalResourceDescription wordNet = createWordNetResource();
		
		/** Configure the wsd algorithm. */
        ExternalResourceDescription degreeCentralityResource = ExternalResourceFactory.createExternalResourceDescription(
        		MyWSDResourceDegreeCentrality.class,
        		MyWSDResourceDegreeCentrality.SENSE_INVENTORY_RESOURCE, wordNet,
        		MyWSDResourceDegreeCentrality.DISAMBIGUATION_METHOD, DegreeCentralityWSD.class.getName(),
        		MyWSDResourceDegreeCentrality.PARAM_SEARCH_DEPTH, SEARCH_DEPTH_DEGREE_CENTRALITY);


		
		AnalysisEngineDescription wsDisambiguator = createEngineDescription(
				WSDAnnotatorCollectivePOS.class,
				WSDAnnotatorCollectivePOS.WSD_ALGORITHM_RESOURCE, degreeCentralityResource,
//				WSDAnnotatorCollectivePOS.PARAM_DISAMBIGUATION_METHOD_NAME, "degreeCentrality",
				WSDAnnotatorCollectivePOS.PARAM_ALLOW_EMPTY_WSD_RESULTS, true,
				WSDAnnotatorCollectivePOS.PARAM_SET_SENSE_DESCRIPTIONS, true);
		return wsDisambiguator;
	}

	private static ExternalResourceDescription createWordNetResource() {
		ExternalResourceDescription wordNet = ExternalResourceFactory.createExternalResourceDescription(
				WordNetSenseKeySenseInventoryResource.class,
				WordNetSenseKeySenseInventoryResource.PARAM_WORDNET_PROPERTIES_URL, WORD_NET_PROPS_LOCATION,
				WordNetSenseKeySenseInventoryResource.PARAM_SENSE_INVENTORY_NAME, "wordNet",
				WordNetSenseKeySenseInventoryResource.PARAM_GRAPH_URL, GraphUtil.SERIALIZED_GRAPH_LOCATION
		);
		return wordNet;
	}

	static AnalysisEngineDescription createLemmatizerDescription() throws ResourceInitializationException {
		AnalysisEngineDescription lemmatizer = createEngineDescription(StanfordLemmatizer.class);
		return lemmatizer;
	}

	static AnalysisEngineDescription createTaggerDescription() throws ResourceInitializationException {
		AnalysisEngineDescription posTagger = createEngineDescription(StanfordPosTagger.class);
		return posTagger;
	}

	static AnalysisEngineDescription createSegmenterDescription() throws ResourceInitializationException {
		AnalysisEngineDescription segmenter = createEngineDescription(StanfordSegmenter.class);
		return segmenter;
	}

}
