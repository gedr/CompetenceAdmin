package minos.data.services;

import java.util.List;
import java.util.Map;

public class ProfilePatternSummary {
	private List<Integer> basisPosts = null;
	private Map<Integer, List<Integer>> fasets = null;
	
	public ProfilePatternSummary() { }
	
	public ProfilePatternSummary( List<Integer> basisPosts, 
			Map<Integer, List<Integer>> fasets ) { 
		this.basisPosts = basisPosts;
		this.fasets = fasets;
	}

	public List<Integer> getBasisPosts() {
		return basisPosts;
	}

	public void setBasisPosts( List<Integer> basisPosts ) {
		this.basisPosts = basisPosts;
	}

	public Map<Integer, List<Integer>> getFasets() {
		return fasets;
	}

	public void setFasets( Map<Integer, List<Integer>> fasets ) {
		this.fasets = fasets;
	}
}