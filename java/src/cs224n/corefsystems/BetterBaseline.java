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
	     
	      for(Entity e : clusters){
	    	  HashSet<String> heads = new HashSet<String>();
	    	  for(Mention m : e.mentions)
	    		  heads.add(m.headWord());
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
	    //(variables)
	    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
	    	    
	    //(for each mention...)
	    for(Mention m : doc.getMentions()){
	    	String mentionHead = m.headWord();
	    	boolean flag = false; 
	    	for(ClusteredMention cm: mentions){
	    		String head2 = cm.mention.headWord();
	    		//first check whether the two heads are equal
	    		//if so, no need to go through the hash table
	    		if(mentionHead.equals(head2))
	    		{
	    			mentions.add(m.markCoreferent(cm.entity));
    				flag = true;
    				break;
	    		}
	    		
	    		//to check whether the two heads are corefferenced.
	    		if(ht.contains(mentionHead)){
	    			if(ht.get(mentionHead).contains(head2)){
	    				mentions.add(m.markCoreferent(cm.entity));
	    				flag = true;
	    				break;
	    			}	    			
	    		}
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
