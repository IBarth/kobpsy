/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.zpid.se4ojs.nlp.nlpPreprocessor.wsd;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;
import java.util.Collection;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.dkpro.wsd.annotator.WSDAnnotatorCollectivePOS;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.DegreeCentralityWSD;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.MyWSDResourceDegreeCentrality;
import de.tudarmstadt.ukp.dkpro.wsd.si.SenseInventoryException;
import de.tudarmstadt.ukp.dkpro.wsd.si.wordnet.resource.WordNetSenseKeySenseInventoryResource;
import de.tudarmstadt.ukp.dkpro.wsd.type.WSDItem;
import de.tudarmstadt.ukp.dkpro.wsd.type.WSDResult;
import de.zpid.se4ojs.nlp.nlpPreprocessor.JatsXmlReader;
import net.sf.extjwnl.JWNLException;

/**
 * 
 */
public class AnnotationValidator
{
	static final String WORD_NET_PROPS_LOCATION =
			"/media/sf_shared/se4ojsNlp/se4ojs/sources/nlpPreprocessor/src/main/resources/extjwnl_properties.xml";
	
    public void disambiguateWordSense()
            throws IOException, UIMAException, SAXException, SenseInventoryException, UnsupportedOperationException, ClassNotFoundException, JWNLException
 {
		JCasIterable pipeline = SimplePipeline.iteratePipeline(
				createReaderDescription(),
				createSegmenterDescription(), createTaggerDescription(), createLemmatizerDescription(),
				createWsdItemCreator(), createDegreeCentralityDisambiguator());
		
		inspectPipeline(pipeline);

	}

	void inspectPipeline(Iterable<JCas> pipeline) {
		for (JCas jcas : pipeline) {
			System.out.println("cas view: " + jcas.getViewName());
			for (Sentence sentence : JCasUtil.select(jcas, Sentence. class)) {
				System.out.println("sent: " + sentence);
				for (Token token : JCasUtil.selectCovered(jcas, Token.class, sentence)) {
					System.out.println("token in sent: " + token);
				}
				for (Lemma lemma : JCasUtil.selectCovered(jcas, Lemma.class, sentence)) {
					System.out.println("lemma in sent: " + lemma);
				}
				for (WSDResult wsdResult : JCasUtil.selectCovered(jcas, WSDResult.class, sentence)) {
					System.out.println("sense inventory: " + wsdResult.getSenseInventory());
					System.out.println("best sense descr: " + wsdResult.getBestSense().getDescription());
				}
			}
//			Collection<Lemma> lemmata = JCasUtil.select(jcas, Lemma.class);
//			for (Lemma lemma : lemmata) {
//				System.out.println("lemma value: " + lemma.getValue());
//			}
			Collection<WSDItem> wsdItems = JCasUtil.select(jcas, WSDItem.class);
			String elementText = jcas.getSofaDataString();
			System.out.println("element text: " + elementText);
			for (WSDItem wsdItem : wsdItems) {
				System.out.println("wsItem: " + wsdItem.getCoveredText());
				System.out.println("pos: " + wsdItem.getPos());
			}
			Collection<WSDResult> wsdResults = JCasUtil.select(jcas, WSDResult.class);
			for (WSDResult wsdResult : wsdResults) {
				System.out.println("wsItem: " + wsdResult.getWsdItem().getCoveredText());
				System.out.println("best sense description" + wsdResult.getBestSense().getDescription());
			}
		}
		
	}
	
	AnalysisEngineDescription createWsdItemCreator() throws ResourceInitializationException {
		AnalysisEngineDescription wsdItemCreator = createEngineDescription(
				AddWSDItemToLemmatizedSentence.class);
		return wsdItemCreator;
	}

	AnalysisEngineDescription createDegreeCentralityDisambiguator() throws ResourceInitializationException {
		ExternalResourceDescription wordNet = createWordNetResource();
		
		/** Configure the wsd algorithm. */
        ExternalResourceDescription degreeCentralityResource = ExternalResourceFactory.createExternalResourceDescription(
        		MyWSDResourceDegreeCentrality.class,
        		MyWSDResourceDegreeCentrality.SENSE_INVENTORY_RESOURCE, wordNet,
        		MyWSDResourceDegreeCentrality.DISAMBIGUATION_METHOD, DegreeCentralityWSD.class.getName(),
        		MyWSDResourceDegreeCentrality.PARAM_SEARCH_DEPTH, "4");


		
		AnalysisEngineDescription wsDisambiguator = createEngineDescription(
				WSDAnnotatorCollectivePOS.class,
				WSDAnnotatorCollectivePOS.WSD_ALGORITHM_RESOURCE, degreeCentralityResource,
//				WSDAnnotatorCollectivePOS.PARAM_DISAMBIGUATION_METHOD_NAME, "degreeCentrality",
				WSDAnnotatorCollectivePOS.PARAM_ALLOW_EMPTY_WSD_RESULTS, true,
				WSDAnnotatorCollectivePOS.PARAM_SET_SENSE_DESCRIPTIONS, true);
		return wsDisambiguator;
	}

	private ExternalResourceDescription createWordNetResource() {
		ExternalResourceDescription wordNet = ExternalResourceFactory.createExternalResourceDescription(
				WordNetSenseKeySenseInventoryResource.class,
				WordNetSenseKeySenseInventoryResource.PARAM_WORDNET_PROPERTIES_URL, WORD_NET_PROPS_LOCATION,
				WordNetSenseKeySenseInventoryResource.PARAM_SENSE_INVENTORY_NAME, "wordNet",
				WordNetSenseKeySenseInventoryResource.PARAM_GRAPH_URL, GraphUtil.SERIALIZED_GRAPH_LOCATION
		);
		return wordNet;
	}

	AnalysisEngineDescription createLemmatizerDescription() throws ResourceInitializationException {
		AnalysisEngineDescription lemmatizer = createEngineDescription(StanfordLemmatizer.class);
		return lemmatizer;
	}

	AnalysisEngineDescription createTaggerDescription() throws ResourceInitializationException {
		AnalysisEngineDescription posTagger = createEngineDescription(StanfordPosTagger.class);
		return posTagger;
	}

	AnalysisEngineDescription createSegmenterDescription() throws ResourceInitializationException {
		AnalysisEngineDescription segmenter = createEngineDescription(StanfordSegmenter.class);
		return segmenter;
	}

	CollectionReaderDescription createReaderDescription() throws ResourceInitializationException {
		return CollectionReaderFactory.createReaderDescription(
				JatsXmlReader.class,
				JatsXmlReader.PARAM_INPUT_FILE, "./ejcop.v2i2.34.XML",
				JatsXmlReader.PARAM_LANGUAGE, JatsXmlReader.DOC_LANGUAGE_ENGLISH,
				JatsXmlReader.PARAM_XML_ELEMENT_TO_EXTRACT, JatsXmlReader.TAG_NAME_PARAGRAPH);
	}

	public static void main(String[] args)
            throws IOException, UIMAException, SAXException, SenseInventoryException, UnsupportedOperationException, ClassNotFoundException, JWNLException {
    	AnnotationValidator resolver = new AnnotationValidator();
    	resolver.disambiguateWordSense();
    }
}
