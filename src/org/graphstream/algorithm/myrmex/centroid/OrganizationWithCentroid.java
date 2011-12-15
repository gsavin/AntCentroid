package org.graphstream.algorithm.myrmex.centroid;

import java.util.HashMap;

import org.graphstream.organic.Organization;
import org.graphstream.organic.OrganizationListener;
import org.graphstream.organic.OrganizationsGraph;

public class OrganizationWithCentroid implements OrganizationListener {
	OrganizationsGraph metaGraph;
	HashMap<Object, AntCentroidAlgorithm> algorithms;
	AntCentroidParams params;
	
	public void connectionCreated(Object metaIndex1,
			Object metaOrganizationIndex1, Object metaIndex2,
			Object metaOrganizationIndex2, String connection) {
		// TODO Auto-generated method stub
		
	}

	public void connectionRemoved(Object metaIndex1,
			Object metaOrganizationIndex1, Object metaIndex2,
			Object metaOrganizationIndex2, String connection) {
		// TODO Auto-generated method stub
		
	}

	public void organizationChanged(Object metaIndex,
			Object metaOrganizationIndex, ChangeType changeType,
			ElementType elementType, String elementId) {
		// TODO Auto-generated method stub
		
	}

	public void organizationCreated(Object metaIndex,
			Object metaOrganizationIndex, String rootNodeId) {
		Organization org = metaGraph.getManager().getOrganization(metaOrganizationIndex);
		AntCentroidAlgorithm algo = new AntCentroidAlgorithm();
	}

	public void organizationMerged(Object metaIndex,
			Object metaOrganizationIndex1, Object metaOrganizationIndex2,
			String rootNodeId) {
		// TODO Auto-generated method stub
		
	}

	public void organizationRemoved(Object metaIndex,
			Object metaOrganizationIndex) {
		// TODO Auto-generated method stub
		
	}

	public void organizationRootNodeUpdated(Object metaIndex,
			Object metaOrganizationIndex, String rootNodeId) {
		// TODO Auto-generated method stub
		
	}

	public void organizationSplited(Object metaIndex,
			Object metaOrganizationBase, Object metaOrganizationChild) {
		// TODO Auto-generated method stub
		
	}
}
