package fr.jgetmove.jgetmove.solver;

import java.util.Set;
import java.util.Vector;

import fr.jgetmove.jgetmove.database.Database;

public class Solver implements ISolver {
	
	private int minSupport, maxPattern, minTime;
	private Set<Integer> totalItem;
	
	/**
	 * @param minSupport support minimal
	 * @param maxPattern nombre maximal de pattern a trouv�
	 * @param minTime temps minimal
	 */
	public Solver(int minSupport, int maxPattern,  int minTime ){
		this.minSupport = minSupport;
		this.maxPattern = maxPattern;
		this.minTime = minTime;
	}
	
	/**
	 * Initialise le solver � partir d'une base de donn�es
	 * @param database la base de donn�es � analyser
	 */
	public void initLcm(Database database){
		
		totalItem = database.getItemset();
		System.out.println("totalItem : " + totalItem);
		
		Vector<Integer> itemsets = new Vector<Integer>();
		Vector<Integer> freqList = new Vector<Integer>();
		
		LcmIterNew(database,itemsets, freqList);
	}
	
	/**
	 * @param database La database � analyser
	 * @param itemsets une liste representant les id/itemsets
	 * @param freqList une liste reprensentant less clusters frequents
	 */
	private void LcmIterNew(Database database, Vector<Integer> itemsets , Vector<Integer> freqList){
		
		Vector <Vector<Integer>> generatedItemsets = new Vector<Vector<Integer>>();
		Vector <Vector<Integer>> generatedTimeId = new Vector<Vector<Integer>>();
		Vector <Vector<Integer>> generatedItemId = new Vector<Vector<Integer>>();
		
		int sizeGenerated = 1;
		
		GenerateItemset(database,itemsets,generatedItemsets,generatedTimeId, generatedItemId, sizeGenerated);		
		
	}

	/**
	 * @param database la database � analyser
	 * @param itemsets une liste representant les id/itemsets
	 * @param generatedItemsets une liste reprensentant les itemsetsGenerees
	 * @param generatedTimeId une liste reprensentant les tempsGenerees
	 * @param generatedItemId une liste reprensentant les itemGenerees
	 * @param sizeGenerated la taille des itemsets gener�es
	 */
	private void GenerateItemset(Database database, Vector<Integer> itemsets,
			Vector<Vector<Integer>> generatedItemsets, Vector<Vector<Integer>> generatedTimeId,
			Vector<Vector<Integer>> generatedItemId, Integer sizeGenerated) {
		
		if(itemsets.size() == 0){
			sizeGenerated = 0;
			return;
		}
		else {
			Integer [] times = database.getTimes().keySet().toArray(new Integer[database.getTimes().keySet().size()]);
			Vector <Integer> clusterId = new Vector <Integer> (database.getClusters().keySet());
			Vector <Integer> listOfDates = new Vector<Integer>();
			int numberSameTime = 0;
			int lastTime = 0;
			
			for(int i=0;i<itemsets.size();i++){
				listOfDates.add(times[itemsets.get(i)]);
				if(i!= itemsets.size() -1){
					if(times[itemsets.get(i)] == times[itemsets.get(i+1)]){
						numberSameTime++;
					}	
				}
				lastTime = times[itemsets.get(i)];
			}
			generatedItemsets.clear();
			
			if(numberSameTime == 0){
				generatedTimeId.add(listOfDates);
				generatedItemId.add(clusterId);
			}
			
			//Manage MultiClustering
			Vector<Vector<Integer>> posDates = new Vector<Vector<Integer>>();
			for(int i=1; i<lastTime ; i++){
				Vector<Integer> row = new Vector<Integer>();
				for(int j=0; j<itemsets.size(); j++){
					if(times[itemsets.get(i)]==i){
						row.add(itemsets.get(i));
					}
				}
				posDates.add(row);
			}
			//sizeGenerated stands for the number of potential itemsets to generate
			sizeGenerated = 1;
			for(int i=0; i<posDates.size();i++){
				sizeGenerated *= posDates.get(i).size();
			}
			
			//initialise the set of generated itemsets
			for (int itemsetIndex = 0; itemsetIndex < posDates.size(); ++itemsetIndex)
			{
				Vector<Integer> itemset = posDates.get(itemsetIndex);
				Vector<Integer> singleton = new Vector<Integer>();
				
				if(itemsetIndex == 0){
					for(Integer iterator : itemset){
						singleton.add(iterator);
						generatedItemsets.add(singleton);
						singleton.clear(); //OLOL #Yolo		
					}
				} else {
					//OUI
					Vector<Vector<Integer> > new_results = new Vector<Vector<Integer> >();
					Vector<Integer> new_result = new Vector<Integer>();
					for(Vector<Integer> iterator : generatedItemsets){
						Vector<Integer> result = iterator;
						for(Integer itItem : itemset){
							new_result = result;
							new_result.add(itItem);
							new_results.add(new_result);
						}	
					}
					generatedItemsets = new_results;
				}
			}
			// Remove the itemsets already existing in the set of transactions
			int nb = database.getNumberOfTransaction();
			Vector<Integer> tempo;
			Vector<Vector<Integer> > CheckedItemsets = new Vector<Vector<Integer> >();
			Vector<Integer> currentItemsets = new Vector<Integer>();
			int nbitemsets = 0;
			boolean insertok;
			for (int u = 0; u < generatedItemsets.size(); u++){
				insertok = true;
				currentItemsets.clear();
				currentItemsets = generatedItemsets.get(u);
				for (int i = 0; i < nb ; i++){
					/*tempo = database.getTransaction(i).itemsets;
					if(tempo==generatedItemsets.get(u)) {
						insertok = false; 
						break;
					}*/
					//Probleme avec la ligne 151
				}
				if(insertok) {
					CheckedItemsets.add(currentItemsets);
					nbitemsets++;
				}
			}
			sizeGenerated=nbitemsets;
			generatedItemsets.clear();
		    generatedItemsets=CheckedItemsets;
		    listOfDates.clear();
		    for (int l=0;l< sizeGenerated;l++){
		    	for(int u=0; u<generatedItemsets.get(l).size(); u++){
		    		for(int i=0; i<itemsets.size();i++){
		    			if(generatedItemsets.get(l).get(u) == itemsets.get(i)){
		    				listOfDates.add(times[itemsets.get(i)]);
		    			}
		    		}
		    	}
		    }
		    generatedTimeId.add(listOfDates);
		    generatedItemId.add(clusterId);
		}	
	}
}
