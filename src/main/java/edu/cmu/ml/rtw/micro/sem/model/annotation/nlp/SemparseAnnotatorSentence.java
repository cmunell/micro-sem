package edu.cmu.ml.rtw.micro.sem.model.annotation.nlp;

import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.CcgParse.SpannedExpression;
import com.jayantkrish.jklol.ccg.supertag.ListSupertaggedSentence;
import com.jayantkrish.jklol.ccg.supertag.SupertaggedSentence;
import com.jayantkrish.jklol.ccg.supertag.Supertagger;
import com.jayantkrish.jklol.util.IoUtils;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;
import edu.cmu.ml.rtw.users.jayantk.ccg.CcgParseUtils;
import edu.cmu.ml.rtw.users.jayantk.ccg.CcgParseUtils.MentionRelationInstance;
import edu.cmu.ml.rtw.users.jayantk.ccg.MentionCcgParser;
import edu.cmu.ml.rtw.users.jayantk.ccg.MentionTaggedSentence;
import edu.cmu.ml.rtw.users.jayantk.ccg.SupertaggingMentionCcgParser;
import edu.cmu.ml.rtw.users.jayantk.ccg.TypedMention;

public class SemparseAnnotatorSentence implements AnnotatorTokenSpan<String> {

	public static final AnnotationTypeNLP<String> LOGICAL_FORM = 
			new AnnotationTypeNLP<String>("nell-sem", String.class, Target.TOKEN_SPAN);

	public static final String PARSER_MODEL_PATH="src/main/resources/models/parser.ser";
	public static final String SUPERTAGGER_MODEL_PATH="src/main/resources/models/supertagger.ser";
  
	private final SupertaggingMentionCcgParser parser;

	public SemparseAnnotatorSentence(SupertaggingMentionCcgParser parser) {
		this.parser = Preconditions.checkNotNull(parser);
	}
  
	public static SemparseAnnotatorSentence fromSerializedModels(String parserFilename, String supertaggerFilename) {
		// Read class parameters.                                                                                                            
		double[] multitagThresholds = new double[] {0.01, 0.001};

		long maxParseTimeMillis = -1;                                                
		int maxChartSize = Integer.MAX_VALUE;                                            

		Supertagger supertagger = IoUtils.readSerializedObject(supertaggerFilename, Supertagger.class);                                      
		MentionCcgParser semanticParser = IoUtils.readSerializedObject(parserFilename,                                                       
		    MentionCcgParser.class);                                                                                                         

		SupertaggingMentionCcgParser parser = new SupertaggingMentionCcgParser(semanticParser, -1,
		    maxParseTimeMillis, maxChartSize, false, null, supertagger, multitagThresholds);
		
		return new SemparseAnnotatorSentence(parser);
	}
  
	@Override
	public String getName() { return "cmunell_sem-0.0.1"; }

	@Override
	public AnnotationType<String> produces() { return LOGICAL_FORM; };

	@Override
	public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLPCat.NELL_CATEGORY, 
			AnnotationTypeNLP.POS }; }

	@Override
	public boolean measuresConfidence() { return true; }

	@Override
	public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
		List<Triple<TokenSpan, String, Double>> annotations = Lists.newArrayList();
		for (int i = 0; i < document.getSentenceCount(); i++) {
			List<String> tokens = document.getSentenceTokenStrs(i);
			List<PoSTag> posTags = document.getSentencePoSTags(i);
			List<String> pos = Lists.newArrayList();
			for (PoSTag posTag : posTags) {
				pos.add(posTag.name());
			}

			List<TypedMention> typedMentions = Lists.newArrayList();
			for (Pair<TokenSpan, String> catAnnotation : document.getTokenSpanAnnotations(AnnotationTypeNLPCat.NELL_CATEGORY, i)) {
				int startIndex = catAnnotation.getFirst().getStartTokenIndex();
				int endIndex = catAnnotation.getFirst().getEndTokenIndex();
				List<String> mentionTokens = Lists.newArrayList();
				for (int j = startIndex; j < endIndex; j++) {
					mentionTokens.add(document.getTokenStr(i, j));
				}
				
				String mentionString = Joiner.on(" ").join(mentionTokens);
				Set<String> categories = Sets.newHashSet("concept:" + catAnnotation.getSecond());

				typedMentions.add(new TypedMention(mentionString, categories, startIndex + 1, endIndex));
			}

			SupertaggedSentence taggedSentence = ListSupertaggedSentence.createWithUnobservedSupertags(                                                
					tokens, pos);
			MentionTaggedSentence mentionSentence = new MentionTaggedSentence(taggedSentence, null, typedMentions);

			CcgParse parse = parser.parse(mentionSentence, null);
			if (parse != null) {
				for (SpannedExpression spannedExpression : parse.getSpannedLogicalForms(true)) {
					Set<MentionRelationInstance> instances = CcgParseUtils.getEntailedRelationInstances(
							spannedExpression.getExpression());
      
					if (instances.size() > 0) {
						TokenSpan span = new TokenSpan(document, i, spannedExpression.getSpanStart(),
								spannedExpression.getSpanEnd() + 1);
        
						annotations.add(new Triple<TokenSpan, String, Double>(span,
								spannedExpression.getExpression().toString(), 1.0));
					}
				}
			}
		}

		return annotations;
	}
}
