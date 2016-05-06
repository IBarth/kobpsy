package de.tudarmsteadt.ukp.dkpro.wsd.graphconnectivity.algorithm;

import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.DegreeCentralityWSD;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.WSDResourceDegreeCentrality;
import de.tudarmstadt.ukp.dkpro.wsd.si.SenseTaxonomy;

public class MyWSDResourceDegreeCentrality extends WSDResourceDegreeCentrality {

	@Override
	public void afterResourcesInitialized() throws ResourceInitializationException {
		  wsdAlgorithm = new DegreeCentralityWSD((SenseTaxonomy) inventory);
	        ((DegreeCentralityWSD) wsdAlgorithm).setSearchDepth(Integer
	                .valueOf(searchDepth));
	        ((DegreeCentralityWSD) wsdAlgorithm).setMinDegree(Integer
	                .valueOf(minDegree));
	        ((DegreeCentralityWSD) wsdAlgorithm).setGraphVisualizer(graphVisualizer);
	}

}
