package cs224n.corefsystems;

import cs224n.coref.*;
import cs224n.coref.Sentence.Token;
import cs224n.util.Pair;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import edu.stanford.nlp.util.logging.StanfordRedwoodConfiguration;

import java.text.DecimalFormat;
import java.util.*;

import static edu.stanford.nlp.util.logging.Redwood.Util.*;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class ClassifierBased implements CoreferenceSystem {

	private static <E> Set<E> mkSet(E[] array){
		Set<E> rtn = new HashSet<E>();
		Collections.addAll(rtn, array);
		return rtn;
	}

	private static final Set<Object> ACTIVE_FEATURES = mkSet(new Object[]{

			/*
			 * TODO: Create a set of active features
			 */

			//Feature.ExactMatch.class,
			
			//Feature.HeaderLemmaMatch.class,

			Feature.MentionCoref.class,
			
			//Feature.MentionDist.class,
			
			//Feature.SentenceDist.class,
			
			Feature.Pronoun.class,
			
			//Feature.ParsePath.class,
			
			Feature.NER.class,
			
			/*
			 * Additional Features
			 */
			Feature.DefNP.class,
			
			Feature.DemNP.class,
			
			Feature.NumAgreement.class,
			
			//Feature.GenderAgreement.class,
			
			Feature.ProperNoun.class,
			
			//skeleton for how to create a pair feature
			//Pair.make(Feature.IsFeature1.class, Feature.IsFeature2.class),
			//Pair.make(Feature.MentionCoref.class, Feature.Pronoun.class),
	});


	protected static final String PonPrix = null;


	private LinearClassifier<Boolean,Feature> classifier;

	public ClassifierBased(){
		StanfordRedwoodConfiguration.setup();
		RedwoodConfiguration.current().collapseApproximate().apply();
	}

	public FeatureExtractor<Pair<Mention,ClusteredMention>,Feature,Boolean> extractor = new FeatureExtractor<Pair<Mention, ClusteredMention>, Feature, Boolean>() {
		private <E> Feature feature(Class<E> clazz, Pair<Mention,ClusteredMention> input, Option<Double> count){
			
			//--Variables
			Mention onPrix = input.getFirst(); //the first mention (referred to as m_i in the handout)
			Mention candidate = input.getSecond().mention; //the second mention (referred to as m_j in the handout)
			Entity candidateCluster = input.getSecond().entity; //the cluster containing the second mention

			//--Features
			if(clazz.equals(Feature.ExactMatch.class)){
				return new Feature.ExactMatch(onPrix.gloss().equals(candidate.gloss()));	
				
			} else if(clazz.equals(Feature.HeaderLemmaMatch.class)) {
				return new Feature.HeaderLemmaMatch(headMatchFnc(onPrix, candidate));

			} else if(clazz.equals(Feature.MentionCoref.class)) {
				return new Feature.MentionCoref(isCoref(onPrix, candidate, candidateCluster));
				
			}else if(clazz.equals(Feature.MentionDist.class)) {
				return new Feature.MentionDist(mentionDistance(onPrix,candidate));

			} else if(clazz.equals(Feature.SentenceDist.class)) {
				return new Feature.SentenceDist(sentenceDistance(onPrix,candidate));				
			
			} else if(clazz.equals(Feature.Pronoun.class)) {
				return new Feature.Pronoun(IsPronoun(onPrix, candidate));
			 					
			} else if(clazz.equals(Feature.NER.class)) {
				//return new Feature.NER(IsNER(onPrix, candidate));
				return new Feature.NER(candidate.headToken().nerTag());
				
			} else if(clazz.equals(Feature.ParsePath.class)) {
				return new Feature.ParsePath(getPath(onPrix, candidate));
				
			//Additional Features
			} else if(clazz.equals(Feature.DefNP.class)) {
				return new Feature.DefNP(IsDefNP(onPrix));
				
			} else if(clazz.equals(Feature.DemNP.class)) {
				return new Feature.DemNP(IsDemNP(onPrix));
				
			} else if(clazz.equals(Feature.NumAgreement.class)) {
				return new Feature.NumAgreement(agreeNum(onPrix, candidate));
				
			} else if(clazz.equals(Feature.GenderAgreement.class)) {
				return new Feature.GenderAgreement(genderAgree(onPrix, candidate));
				
			} else if(clazz.equals(Feature.ProperNoun.class)) {
				return new Feature.ProperNoun(isBothPN(onPrix, candidate));
				
			//} else if(clazz.equals(Feature.NewFeature.class)) {
			//TODO: Add features to return for specific classes. Implement calculating values of features here.				

			//} else if(clazz.equals(Feature.NewFeature.class)) {
			//TODO: Add features to return for specific classes. Implement calculating values of features here.				
				
			}else {
				throw new IllegalArgumentException("Unregistered feature: " + clazz);
			}
		}

		/*
		 * Helper Methods 
		 */
		// Return if two mention have similar content
		private boolean headMatchFnc(Mention onPrix, Mention candidate) {
			//return new Feature.HeaderMatch(onPrix.headWord().equals(candidate.headWord()));			
			return onPrix.headToken().lemma().equalsIgnoreCase(candidate.headToken().lemma());
		}

		// Return if onPrix mention belongs to the candidateCluster
		private boolean isCoref(Mention onPrix, Mention candidate, Entity candidateCluster) {
			return headMatchFnc(onPrix, candidate)  || candidateCluster.mentions.contains(onPrix);
		}
		
		// Return the distance(# of mentions) of two mentions in the same document		
		private int sentenceDistance(Mention onPrix, Mention candidate) {
			Document locatedDoc = onPrix.doc;
			assert(locatedDoc.equals(candidate.doc));
			
			List<Sentence> sentences = locatedDoc.sentences;
			Sentence onPrix_sentence = onPrix.sentence;
			Sentence candidate_sentence = candidate.sentence;
			
			int onPrix_pos = -1;
			int candidate_pos = -1;
			for(int i = 0; i< sentences.size(); i++){
				if(onPrix_sentence.equals(sentences.get(i))) onPrix_pos = i;//TODO: check if use "equals" for sentence comparision
				if(candidate_sentence.equals(sentences.get(i))) candidate_pos = i;
				if(onPrix_pos != -1 && candidate_pos != -1) break;
			}
			assert(onPrix_pos != -1 && candidate_pos != -1);
			//return (onPrix_pos - candidate_pos);
			return Math.abs( onPrix.doc.indexOfSentence(onPrix.sentence) - onPrix.doc.indexOfSentence(candidate.sentence) );
		}

		// Return the distance(# of mentions) of two mentions in the same document
		private int mentionDistance(Mention onPrix, Mention candidate) {
			Document locatedDoc = onPrix.doc;
			assert(locatedDoc.equals(candidate.doc));
			
			List<Mention> mentions = locatedDoc.getMentions();
			int onPrix_pos = -1;
			int candidate_pos = -1;
			for(int i = 0; i< mentions.size(); i++){
				if(onPrix.equals(mentions.get(i))) onPrix_pos = i;
				if(candidate.equals(mentions.get(i))) candidate_pos = i;
				if(onPrix_pos != -1 && candidate_pos != -1) break;
			}
			assert(onPrix_pos != -1 && candidate_pos != -1);
			//return (onPrix_pos - candidate_pos);
			return Math.abs( onPrix.doc.indexOfMention(onPrix) - onPrix.doc.indexOfMention(candidate) );
		}

		// Return if any one of the two mentions is a pronoun
		private boolean IsPronoun(Mention onPrix, Mention candidate) {
			return Pronoun.isSomePronoun(onPrix.gloss()) || Pronoun.isSomePronoun(candidate.gloss());
		}

		// TODO:
		private boolean IsNER(Mention onPrix, Mention candidate) {
			return onPrix.headToken().nerTag().equals(candidate.headToken().nerTag());
		}

		// Return the path between onPrix and candidate mentions
		private String getPath(Mention onPrix, Mention candidate) {
			if( !onPrix.sentence.equals(candidate.sentence) )
				return "";
			
			String rnt_path = new String();
			rnt_path += onPrix.sentence.parse.pathToIndex(onPrix.headWordIndex);
			rnt_path += candidate.sentence.parse.pathToIndex(onPrix.headWordIndex);
			return rnt_path;
		}
		
		private boolean IsDemNP(Mention onPrix) {
			return onPrix.headToken().isNoun() && ( onPrix.gloss().startsWith("this") || onPrix.gloss().startsWith("This") || onPrix.gloss().startsWith("that") || onPrix.gloss().startsWith("That") || onPrix.gloss().startsWith("those") || onPrix.gloss().startsWith("Those") || onPrix.gloss().startsWith("these") || onPrix.gloss().startsWith("These"));
		}
		
		private boolean IsDefNP(Mention onPrix) {
			return onPrix.headToken().isNoun() && ( onPrix.gloss().startsWith("the") || onPrix.gloss().startsWith("The") );
		}

		private boolean agreeNum(Mention onPrix, Mention candidate) {
			return (onPrix.headToken().isPluralNoun() && candidate.headToken().isPluralNoun()) || (candidate.headToken().isPluralNoun() && candidate.headToken().isPluralNoun());
		}
		
		private boolean genderAgree(Mention onPrix, Mention candidate) {
			return Name.mostLikelyGender(onPrix.headWord()).isCompatible(Name.mostLikelyGender(candidate.headWord()));
		}
		
		private boolean isBothPN(Mention onPrix, Mention candidate) {
			return onPrix.headToken().isProperNoun() && candidate.headToken().isProperNoun();
		}		

		@SuppressWarnings({"unchecked"})
		@Override
		protected void fillFeatures(Pair<Mention, ClusteredMention> input, Counter<Feature> inFeatures, Boolean output, Counter<Feature> outFeatures) {
			//--Input Features
			for(Object o : ACTIVE_FEATURES){
				if(o instanceof Class){
					//(case: singleton feature)
					Option<Double> count = new Option<Double>(1.0);
					Feature feat = feature((Class) o, input, count);
					if(count.get() > 0.0){
						inFeatures.incrementCount(feat, count.get());
					}
				} else if(o instanceof Pair){
					//(case: pair of features)
					Pair<Class,Class> pair = (Pair<Class,Class>) o;
					Option<Double> countA = new Option<Double>(1.0);
					Option<Double> countB = new Option<Double>(1.0);
					Feature featA = feature(pair.getFirst(), input, countA);
					Feature featB = feature(pair.getSecond(), input, countB);
					if(countA.get() * countB.get() > 0.0){
						inFeatures.incrementCount(new Feature.PairFeature(featA, featB), countA.get() * countB.get());
					}
				}
			}

			//--Output Features
			if(output != null){
				outFeatures.incrementCount(new Feature.CoreferentIndicator(output), 1.0);
			}
		}

		@Override
		protected Feature concat(Feature a, Feature b) {
			return new Feature.PairFeature(a,b);
		}
	};

	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		startTrack("Training");
		//--Variables
		RVFDataset<Boolean, Feature> dataset = new RVFDataset<Boolean, Feature>();
		LinearClassifierFactory<Boolean, Feature> fact = new LinearClassifierFactory<Boolean,Feature>();
		//--Feature Extraction
		startTrack("Feature Extraction");
		for(Pair<Document,List<Entity>> datum : trainingData){
			//(document variables)
			Document doc = datum.getFirst();
			List<Entity> goldClusters = datum.getSecond();
			List<Mention> mentions = doc.getMentions();
			Map<Mention,Entity> goldEntities = Entity.mentionToEntityMap(goldClusters);
			startTrack("Document " + doc.id);
			//(for each mention...)
			for(int i=0; i<mentions.size(); i++){
				//(get the mention and its cluster)
				Mention onPrix = mentions.get(i);
				Entity source = goldEntities.get(onPrix);
				if(source == null){ throw new IllegalArgumentException("Mention has no gold entity: " + onPrix); }
				//(for each previous mention...)
				int oldSize = dataset.size();
				for(int j=i-1; j>=0; j--){
					//(get previous mention and its cluster)
					Mention cand = mentions.get(j);
					Entity target = goldEntities.get(cand);
					if(target == null){ throw new IllegalArgumentException("Mention has no gold entity: " + cand); }
					//(extract features)
					Counter<Feature> feats = extractor.extractFeatures(Pair.make(onPrix, cand.markCoreferent(target)));
					//(add datum)
					dataset.add(new RVFDatum<Boolean, Feature>(feats, target == source));
					//(stop if
					if(target == source){ break; }
				}
				//logf("Mention %s (%d datums)", onPrix.toString(), dataset.size() - oldSize);
			}
			endTrack("Document " + doc.id);
		}
		endTrack("Feature Extraction");
		//--Train Classifier
		startTrack("Minimizer");
		this.classifier = fact.trainClassifier(dataset);
		endTrack("Minimizer");
		//--Dump Weights
		startTrack("Features");
		//(get labels to print)
		Set<Boolean> labels = new HashSet<Boolean>();
		labels.add(true);
		//(print features)
		for(Triple<Feature,Boolean,Double> featureInfo : this.classifier.getTopFeatures(labels, 0.0, true, 100, true)){
			Feature feature = featureInfo.first();
			Boolean label = featureInfo.second();
			Double magnitude = featureInfo.third();
			//log(FORCE,new DecimalFormat("0.000").format(magnitude) + " [" + label + "] " + feature);
		}
		end_Track("Features");
		endTrack("Training");
	}

	public List<ClusteredMention> runCoreference(Document doc) {
		//--Overhead
		startTrack("Testing " + doc.id);
		//(variables)
		List<ClusteredMention> rtn = new ArrayList<ClusteredMention>(doc.getMentions().size());
		List<Mention> mentions = doc.getMentions();
		int singletons = 0;
		//--Run Classifier
		for(int i=0; i<mentions.size(); i++){
			//(variables)
			Mention onPrix = mentions.get(i);
			int coreferentWith = -1;
			//(get mention it is coreferent with)
			for(int j=i-1; j>=0; j--){
				ClusteredMention cand = rtn.get(j);
				boolean coreferent = classifier.classOf(new RVFDatum<Boolean, Feature>(extractor.extractFeatures(Pair.make(onPrix, cand))));
				if(coreferent){
					coreferentWith = j;
					break;
				}
			}
			//(mark coreference)
			if(coreferentWith < 0){
				singletons += 1;
				rtn.add(onPrix.markSingleton());
			} else {
				//log("Mention " + onPrix + " coreferent with " + mentions.get(coreferentWith));
				rtn.add(onPrix.markCoreferent(rtn.get(coreferentWith)));
			}
		}
		//log("" + singletons + " singletons");
		//--Return
		endTrack("Testing " + doc.id);
		return rtn;
	}

	private class Option<T> {
		private T obj;
		public Option(T obj){ this.obj = obj; }
		public Option(){};
		public T get(){ return obj; }
		public void set(T obj){ this.obj = obj; }
		public boolean exists(){ return obj != null; }
	}
}