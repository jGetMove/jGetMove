package fr.jgetmove.jgetmove.database;

import fr.jgetmove.jgetmove.exception.ClusterNotExistException;
import fr.jgetmove.jgetmove.io.Input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contient toutes les structures de données
 */
public class Database {
    private Input inputObj, inputTime;


    private HashMap<Integer, Cluster> clusters;
    private HashMap<Integer, Transaction> transactions;
    private HashMap<Integer, Time> times;
    private ArrayList<Integer> itemsets;

    /**
     * Initialise Database en fonction des fichiers de données
     *
     * @param inputObj  fichier (transactionId [clusterId ...])
     * @param inputTime fichier (timeId clusterId)
     * @throws IOException              si inputObj ou inputTime est incorrect
     * @throws ClusterNotExistException si le cluster n'existe pas
     */
    public Database(Input inputObj, Input inputTime) throws IOException, ClusterNotExistException {
        this.inputObj = inputObj;
        this.inputTime = inputTime;

        clusters = new HashMap<>();
        transactions = new HashMap<>();
        times = new HashMap<>();
        itemsets = new ArrayList<>();
        //Initialisation des clusters et transactions
        initClusterAndTransaction();
        //Initialisation des temps
        initTimeAndCluster();


    }

    /**
     * Initialise les HashMap de clusters et transactions
     *
     * @throws IOException si le fichier est incorrect
     * @see Database#Database(Input, Input)
     */
    private void initClusterAndTransaction() throws IOException {
        String line;
        int transactionId = 0;
        while ((line = inputObj.readLine()) != null) {
            String[] lineClusterId = line.split("( |\\t)+");
            Transaction transaction = new Transaction(transactionId);

            for (String strClusterId : lineClusterId) {
                int clusterId = Integer.parseInt(strClusterId);
                Cluster cluster;

                if (clusters.get(clusterId) == null) {
                    cluster = new Cluster(clusterId);
                    this.add(cluster);
                    itemsets.add(clusterId);
                } else {
                    cluster = clusters.get(clusterId);
                }

                cluster.add(transaction);
                transaction.add(cluster);
            }

            this.add(transaction);

            transactionId++;
        }
    }

    /**
     * Initialise la liste des temps ainsi que les clusters associés
     *
     * @throws IOException              si le fichier inputTime est inccorect
     * @throws ClusterNotExistException si le cluster n'existe pas
     * @see Database#Database(Input, Input)
     */
    private void initTimeAndCluster() throws IOException, ClusterNotExistException {
        String line;
        int timeId;
        int clusterId;

        while ((line = inputTime.readLine()) != null) {
            String[] splitLine = line.split("( |\\t)+");

            if (splitLine.length == 2) { //Check si non malformé

                timeId = Integer.parseInt(splitLine[0]);
                clusterId = Integer.parseInt(splitLine[1]);
                Time time;
                Cluster cluster;

                //Check si le temps existe
                if (times.get(timeId) == null) {
                    time = new Time(timeId);
                    this.add(time);
                } else {
                    time = times.get(timeId);
                }

                //Check si le cluster existe
                if (clusters.get(clusterId) == null) {
                    throw new ClusterNotExistException();
                } else {
                    cluster = clusters.get(clusterId);
                    cluster.setTime(time);
                    time.add(cluster);
                }
            }
        }
    }

    public Set<Integer> getItemset() {
        /// nonesense ?
        Set<Integer> itemsets = new TreeSet<>();

        for (Transaction transaction : transactions.values()) {
            itemsets.addAll(transaction.getClusterIds());

        }

        return itemsets;
    }

    /**
     * @return l'ensemble des itemsets dans une arrayList
     */
    public ArrayList<Integer> getItemsets() {
        return itemsets;

    }

    /**
     * @param cluster le cluster à ajouter à la base
     */
    private void add(Cluster cluster) {
        this.clusters.put(cluster.getId(), cluster);
    }

    /**
     * @param transaction la transaction à ajouter à la base
     */
    private void add(Transaction transaction) {
        this.transactions.put(transaction.getId(), transaction);
    }

    /**
     * @param time le temps � ajouter � la base
     */
    private void add(Time time) {
        this.times.put(time.getId(), time);
    }

    /**
     * @return l'ensemble des Clusters de la base
     */
    public HashMap<Integer, Cluster> getClusters() {
        return clusters;
    }

    /**
     * @return l'ensemble des ClustersIds de la base
     */
    public Set<Integer> getClusterIds() {
        return clusters.keySet();
    }

    /**
     * @param clusterId l'index du cluster
     * @return le cluster possèdant l'identifiant clusterId
     */
    public Cluster getClusters(int clusterId) {
        return clusters.get(clusterId);
    }

    /**
     * @return l'ensemble des Transactions de la base
     */
    public HashMap<Integer, Transaction> getTransactions() {
        return transactions;
    }

    /**
     * @return La transaction de la base à l'index en paramètre
     */
    public Transaction getTransaction(int transactionId) {
        return transactions.get(transactionId);
    }

    /**
     * @return le nombre de Transaction de la base
     */
    public int getNumberOfTransaction() {
        return transactions.size();
    }

    /**
     * @return l'ensemble des Times de la base
     */
    public HashMap<Integer, Time> getTimes() {
        return times;
    }

    public Set<Integer> getTimeIds() {
        return times.keySet();
    }

    @Override
    public String toString() {
        String str = "Fichiers :" + inputObj + "; " + inputTime + "\n";
        str += "Clusters :" + clusters + "\n";
        str += "Transactions :" + transactions + "\n";
        str += "Temps :" + times + "\n";
        return str;
    }
}
