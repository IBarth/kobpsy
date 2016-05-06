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
package de.zpid.se4ojs.nlp;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordCoreferenceResolver;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.zpid.se4ojs.nlp.type.OntologyConcept;
import de.zpid.se4ojs.nlp.wsd.WordSenseDisambiguator;

/**
 * <p>
 * Annnotates the parargraphs of JATS-XML documents with ontology concepts (using NCBOAnnotator).
 * </p> 
 */

public class BioportalAnnotator extends JCasAnnotator_ImplBase
{
//	public static final String TEST_DIR = "src/test/resources/psychOpenTestDocs/";
	public static final String TEST_DIR = "/media/sf_shared/psychOpenTestAnnotation";
	public static final String XMI_TARGET_FILE = TEST_DIR  + "xmiDump.xmi";
    protected static final File TARGET_FILE = new File("target/result.txt");

	private static final String LANGUAGE_EN = "en";
	private static final Logger log = Logger.getLogger(BioportalAnnotator.class);
	private static final Integer COREF_MAXDIST = 3;
	private static final String REF_RELATION_PRONOMINAL = "PRONOMINAL";
	public static final String PREPROCESSED_VIEW = "preProcessedView";
	//TODO make configurable
	private static final String ONTOLOGIES = "MESH, APAONTO";
	public static final String XMI_DUMP_DIR = "xmiDump.xmi";
	
    public void annotateOntologyConcepts()
            throws IOException, UIMAException, SAXException
 {
		CollectionReaderDescription reader = createReaderDescription(
				JatsXmlReader.class,
				JatsXmlReader.PARAM_SOURCE_LOCATION, TEST_DIR,
				JatsXmlReader.PARAM_LANGUAGE, LANGUAGE_EN,
				JatsXmlReader.PARAM_PATTERNS, new String[] {"[+]*.xml", "[+]*.XML"},
						JatsXmlReader.PARAM_READ_PARAGRAPH, true,
						JatsXmlReader.PARAM_READ_SECTION, false);
		AnalysisEngineDescription segmenter = createEngineDescription(StanfordSegmenter.class);
//		AnalysisEngineDescription posTagger = createEngineDescription(StanfordPosTagger.class);
//		AnalysisEngineDescription lemmatizer = createEngineDescription(StanfordLemmatizer.class);
//		AnalysisEngineDescription parser = createEngineDescription(StanfordParser.class);
//		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class);
//		AnalysisEngineDescription corefResolver = createEngineDescription(
//				StanfordCoreferenceResolver.class,
//				StanfordCoreferenceResolver.PARAM_POSTPROCESSING, true,
//				StanfordCoreferenceResolver.PARAM_MAXDIST, COREF_MAXDIST);
//		AnalysisEngineDescription bioportalAnnotator = createEngineDescription(BioportalAnnotator.class);
//		AnalysisEngineDescription annotationValidator = WordSenseDisambiguator.createAnnotationValidator(false, false, false);
//		AnalysisEngineDescription ontologyConceptValidator = createEngineDescription(OntologyConceptValidator.class);
		AnalysisEngineDescription casDumpWriter = createEngineDescription(CasDumpWriter.class,
				CasDumpWriter.PARAM_OUTPUT_FILE, TEST_DIR + "/casDump");
		AnalysisEngineDescription xmiSerializer = createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, XMI_TARGET_FILE);

		SimplePipeline.runPipeline(reader,
				segmenter,
//				posTagger, lemmatizer, parser, ner,
//				corefResolver, bioportalAnnotator,
//				annotationValidator, ontologyConceptValidator,
				casDumpWriter, xmiSerializer);
			
	}
    
    public static void main(String[] args)
            throws IOException, UIMAException, SAXException {
    	
    	BioportalAnnotator bioportalAnnotator = new BioportalAnnotator();
    	bioportalAnnotator.annotateOntologyConcepts();
    	log.info("Finished Processing JATS articles");
    };

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		for (Sentence s : JCasUtil.select(jCas, Sentence.class)) {
			NCBOAnnotationFacilitator annotator = new NCBOAnnotationFacilitator(ONTOLOGIES, jCas);
			Map<Pair<Integer, Integer>, String> replacementMap  = replacePronominalCoreferences(jCas, s);
			
			List<OntologyConcept> concepts = annotator.mapConcepts(s, replacementMap);
			for (OntologyConcept concept : concepts) {
				concept.addToIndexes(jCas);	
			}
		}

	}

    private Map<Pair<Integer, Integer>, String> replacePronominalCoreferences(
    		JCas jCas, Sentence sentence) {
    	
    	Map<Pair<Integer, Integer>, String> replacementMap = new TreeMap<>();
		List<CoreferenceLink> links = JCasUtil.selectCovered(CoreferenceLink.class, sentence);
		for (CoreferenceLink link : links) {
			String refType = link.getReferenceType();
			if (refType != null && refType.equals(REF_RELATION_PRONOMINAL)) {
				//search root link in Coreference Chains
				String root = findRootWord(jCas, link);
				if (!StringUtils.isEmpty(root)) {
					replacementMap.put(
							new ImmutablePair<Integer, Integer>(link.getBegin(), link.getEnd()),
							root);
				}
			}
		}
		return replacementMap;
	}

    private String findRootWord(JCas jCas, CoreferenceLink link) {
    	Collection<CoreferenceChain> chains = JCasUtil.select(jCas, CoreferenceChain.class);
    	for (CoreferenceChain chain : chains) {
    		for (CoreferenceLink chainLink : chain.links()) {
    			if (chainLink.equals(link)) {
    				return chain.getFirst().getCoveredText();
    			}
    		}
    	}
    	return null;
    }
    
	@Override public void collectionProcessComplete()
            throws AnalysisEngineProcessException
    {
        super.collectionProcessComplete();

        System.out.println("TODO: inspect results after collection process complete");


    }
    

	public static Token findTokenByEndPosition(JCas jCas, int end) {
		try {
			for (Token token : JCasUtil.select(jCas.getView("_InitialView"),
					Token.class)) {
				if (token.getEnd() == end) {
					return token;
				}
			}
		} catch (CASException e) {
			throw new IllegalStateException(e);
		}

		return null;
	}	
}
