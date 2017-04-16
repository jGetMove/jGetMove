package fr.jgetmove.jgetmove.detector;

import fr.jgetmove.jgetmove.database.Cluster;
import fr.jgetmove.jgetmove.database.Database;
import fr.jgetmove.jgetmove.database.Time;
import fr.jgetmove.jgetmove.debug.Debug;
import fr.jgetmove.jgetmove.motifs.Convoy;
import fr.jgetmove.jgetmove.motifs.Pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Class/Singleton related to the detection of convoys in a database
 */
public class ConvoyDetector implements Detector {

    private static ConvoyDetector convoyDetector;
    private int minTime;

    /**
     * Empty Constructor
     */
    private ConvoyDetector(int minTime) {
        this.minTime = minTime;
    }

    /**
     * Cree une instance de ConvoyDetector ou retourne celle deja presente
     *
     * @return une nouvelle instance de convoyDetector si elle n'a pas été deja crée
     */
    public static ConvoyDetector getInstance(int minTime) {
        if (convoyDetector == null) {
            convoyDetector = new ConvoyDetector(minTime);
            return convoyDetector;
        }
        return convoyDetector;
    }

    /**
     * Detect les pattern associé au detecteur
     *
     * TODO: fonction d'origine
     *
     * @return une liste de motif/pattern present dans la database pour ce detecteur
     */
    public ArrayList<Pattern> detect(Database database, ArrayList<ArrayList<Integer>> clustersGenerated) {

        ArrayList<Pattern> convoys = new ArrayList<>();

        for (ArrayList<Integer> itemset : clustersGenerated) {
            ArrayList<Integer> timeSet = new ArrayList<>();
            for (int clusterId : itemset) {
                timeSet.add(database.getClusterTimeId(clusterId));
            }


            int firstTime = 0; //Premier temps
            int lastTime = firstTime; //lastTime
            int currentTime;

            Set<Integer> correctTransactionSet = new HashSet<>(database.getClusterTransactions(itemset.get(0)).keySet());
            Set<Integer> currentTransactionSet;
            //Pour tout les temps
            for (int i = 0; i < timeSet.size(); i++) {
                currentTransactionSet = new HashSet<>(database.getClusterTransactions(itemset.get(i)).keySet());
                currentTime = timeSet.get(i);

                //Recopie inutile ?
                if (currentTime == lastTime + 1) {
                    ArrayList<Integer> transactionList = new ArrayList<>();
                    for (int transactionId : correctTransactionSet) {
                        if (currentTransactionSet.contains(transactionId)) {
                            transactionList.add(transactionId);
                        }
                    }
                    correctTransactionSet.clear();
                    correctTransactionSet.addAll(transactionList);

                    lastTime = currentTime;
                } else {
                    if (currentTime > (lastTime + 1)) {
                        firstTime = currentTime;
                        lastTime = currentTime;

                        //Si l'ecart de temps est superieur a min_t
                        if (lastTime - firstTime >= minTime) {
                            //Init new Convoy
                            Set<Time> timesOfItemset = new HashSet<>();
                            Set<Cluster> clustersOfItemset = new HashSet<>();

                            //Get Clusters Object from database
                            for (int clusterIndex = 0; clusterIndex < correctTransactionSet.size(); clusterIndex++) {
                                clustersOfItemset.add(database.getCluster(clusterIndex));
                            }

                            convoys.add(new Convoy(clustersOfItemset, timesOfItemset));
                        } else {
                            correctTransactionSet.clear();
                            correctTransactionSet.addAll(currentTransactionSet);
                        }
                    }
                }
            }
            //Si L'ecart entre lastTime et firstTime est superieur a min_t
            if ((lastTime - firstTime) >= minTime) {
                //Init new Convoy
                Set<Time> timesOfItemset = new HashSet<>();
                Set<Cluster> clustersOfItemset = new HashSet<>();
                //Get Clusters Object from database
                for (int clusterIndex = 0; clusterIndex < correctTransactionSet.size(); clusterIndex++) {
                    clustersOfItemset.add(database.getCluster(clusterIndex));
                }
                //Get Times Object from database

                for (int time = firstTime; time < lastTime; time++) {
                    timesOfItemset.add(database.getTime(timeSet.get(time)));
                }

                convoys.add(new Convoy(clustersOfItemset, timesOfItemset));
            }
        }
        Debug.println("Il y a : " + convoys.size() + "Convoy");

        return convoys;
    }
}
