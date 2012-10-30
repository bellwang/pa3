package cs224n.corefsystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

public class BetterBaseline implements CoreferenceSystem {
	private Hashtable< String, HashSet<String> > ht = new Hashtable< String, HashSet<String>>() ;
			
	@Override
	  public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
	    for(Pair<Document, List<Entity>> pair : trainingData){
	      //--Get Variables
	      Document doc = pair.getFirst();
	      List<Entity> clusters = pair.getSecond();
	      List<Mention> mentions = doc.getMentions();
	      //--Print the Document
	     
	      //--Iterate Over Coreferent Mention Pairs
	      for(Entity e : clusters){
	        for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
	        	String head1 = mentionPair.getFirst().headWord();
	        	String head2 = mentionPair.getSecond().headWord();
	        	if( ht.containsKey(head1) ){
	        		ht.get(head1).add(head2);
	        	}
	        	
	        	if( ht.containsKey(head2) ){
	        		ht.get(head2).add(head1);
	        	}	        	
	        }
	      }
	    }
	  }

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
	    //(variables)
	    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
	    	    
	    //(for each mention...)
	    for(Mention m : doc.getMentions()){

	    	String mentionHead = m.headWord();
	    	
	    	boolean flag = false; 
	    	
	    	if(ht.contains(mentionHead)){
	    		for(ClusteredMention cm: mentions){
	    			String head2 = cm.mention.headWord();
	    			
	    			if(ht.get(mentionHead).contains(head2)){
	    				mentions.add(m.markCoreferent(cm.entity));
	    				flag = true;
	    				break;
	    			}	    			
	    		}
	    		//(...get its text)	    		
	    	}
	    	
	    	if(flag == false){
	        //(...else create a new singleton cluster)
	        ClusteredMention newCluster = m.markSingleton();
	        mentions.add(newCluster);
	      }
	    }
	    //(return the mentions)
	    return mentions;
	  }

}
