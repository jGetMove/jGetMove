package fr.jgetmove.jgetmove.solver;

import fr.jgetmove.jgetmove.config.DefaultConfig;
import fr.jgetmove.jgetmove.database.*;
import fr.jgetmove.jgetmove.debug.Debug;
import fr.jgetmove.jgetmove.debug.PrettyPrint;
import fr.jgetmove.jgetmove.detector.Detector;
import fr.jgetmove.jgetmove.detector.MultiDetector;
import fr.jgetmove.jgetmove.detector.SingleDetector;
import fr.jgetmove.jgetmove.pattern.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.*;

/**
 * Manager that handles all the core logic of the application.
 * <p>
 * It's used to call {@link ItemsetsFinder} to detect all the itemsets per block
 * <p>
 * It's used to call {@link PatternGenerator} to merge them in a single array of itemsets used for detecting different patterns with their given detectors.
 */
public class Solver implements PrettyPrint {

    /**
     * Config shared accross the application
     */
    private final DefaultConfig config;

    /**
     * ItemsetsFinder that will handle the creation of itemsets
     */
    private ItemsetsFinder itemsetsFinder;

    /**
     * List of detectors called foreach itemset
     *
     * @see SingleDetector#detect(DataBase, Itemset)
     */
    private Set<SingleDetector> singleDetectors;

    /**
     * List of detectors called once, passing all the itemsets
     *
     * @see MultiDetector#detect(DataBase, List)
     */
    private Set<MultiDetector> multiDetectors;


    /**
     * Prepares the solver with all the elements used to detect patterns.
     *
     * @param itemsetsFinder  DI, has for function to find all the itemsets of the database
     * @param singleDetectors initializes all the singleDetectors to use
     * @param multiDetectors  initializes all the multiDetectors to use
     * @param config          configuration
     */
    public Solver(ItemsetsFinder itemsetsFinder,
                  Set<SingleDetector> singleDetectors, Set<MultiDetector> multiDetectors, DefaultConfig config) {
        this.itemsetsFinder = itemsetsFinder;
        this.singleDetectors = singleDetectors;
        this.multiDetectors = multiDetectors;
        this.config = config;
    }

    /**
     * Prepares the solver with all the elements used to detect patterns.
     *
     * @param itemsetsFinder DI, has for function to find all the itemsets of the database
     * @param config         configuration
     */
    public Solver(ItemsetsFinder itemsetsFinder, DefaultConfig config) {
        this.itemsetsFinder = itemsetsFinder;
        singleDetectors = new HashSet<>();
        multiDetectors = new HashSet<>();
        this.config = config;
    }

    /**
     * Finds all the itemsets from the database.
     * <p>
     * Breaks the task by blocks (a given time interval {@link DefaultConfig#blockSize} ) and returns an array of blocks containing their respective itemsets.
     * <p>
     * If the {@link DefaultConfig#blockSize} is 0, then a single block is returned, containing all the itemset of the database.
     *
     * @return ArrayList of blocks containing it's id and all the itemsets detected in the block
     */
    public ArrayList<ArrayList<Itemset>> findItemsets(DataBase dataBase) {
        Debug.printTitle("Itemsets Finder", Debug.INFO);

        ArrayList<ArrayList<Itemset>> blockItemsets = new ArrayList<>();

        int blockId = 0;

        if (config.getBlockSize() > 0) { // if blockSize is set
            BlockBase blockBase;
            Iterator<Integer> lastTime = dataBase.getTimeIds().iterator();

            while ((blockBase = createBlock(blockId, dataBase, lastTime)) != null) {
                // if there is more than one block, min time is ignored for the glory of mankind (or because it could break the block fusion later on).
                // generating the itemsets
                ArrayList<Itemset> itemsets = itemsetsFinder.generate(blockBase, 0);
                // putting the itemsets in the block
                blockItemsets.add(itemsets);
                ++blockId; // incrementing blockId
            }
        } else { // if blockSize is not set
            // One block for all of this
            ArrayList<Itemset> result = itemsetsFinder.generate(dataBase, config.getMinTime());
            // aaand putting the itemsets in the first block
            blockItemsets.add(blockId, result);
        }

        Debug.println("Blocks", blockItemsets, Debug.DEBUG);
        Debug.println("n° of Blocks", blockItemsets.size(), Debug.INFO);
        return blockItemsets;
    }

    /**
     * Creates and returns an new {@link BlockBase} if and only if there it's possible to create another {@link BlockBase} from the {@link DataBase}.
     * <p>
     * If the lastTime iterator is
     *
     * @param id       the id of the block to create
     * @param dataBase to create the Blocks from
     * @param lastTime the time from where the block need to begin
     * @return null or the created Block
     */
    private BlockBase createBlock(int id, DataBase dataBase, Iterator<Integer> lastTime) {
        BlockBase blockBase = null;

        if (lastTime.hasNext()) {
            blockBase = new BlockBase(id);

            int counter = 0;

            while (lastTime.hasNext() && counter < config.getBlockSize()) {
                // adds a time in the blockBase
                Integer timeId = lastTime.next();
                Time blockTime = new Time(timeId);
                blockBase.add(blockTime);

                for (Cluster cluster : dataBase.getTime(timeId).getClusters().values()) {
                    // adds a (possibly new) cluster in the blockBase and links it with the correct time
                    Cluster blockCluster = blockBase.getOrCreateCluster(cluster.getId());
                    blockCluster.setTime(blockTime);

                    for (int transactionId : cluster.getTransactions().keySet()) {
                        // get all the transactions of the cluster, adds them in the blockBase and links it with cluster
                        Transaction blockTransaction = blockBase.getOrCreateTransaction(transactionId);

                        blockCluster.add(blockTransaction);
                        blockTransaction.add(blockCluster);
                    }
                }
                ++counter;
            }
        }


        return blockBase;
    }

    /**
     * Call PatternGenerator and launches the pattern detection system
     *
     * @return a HashMap SingleDetector -> ArrayList< Motif>
     */
    public HashMap<Detector, ArrayList<Pattern>> detectPatterns(DataBase dataBase, ArrayList<ArrayList<Itemset>> results) {
        Debug.printTitle("Detecting Patterns", Debug.INFO);
        Debug.println("MultiClustering doesn't work properly, please be careful", Debug.ERROR);

        ArrayList<Itemset> itemsets = new ArrayList<>();
        // setting a perf itemset-merged checker
        ArrayList<ArrayList<Integer>> blockItemsetIndex = new ArrayList<>(results.size());
        for (ArrayList<Itemset> result : results) {
            ArrayList<Integer> itemsetIndex = new ArrayList<>(result.size());
            for (int i = 0; i < result.size(); i++) {
                itemsetIndex.add(i);
            }

            blockItemsetIndex.add(itemsetIndex);
        }

        int itemsetId = 0;
        for (int blockId = 0, size = blockItemsetIndex.size() - 1; blockId < size; blockId++) {
            for (Integer itemsetIndex : blockItemsetIndex.get(blockId)) {
                Itemset tomerge = results.get(blockId).get(itemsetIndex);

                Set<Integer> mergedClusters = new HashSet<>(tomerge.getClusters());
                Set<Integer> mergedTimes = new HashSet<>(tomerge.getTimes());
                Set<Integer> mergedTransactions = new HashSet<>(tomerge.getTransactions());

                for (int itBlockId = blockId + 1, itSize = blockItemsetIndex.size(); itBlockId < itSize; ++itBlockId) {
                    for (Iterator<Integer> iterator = blockItemsetIndex.get(itBlockId).iterator(); iterator.hasNext(); ) {
                        Itemset toFuseItemset = results.get(itBlockId).get(iterator.next());

                        if (tomerge.getTransactions().equals(toFuseItemset.getTransactions())) {
                            mergedClusters.addAll(toFuseItemset.getClusters());
                            mergedTimes.addAll(toFuseItemset.getTimes());
                            // not fusing transactions because they are the same

                            iterator.remove();
                        }
                    }
                }

                if (mergedClusters.size() > config.getMinTime()) {
                    itemsets.add(new Itemset(itemsetId, mergedTransactions, mergedClusters, mergedTimes));
                    ++itemsetId;
                }

            }
        }

        for (int itemsetIndex : blockItemsetIndex.get(blockItemsetIndex.size() - 1)) {
            Itemset itemset = results.get(results.size() - 1).get(itemsetIndex);
            if (itemset.getClusters().size() > config.getMinTime()) {
                itemsets.add(new Itemset(itemsetId, itemset.getTransactions(), itemset.getClusters(), itemset.getTimes()));
                ++itemsetId;
            }
        }

        Debug.println("Itemsets", itemsets, Debug.DEBUG);
        Debug.println("n° of itemsets", itemsets.size(), Debug.INFO);

        HashMap<Detector, ArrayList<Pattern>> patterns = new HashMap<>(singleDetectors.size() + multiDetectors.size());
        //patternGenerator.generate(dataBase, results);


        for (SingleDetector singleDetector : singleDetectors) {
            patterns.put(singleDetector, new ArrayList<>());
            ArrayList<Pattern> detectorPatterns = patterns.get(singleDetector);
            for (Itemset itemset : itemsets) {
                detectorPatterns.addAll(singleDetector.detect(dataBase, itemset));
            }

            Debug.println(singleDetector.toString(), detectorPatterns, Debug.DEBUG);
            Debug.println(singleDetector.toString(), detectorPatterns.size() + " patterns found", Debug.INFO);

        }

        for (MultiDetector multiDetector : multiDetectors) {
            patterns.put(multiDetector, multiDetector.detect(dataBase, itemsets));

            Debug.println(multiDetector.toString(), patterns.get(multiDetector), Debug.DEBUG);
            Debug.println(multiDetector.toString(), patterns.get(multiDetector).size() + " patterns found", Debug.INFO);
        }

        return patterns;
    }

    /**
     * {@link SingleDetector#detect(DataBase, Itemset)} is called foreach itemset.
     *
     * @param singleDetector adds this detector to the list of detectors to detect
     */
    private void add(SingleDetector singleDetector) {
        singleDetectors.add(singleDetector);
    }

    /**
     * {@link MultiDetector#detect(DataBase, List)} is called for all Itemsets.
     *
     * @param multiDetector adds this detector to the list of detectors to detect
     */
    private void add(MultiDetector multiDetector) {
        multiDetectors.add(multiDetector);
    }

    /**
     * @param singleDetector removes this detector from the list of detectors to detect
     */
    private void remove(SingleDetector singleDetector) {
        singleDetectors.remove(singleDetector);
    }

    /**
     * @param multiDetector removes this detector from the list of detectors to detect
     */
    private void remove(MultiDetector multiDetector) {
        multiDetectors.remove(multiDetector);
    }

    public JsonArrayBuilder toJson(HashMap<Detector, ArrayList<Pattern>> detectors) {
        JsonArrayBuilder jsonPatterns = Json.createArrayBuilder();

        for (Map.Entry<Detector, ArrayList<Pattern>> detector : detectors.entrySet()) {
            JsonObjectBuilder jsonPattern = Json.createObjectBuilder();

            jsonPattern.add("name", detector.getKey().getClass().getSimpleName());

            ArrayList<Pattern> patterns = detector.getValue();

            JsonArrayBuilder jsonLinks = Json.createArrayBuilder();

            int i = 0;
            for (Pattern pattern : patterns) {
                jsonPattern.add("name", pattern.getClass().getSimpleName());
                for (JsonObject jsonLink : pattern.getLinksToJson(i)) {
                    jsonLinks.add(jsonLink);
                }
                i++;
            }

            jsonPattern.add("links", jsonLinks);
            jsonPatterns.add(jsonPattern);
        }
        return jsonPatterns;
    }

    @Override
    public String toPrettyString() {
        return "\n|-- ItemsetsFinder :" + itemsetsFinder
                + "\n|-- SingleDetectors :" + singleDetectors
                + "\n`-- MultiDetectors :" + multiDetectors;
    }

    @Override
    public String toString() {
        return "\nSolver :" + Debug.indent(toPrettyString());
    }
}
