package cs224n.corefsystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Gender;
import cs224n.coref.Mention;
import cs224n.coref.Name;
import cs224n.coref.Pronoun;
import cs224n.coref.Sentence;
import cs224n.coref.Util;
import cs224n.ling.Tree;
import cs224n.util.Pair;

public class RuleBased implements CoreferenceSystem {
	private Hashtable< String, HashSet<String> > ht = new Hashtable< String, HashSet<String>>() ;
	private ArrayList<HashSet<String>> pronounGroups = new ArrayList<HashSet<String>>();
	private void constructPronounGroups()
	{
		HashSet<String> hs1 = new HashSet<String>();
		hs1.add("i");hs1.add("me");hs1.add("mine");hs1.add("my");hs1.add("myself");
		HashSet<String> hs2 = new HashSet<String>();
		hs2.add("he");hs2.add("him");hs2.add("himself");hs2.add("his");hs2.add("hisself");
		HashSet<String> hs3 = new HashSet<String>();
		hs3.add("she");hs3.add("her");hs3.add("hers");hs3.add("herself");
		HashSet<String> hs4 = new HashSet<String>();
		hs4.add("it");hs4.add("its");hs4.add("itself");
		HashSet<String> hs5 = new HashSet<String>();
		hs5.add("one");hs5.add("one's");hs5.add("oneself");
		HashSet<String> hs6 = new HashSet<String>();
		hs6.add("our");hs6.add("ours");hs6.add("ourself");hs6.add("ourselves");hs6.add("us");hs6.add("we");hs6.add("all");
		HashSet<String> hs7 = new HashSet<String>();
		hs7.add("their");hs7.add("theirs");hs7.add("theirselves");hs7.add("them");hs7.add("themself");
		hs7.add("themself");hs7.add("themselves");hs7.add("they");hs7.add("thine");hs7.add("thy");hs7.add("thyself");hs7.add("all");
		HashSet<String> hs8 = new HashSet<String>();
		hs8.add("y'all");hs8.add("y'all's");hs8.add("y'all's selves");hs8.add("ye");hs8.add("you");hs8.add("you all");hs8.add("your");
		hs8.add("yours");hs8.add("yourself");hs8.add("yourselves");hs8.add("youse");hs8.add("all");hs8.add("thee");hs8.add("thou");
		pronounGroups.add(hs1);pronounGroups.add(hs2);pronounGroups.add(hs3);pronounGroups.add(hs4);pronounGroups.add(hs5);pronounGroups.add(hs6);pronounGroups.add(hs7);pronounGroups.add(hs8);
	}
	
	private boolean sameGroupPronoun(Mention m1, Mention m2)
	{
		String s1 = m1.headToken().lemma().toLowerCase();
		String s2 = m2.headToken().lemma().toLowerCase();
		for(HashSet<String> hs : pronounGroups)
		{
			if(hs.contains(s1)&&hs.contains(s2))
				return true;
		}
		return false;
	}
	
	
	//use train to collect heads matching table.
	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		//initialize pronoun groups
		constructPronounGroups();
		for(Pair<Document, List<Entity>> pair : trainingData){
			//--Get Variables
			List<Entity> clusters = pair.getSecond();
			for(Entity e : clusters){
				HashSet<String> heads = new HashSet<String>();
				for(Mention m : e.mentions)
					heads.add(m.headToken().lemma().toLowerCase());
				for(String head : heads)
				{
					if(ht.contains(head))
						ht.get(head).addAll(heads);
					else
						ht.put(head, heads);
				}
			}
		}
	}


	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		List<ClusteredMention> clusteredMentions = new ArrayList<ClusteredMention>();
		
		Hobbs(doc,clusteredMentions);
		
		HeadMatch(doc, clusteredMentions);
		CheckSpeaker(doc, clusteredMentions);
		NER(doc, clusteredMentions);
		
		for(Mention m : doc.getMentions())
		{
			if(getEntity(clusteredMentions, m)!=null)
				continue;
			ClusteredMention newCluster = m.markSingleton();
			clusteredMentions.add(newCluster);
		}
		return clusteredMentions;
	}
	
	private void NER(Document doc, List<ClusteredMention> clusteredMentions)
	{
		List<Mention> mentions = doc.getMentions();
		for(int i = 0; i< mentions.size(); i++){
	    	Mention m1 = mentions.get(i);
	    	if(getEntity(clusteredMentions, m1)!=null)
	    		continue;
	    	for(int j = i-1; j>=0; j--){
	    		Mention m2 = mentions.get(j);
				if(m1.sentence.equals(m2.sentence)&&((m1.headToken().nerTag().equals("DATE")
						&&m2.headToken().nerTag().equals("DATE"))))
				{
					Entity e = getEntity(clusteredMentions, m2);
					if(e!=null)
					{
						clusteredMentions.add(m1.markCoreferent(e));
					}
					else
					{
						ClusteredMention newCluster = m2.markSingleton();
						clusteredMentions.add(newCluster);
						clusteredMentions.add(m1.markCoreferent(newCluster.entity));
					}
					break;
				}
			}
		}
	}
	
	private boolean genderCompare(Mention m1, Mention m2)
	{
		Pair<Boolean, Boolean> r = Util.haveGenderAndAreSameGender(m1, m2);
		if(!r.getFirst())
			return true;
		if(!r.getSecond())
			return false;
		else
			return true;
	}
	
	private void CheckSpeaker(Document doc, List<ClusteredMention> clusteredMentions)
	{
		List<Mention> mentions = doc.getMentions();
		for(int i = 0; i< mentions.size(); i++){
	    	Mention m1 = mentions.get(i);
	    	if(getEntity(clusteredMentions, m1)!=null)
	    		continue;
	    	for(int j = i-1; j>=0; j--){
	    		Mention m2 = mentions.get(j);
	    		if(SpeakerMatch(m1,m2))
				{
					Entity e = getEntity(clusteredMentions, m2);
					if(e!=null)
					{
						clusteredMentions.add(m1.markCoreferent(e));
					}
					else
					{
						ClusteredMention newCluster = m2.markSingleton();
						clusteredMentions.add(newCluster);
						clusteredMentions.add(m1.markCoreferent(newCluster.entity));
					}
					break;
				}
	    	}
		}
	}
	
	private boolean SpeakerMatch(Mention m1, Mention m2)
	{
		if(!m1.headToken().isQuoted())
			return false;

		if(!m2.headToken().word().equalsIgnoreCase(m1.headToken().speaker()))
			return false;
		
		int genderType = 0; 
		if(Pronoun.isSomePronoun(m1.headToken().lemma().toLowerCase()))
		{
			genderType = whichPronounGroup(m1.headToken().lemma().toLowerCase());
			if(genderType<0)
				return false;
		}
		else 
			return false;
		String head2 = m2.headToken().lemma().toLowerCase();
		Gender g = Name.mostLikelyGender(head2);
		if(g == Gender.NEUTRAL && genderType == 6 )
			return true;
		else if(g == Gender.EITHER)
			return true;
		else if(g == Gender.MALE && genderType == 1)
			return true;
		else if(g == Gender.FEMALE && genderType == 2)
			return true;
		return false;
	}
	
	private int whichPronounGroup(String pronoun)
	{
		int r=-1;
		for(int i =0;i<pronounGroups.size();i++)
		{
			if(pronounGroups.get(i).contains(pronoun))
			{
				r = i;
				break;
			}
		}
		return r;
	}
	
	private boolean checkAdj(Mention m1, Mention m2)
	{
		if(m1.headToken().posTag().equals("NN") && m2.headToken().posTag().equals("NN"))
		{
			if(m1.parse.getPreTerminalYield().toString().equals(m2.parse.getPreTerminalYield().toString()))
			{
				Sentence.Token adj1 = null, adj2 = null;
				if(m1.headWordIndex > m1.beginIndexInclusive)
					adj1 = m1.sentence.tokens.get(m1.headWordIndex-1);
				if(m2.headWordIndex > m2.beginIndexInclusive)
					adj2 = m2.sentence.tokens.get(m2.headWordIndex-1);
				if(adj1!=null&&adj2!=null)
				{
					if(adj1.posTag().equals("JJ")&&adj2.posTag().equals("JJ")
							&&!adj1.word().toLowerCase().equals(adj2.word().toLowerCase()))
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void HeadMatch(Document doc, List<ClusteredMention> clusteredMentions)
	{	    
	    //(for each mention...)
		List<Mention> mentions = doc.getMentions();
	    for(int i = 0; i< mentions.size(); i++){
	    	Mention m1 = mentions.get(i);
//	    	if(!m1.headToken().nerTag().equals("O"))
//	    	System.out.println(m1.headWord()+" "+m1.headToken().nerTag());
	    	if(getEntity(clusteredMentions, m1)!=null)
	    		continue;
	    	String head1 = m1.headToken().lemma().toLowerCase();
	    	for(int j = i-1; j>=0; j--){
	    		Mention m2 = mentions.get(j);
	    		String head2 = m2.headToken().lemma().toLowerCase();
	    		//first check whether the two heads are equal
	    		//if so, no need to go through the hash table
	    		boolean isCorf = false;
	    		if(head1.equals(head2))
	    			isCorf = true;
	    		
	    		//to check whether the two heads are corefferenced.
	    		else if(m1.headToken().nerTag().equals(m2.headToken().nerTag())&&
	    				ht.contains(head1)&&ht.get(head1).contains(head2)){
	    				isCorf = true;	   
	    		}

	    		if(isCorf&&!checkAdj(m1,m2))
	    		{
	    			isCorf = false;
	    		}
	    		if(Pronoun.isSomePronoun(head1)&&Pronoun.isSomePronoun(head2))
	    		{
	    			if(sameGroupPronoun(m1,m2))
	    				isCorf = true;
	    		}
	    		if(isCorf && m1.headToken().isNoun() && m2.headToken().isNoun()
	    				&& m1.headToken().isPluralNoun()!=m2.headToken().isPluralNoun())
	    			isCorf = false;
	    		
	    		

	    		if(isCorf)
	    		{
//	    			System.out.println("m1: "+m1.sentence.toString());
//	    			System.out.println("m2: "+m2.sentence.toString());
//	    			System.out.println(m1.toString()+" :: "+m2.toString());
//	    			System.out.println(m1.parse.getPreTerminalYield().toString());
//	    			System.out.println(m2.parse.getPreTerminalYield().toString());
	    			Entity e2 = getEntity(clusteredMentions, m2);
	    			if(e2!=null)
	    			{
	    				clusteredMentions.add(m1.markCoreferent(e2));
	    			}
	    			else
	    			{
	    				ClusteredMention newCluster = m2.markSingleton();
	    				clusteredMentions.add(newCluster);
	    				clusteredMentions.add(m1.markCoreferent(newCluster.entity));
	    			}
	    			break;
	    		}
//	    		System.out.println(m1.toString()+" :: "+m2.toString());
	    	}
	      }
	}
	
	private void Hobbs(Document doc, List<ClusteredMention> clusteredMentions)
	{	
		List<Mention> mentions = doc.getMentions();
	    for(int i = 0; i< mentions.size(); i++){
	    	Mention m1 = mentions.get(i);
			if(getEntity(clusteredMentions, m1)!=null)
				continue;
			String head = m1.headToken().lemma().toLowerCase();
			if(Pronoun.isSomePronoun(head))
			{
				ArrayList<List<String>> proposed = Hobbs(m1);
				for(Mention m2 : mentions)
				{
					if(m1.equals(m2)||!m1.sentence.equals(m2.sentence))
						continue;

					String head2 = m2.headToken().lemma().toLowerCase();
					boolean isCorf = false;
					for(List<String> slist : proposed)
					{
						if(slist.contains(m2.headWord()))
						{
							String pre = null, pos = null;
							if(m2.headWordIndex>0)
								pre = m2.sentence.words.get(m2.headWordIndex-1);
							if(m2.headWordIndex<m2.sentence.words.size())
								pos = m2.sentence.words.get(m2.headWordIndex+1);
							if(!(slist.contains(pre)||slist.contains(pos)))
								continue;
						
							if(m1.headToken().nerTag().equals(m2.headToken().nerTag()))
							{
								isCorf = true;
								
							}
							if(Pronoun.isSomePronoun(head2))
							{
								if(!sameGroupPronoun(m1,m2))
								{
									isCorf = false;
								}
							}
							//if m2 is not a pronoun
							if(!Pronoun.isSomePronoun(head2)&&m2.headToken().isNoun())
							{
								isCorf = true;
								int pronounType = whichPronounGroup(head);
								//if m1 is I, we, ...
								if((pronounType==0 || pronounType==7) && !m2.headToken().nerTag().equals("PERSON"))
									isCorf = false;
								//if m1 is he, his, ..
								else if(pronounType==1 && !(m2.headToken().nerTag().equals("PERSON") 
										&& (Name.mostLikelyGender(m2.headToken().lemma().toLowerCase()) == Gender.MALE)))
									isCorf = false;
								else if(pronounType==2 && !(m2.headToken().nerTag().equals("PERSON")
										&& Name.mostLikelyGender(m2.headToken().lemma().toLowerCase()) == Gender.FEMALE))
									isCorf = false;
								else if((pronounType==5||pronounType==6||head2.contains("all")) && !m2.headToken().isPluralNoun())
									isCorf = false;
							}
							if(!genderCompare(m1,m2))
								isCorf = false;	

							if(isCorf)
								break;
						}
						
					}
					if(isCorf)
					{
//						System.out.println(m1.toString()+" ---> "+m2.toString());
						Entity e = getEntity(clusteredMentions, m2);
						if(e!=null)
						{
							clusteredMentions.add(m1.markCoreferent(e));
						}
						else
						{
							ClusteredMention newCluster = m2.markSingleton();
		    				clusteredMentions.add(newCluster);
		    				clusteredMentions.add(m1.markCoreferent(newCluster.entity));
						}
						break;
					}
				}
			}
		}
	}
	
	private Entity getEntity(List<ClusteredMention> mentions, Mention m)
	{
		for(ClusteredMention cm : mentions)
		{
			if(cm.mention.equals(m))
				return cm.entity;
		}
		return null;
	}




	
	private ArrayList<List<String>> Hobbs(Mention m)
	{
		ArrayList<List<String>> proposed = new ArrayList<List<String>>();
		Sentence s = m.sentence;
		Tree<String> tree = s.parse;
		int index = m.headWordIndex;
		ArrayList<Tree<String>> path = new ArrayList<Tree<String>>();
		pathToIndex(tree, index, path);
		//the last one (size - 1) is the word, so its parent is size - 2
		int position = path.size()-2;
		
		//Step 1, begin at NP
		position = Step1(path, position);
		//Step 2, go up to first NP or S
		position = Step2(path, position);
		//Step 3, use BFS to find NP or S
		if(position >=1 )
			proposed.addAll(Step3(path, position));
		//Step 4 to 9
		if(position >=1 )
			proposed.addAll(Step4_9(path, position));
		return proposed;
	}
	
	private int Step1(ArrayList<Tree<String>> path, int position)
	{
		Tree<String> currNode;
		for(; position >=0; position --)
		{
			currNode = path.get(position);
			if(currNode.getLabel().equals("NP"))
				break;
		}
		return position;
	}
	
	private int Step2(ArrayList<Tree<String>> path, int position)
	{
		Tree<String> currNode;
		position --;
		for(; position >=0; position --)
		{
			currNode = path.get(position);
			if(currNode.getLabel().equals("NP") || currNode.getLabel().equals("S"))
			{
				return position;
			}
		}
		return position;
	}
	
	private ArrayList<List<String>> Step3(ArrayList<Tree<String>> path, int position)
	{
		return BFS_NP_S(path, position);
	}
	
	private ArrayList<List<String>> BFS_NP_S(ArrayList<Tree<String>> path, int position)
	{
		ArrayList<List<String>> result = new ArrayList<List<String>>();
		Tree<String> X = path.get(position);
		Tree<String> child = path.get(position + 1);
	
		LinkedList<Pair<Tree<String>, Boolean>> queue = new LinkedList<Pair<Tree<String>, Boolean>>();
		for(Tree<String> c : X.getChildren())
		{
			if(c.equals(child))
				break;
			//there is no NP or S between X and its children, set false
			queue.addLast(new Pair<Tree<String>, Boolean>(c, false));
		}
		while(!queue.isEmpty())
		{
			Pair<Tree<String>, Boolean> pair = queue.removeFirst();
			Tree<String> node = pair.getFirst();
			boolean flag = pair.getSecond();
			String label = node.getLabel();
			if(flag && label.equals("NP"))
			{
				result.add(node.getYield());
			}
			else
			{
				boolean currFlag;
				if(flag || label.equals("NP") || label.equals("S"))
					currFlag = true;
				else
					currFlag = false;
				for(Tree<String> c : node.getChildren())
				{
					queue.addLast(new Pair<Tree<String>, Boolean>(c, currFlag));
				}
			}
		}
		return result;
	}
	
	private ArrayList<List<String>> Step4_9(ArrayList<Tree<String>> path, int position)
	{
		ArrayList<List<String>> result = new ArrayList<List<String>>();
		if(position == 1)
			return BFS_NP(path, position);
		else
			//Step 5
			position = Step2(path, position);
		if(position <= 0)
			return result;
		//Step 6
		Tree<String> X = path.get(position);
		if(X.getLabel().equals("NP"))
		{
			Tree<String> child = path.get(position + 1);
			if(!child.getLabel().startsWith("N"))
				result.add(X.getYield());
		}
		//Step 7
		result.addAll(BFS_NP(path, position));
	
		//Step 8
		result.addAll(BFS_NP_S_Right(path, position));
		//Step 9
		result.addAll(Step4_9(path, position));
		return result;
	}
	
	private ArrayList<Tree<String>> getAllLeaf(Tree<String> X)
	{
		ArrayList<Tree<String>> leafnodes = new ArrayList<Tree<String>>();
		LinkedList<Tree<String>> queue = new LinkedList<Tree<String>>();
		for(Tree<String> c : X.getChildren())
		{
			queue.addLast(c);
		}
		while(!queue.isEmpty())
		{
			Tree<String> node = queue.removeFirst();
			if(node.isLeaf())
			{
				leafnodes.add(node);
			}
			else
			{
				for(Tree<String> c : node.getChildren())
				{
					queue.addLast(c);
				}
			}
		}
		return leafnodes;
	}
	
	private ArrayList<List<String>> BFS_NP(ArrayList<Tree<String>> path, int position)
	{
		ArrayList<List<String>> result = new ArrayList<List<String>>();
		Tree<String> X = path.get(position);
		Tree<String> child = path.get(position + 1);
	
		LinkedList<Tree<String>> queue = new LinkedList<Tree<String>>();
		for(Tree<String> c : X.getChildren())
		{
			if(c.equals(child))
				break;
			queue.addLast(c);
		}
		while(!queue.isEmpty())
		{
			Tree<String> node = queue.removeFirst();
			String label = node.getLabel();
			if(label.equals("NP"))
			{
				result.add(node.getYield());
			}
			else
			{
				for(Tree<String> c : node.getChildren())
				{
					queue.addLast(c);
				}
			}
		}
		return result;
	}
	
	private ArrayList<List<String>> BFS_NP_S_Right(ArrayList<Tree<String>> path, int position)
	{
		ArrayList<List<String>> result = new ArrayList<List<String>>();
		Tree<String> X = path.get(position);
		Tree<String> child = path.get(position + 1);
	
		LinkedList<Tree<String>> queue = new LinkedList<Tree<String>>();
		boolean find = false;
		for(Tree<String> c : X.getChildren())
		{
			//begin from the node to the right of the path
			if(!find)
			{
				if(c.equals(child))
				{
					find = true;
				}
				continue;
			}
			queue.addLast(c);
		}
		while(!queue.isEmpty())
		{
			Tree<String> node = queue.removeFirst();
			String label = node.getLabel();
			if(label.equals("NP") || label.equals("S"))
			{
				result.add(node.getYield());
			}
			else
			{
				for(Tree<String> c : node.getChildren())
				{
					queue.addLast(c);
				}
			}
		}
		return result;
	}
	
	private void pathToIndex(Tree<String> tree, int index, ArrayList<Tree<String>> path)
	{
		//--Base Case
		if(tree.isLeaf()){
			path.add(tree);
			return;
		}
		//--Recursive Case
		//(get children)
		List<Tree<String>> children = tree.getChildren();
		//(get child with relevant span)
		int yieldSoFar = 0;
		int childIndex = 0;
		int lastYield = 0;

		while(yieldSoFar <= index){
			lastYield = children.get(childIndex).getYield().size();
			yieldSoFar += lastYield;
			childIndex += 1;
		}
		//(move back one step)
		childIndex -= 1;
		yieldSoFar -= lastYield;
		//(get rest of path)
		path.add(tree);
		pathToIndex(children.get(childIndex), index - yieldSoFar, path);	
	}

}
