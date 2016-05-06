package de.zpid.se4ojs.nlp.wsd;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.DegreeCentralityWSD;
import de.tudarmstadt.ukp.dkpro.wsd.type.WSDItem;

/**
 * Prepares for WSd by {@link DegreeCentralityWSD}:
 * WSDItems are produced (TODO filter by POS and annotate nouns / NPs only? CAVE: we have some compounds that should not be split.
 *     
 * TODO the {@link DegreeCentralityWSD} algorithms itself expects input of the following format:
 * 		Collection<Pair<String, POS>> sods.
 * 
 * 
 * @author barth
 *
 */
public class WSDItemizer extends JCasAnnotator_ImplBase {

	private Logger log = Logger.getLogger(WSDItemizer.class);

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		Collection<Sentence> sentences = JCasUtil.select(aJCas, Sentence.class);
		int sentenceCount = 0;
		log.debug("WSDItemizer:");
		for (Sentence sentence : sentences) {
			log.debug("sent: " + sentence);
			
			int tokenCount = 0;
			for (Token token : JCasUtil.selectCovered(aJCas, Token.class, sentence)) {
				if (token.getPos() != null) {
					String posValue = token.getPos().getPosValue();
					
					String wnPos = posToWsdSimplePos(posValue);
					System.out.println("token in sent: " + token);
					if (wnPos != null) {
						WSDItem wsdItem = new WSDItem(aJCas);
						wsdItem.setPos(wnPos);
						String lemmaValue = token.getLemma().getValue();
						wsdItem.setSubjectOfDisambiguation(lemmaValue);
						wsdItem.setBegin(token.getBegin());
						wsdItem.setEnd(token.getEnd());
						wsdItem.setId(lemmaValue + "_" + posValue + ++sentenceCount + "_" + ++tokenCount);
						wsdItem.addToIndexes();
					}
				} else {
					log.warn("token's POS value is null! " + token.toString());
				}
			}
		}
	}
	
    protected String posToWsdSimplePos(String pos)
    {
        if (pos == null) {
            throw new IllegalArgumentException();
        }
        if (pos.startsWith("JJ")) {
            return de.tudarmstadt.ukp.dkpro.wsd.si.POS.ADJ.name();
        }
        else if (pos.startsWith("RB")) {
            return de.tudarmstadt.ukp.dkpro.wsd.si.POS.ADV.name();

        }
        else if (pos.startsWith("VB") || pos.equals("MD")) {
            return de.tudarmstadt.ukp.dkpro.wsd.si.POS.VERB.name();
        }
        else if (pos.startsWith("NN")) {
            return de.tudarmstadt.ukp.dkpro.wsd.si.POS.NOUN.name();
        }
        else {
            return null;
        }
    }
	
}