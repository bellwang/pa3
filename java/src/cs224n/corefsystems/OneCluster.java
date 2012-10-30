package cs224n.corefsystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

public class OneCluster implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		//(variables)
	    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
	    Map<String,Entity> clusters = new HashMap<String,Entity>();
	    Entity entity = null;
	    //(for each mention...)
	    for(int i = 0;i < doc.getMentions().size(); i++)
	    {
	    	Mention m = doc.getMentions().get(i);
	    	if(i == 0)
	    	{
	    		ClusteredMention newCluster = m.markSingleton();
		        mentions.add(newCluster);
		        entity = newCluster.entity;
	    	}
	    	else
	    		mentions.add(m.markCoreferent(entity));
	    }
	    //(return the mentions)
	    return mentions;
	}

}
