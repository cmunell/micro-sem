package edu.cmu.ml.rtw.micro.sem.model.annotation.nlp;

import java.util.List;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Language;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;

public class SemparseAnnotatorSentenceTest {
	@Test
	public void testDocument() {
		PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
		PipelineNLPExtendable pipelineExtendable = new PipelineNLPExtendable();
		
		SemparseAnnotatorSentence semanticParser = SemparseAnnotatorSentence.fromSerializedModels(SemparseAnnotatorSentence.PARSER_MODEL_PATH, SemparseAnnotatorSentence.SUPERTAGGER_MODEL_PATH);
		
		pipelineExtendable.extend(new NELLMentionCategorizer());
		pipelineExtendable.extend(semanticParser);
		PipelineNLP pipeline = pipelineStanford.weld(pipelineExtendable);
		DataTools dataTools = new DataTools();
		dataTools.addAnnotationTypeNLP(SemparseAnnotatorSentence.LOGICAL_FORM);
		DocumentNLP document = new DocumentNLPInMemory(dataTools, 
		                                               "Test document", 
		                                               "Barack Obama is the president of the United States. Madonna who was born in Bay City, Michigan. " +
		                                               "Larry Page founded Google. Google was founded by Larry Page and Sergey Brin.",
		                                               Language.English, pipeline);
		List<Annotation> annotations = document.toMicroAnnotation().getAllAnnotations();
		for (Annotation annotation : annotations) {
			System.out.println(annotation.toJsonString());
		}
	}
}
