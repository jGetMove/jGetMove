package fr.jgetmove.jgetmove.solver;

import fr.jgetmove.jgetmove.database.Database;
import fr.jgetmove.jgetmove.database.Transaction;
import fr.jgetmove.jgetmove.debug.Debug;
import fr.jgetmove.jgetmove.debug.TraceMethod;
import fr.jgetmove.jgetmove.detector.Detector;
import fr.jgetmove.jgetmove.pattern.Pattern;
import fr.jgetmove.jgetmove.utils.GeneratorUtils;

import java.util.*;

public class PatternGenerator implements Generator {

    private final Database defaultDatabase;
    private ArrayList<ArrayList<Integer>> clustersGenerated;
    private int minSupport, maxPattern, minTime;
	private boolean printed;

    /**
     * Initialise le solveur.
     *
     * @param database   database par defaut
     * @param minSupport support minimal
     * @param maxPattern nombre maximal de pattern a trouvé
     * @param minTime    temps minimal
     */
    public PatternGenerator(Database database,int minSupport, int maxPattern, int minTime) {
        this.minSupport = minSupport;
        this.maxPattern = maxPattern;
        this.minTime = minTime;
        this.clustersGenerated = new ArrayList<>();
        this.defaultDatabase = database;
        printed = false;
        
    }
    

    /**
     * <pre>
     * Lcm::MakeClosure(const Database &database, vector<int> &transactionList,
     * vector<int> &q_sets, vector<int> &itemsets,
     * int item)
     * </pre>
     *
     * @param database       (database)
     * @param transactionIds (transactionList)
     * @param qSets          (q_sets)
     * @param itemset        (itemsets)
     * @param freq           (item)
     * @deprecated use {@link GeneratorUtils#makeClosure(Database, Set, ArrayList, ArrayList, int)}
     */
    @TraceMethod(displayTitle = true)
    private static void MakeClosure(Database database, Set<Integer> transactionIds, ArrayList<Integer> qSets,
                                    ArrayList<Integer> itemset, int freq) {
        GeneratorUtils.makeClosure(database, transactionIds, qSets, itemset, freq);
    }

    /**
     * Teste si p(i-1) == q(i-1)
     * <p>
     * <pre>
     * Lcm::PpcTest(database, []itemsets, []transactionList, item, []newTransactionList)
     * </pre>
     *
     * @param clusters          (itemsets)
     * @param transactionIds    (transactionList)
     * @param freqClusterId     (item)
     * @param newTransactionIds (newTransactionList)
     * @return vrai si ppctest est réussi
     * @deprecated use {@link Database#getFilteredTransactionIdsIfHaveCluster(Set, int)} and {@link GeneratorUtils#ppcTest(Database, ArrayList, Set, int, Set)}
     */
    @TraceMethod(displayTitle = true)
    private static boolean PPCTest(Database database, ArrayList<Integer> clusters, Set<Integer> transactionIds,
                                   int freqClusterId, Set<Integer> newTransactionIds) {
        // CalcTransactionList
        for (int transactionId : transactionIds) {
            Transaction transaction = database.getTransaction(transactionId);

            if (transaction.getClusterIds().contains(freqClusterId)) {
                newTransactionIds.add(transactionId);
            }
        }

        for (int clusterId = 0; clusterId < freqClusterId; clusterId++) {
            if (!clusters.contains(clusterId) && GeneratorUtils
                    .CheckItemInclusion(database, newTransactionIds, clusterId)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <pre>
     * Lcm::CalcurateCoreI(database, itemsets, freqList)
     * </pre>
     *
     * @param clusterIds         (itemsets)
     * @param frequentClusterIds (freqList)
     * @return le dernier élement different du dernier clusterId de frequentClusterIds, si frequentClusterIds est trop petit, renvoie le premier element de clusterIds, sinon renvoi 0 si clusterIds est vide
     * @deprecated use {@link GeneratorUtils#getDifferentFromLastCluster(ArrayList, ArrayList)}
     */
    private static int CalcurateCoreI(ArrayList<Integer> clusterIds, ArrayList<Integer> frequentClusterIds) {
        return GeneratorUtils.getDifferentFromLastCluster(clusterIds, frequentClusterIds);
    }

    /**
     */
    protected void run(Database database, ArrayList<ArrayList<Integer>> lvl2ClusterId,
                       ArrayList<ArrayList<Integer>> lvl2TimeId, Set<Detector> detectors) {
    	
    	
        ArrayList<Integer> itemsets = new ArrayList<>();
        ArrayList<Integer> freqList = new ArrayList<>();
        Debug.println("Database : " + database);
        Debug.println("Run Lvl2TimeId  " +lvl2TimeId);
        Debug.println("Run Lvl2ClusterId  " +lvl2ClusterId);
        run(database, itemsets, database.getTransactionIds(), freqList, lvl2ClusterId, lvl2TimeId, detectors);

    }

    private void run(Database database, ArrayList<Integer> itemsets, Set<Integer> transactionIds,
                     ArrayList<Integer> freqList, ArrayList<ArrayList<Integer>> lvl2ClusterId,
                     ArrayList<ArrayList<Integer>> lvl2TimeId, Set<Detector> detectors) {
    	Debug.println("RUN");
    	Debug.println("");

        int calcurateCoreI = CalcurateCoreI(itemsets, freqList);
        System.err.println("CORE I : " + calcurateCoreI);
        SortedSet<Integer> lowerBounds = GeneratorUtils.lower_bound(database.getClusterIds(), calcurateCoreI);
        
        //Blocks
        ArrayList<Integer> blocks = new ArrayList<>();
        for(int i=0;i<itemsets.size();i++){
        	blocks.add(lvl2TimeId.get(itemsets.get(i)).get(0));
        }
        // freq_i
        ArrayList<Integer> freqClusterIds = new ArrayList<>();
        Debug.println("lower bounds : " + lowerBounds.size());

        for (int clusterId : lowerBounds) {

            if (database.getCluster(clusterId).getTransactions().size() >= minSupport &&
                    !itemsets.contains(clusterId)) {
                freqClusterIds.add(clusterId);
            }
        }
        Debug.println("freq_i : " + freqClusterIds);

        ArrayList<Integer> qSets = new ArrayList<>();
        ArrayList<Integer> newFreqList = new ArrayList<>();

        for (int freqClusterId : freqClusterIds) {
            Set<Integer> newTransactionIds = new TreeSet<>();
            
            int blockOfItem = lvl2TimeId.get(freqClusterId).get(0);
            if (PPCTest(database, itemsets, transactionIds, freqClusterId, newTransactionIds) &&
            		!blocks.contains(blockOfItem)) {
                qSets.clear();
                MakeClosure(database, newTransactionIds, qSets, itemsets, freqClusterId);

                if (maxPattern == 0 || qSets.size() <= maxPattern) {
                    newTransactionIds.clear();

                    Set<Integer> iterTransactionIds = GeneratorUtils
                            .updateTransactions(database, database.getTransactionIds(), qSets, freqClusterId);
                    newFreqList.clear();
                    newFreqList = GeneratorUtils
                            .updateFreqList(database, database.getTransactionIds(), qSets, freqList, freqClusterId);
                    run(database, qSets, iterTransactionIds, newFreqList, lvl2ClusterId, lvl2TimeId, detectors);
                }
            }
        }
        if(!printed){
        	printItemsets(defaultDatabase,database,itemsets, lvl2ClusterId, lvl2TimeId, detectors);
        	printed = true;
        }
    }

    private void printItemsets(Database tempDb, Database database2, ArrayList<Integer> itemsets,
                               ArrayList<ArrayList<Integer>> lvl2ClusterId, ArrayList<ArrayList<Integer>> lvl2TimeId,
                               Set<Detector> detectors) {
    	Debug.errln("Print Itemsets");
    	Debug.println("Database2" + database2);
    	Debug.println("itemsets : " + itemsets);
    	Debug.println("lvl2TimeId : " + lvl2TimeId);
    	Debug.println("lvl2ClusterId : " + lvl2ClusterId);
        if (itemsets.size() > 0) {

            for (int i = 0; i < database2.getClusters().size(); i++) {

                Set<Integer> timeBased = new TreeSet<>();
                Set<Integer> clusterBased = new TreeSet<>();
                
                Collection<Transaction> transactions = database2.getCluster(i).getTransactions().values();
                Debug.errln("TEST : " +transactions);
                for (int j = 1; j < lvl2TimeId.get(i).size(); j++) {
                    timeBased.add(lvl2TimeId.get(i).get(j));
                }

                for (int j = 1; j < lvl2ClusterId.get(i).size(); j++) {
                    clusterBased.add(lvl2ClusterId.get(i).get(j));
                }
                
                if (timeBased.size() > minTime){
                	 HashMap<Detector, ArrayList<Pattern>> motifs = new HashMap<>();
                     for (Detector detector : detectors) {
                     	motifs.put(detector, detector.detect(defaultDatabase, timeBased, clusterBased, transactions));
                     }
                }
            }
        }
    }


	@Override
	public ClusterGeneratorResult generate() {
		// TODO Auto-generated method stub
		return null;
	}
}
