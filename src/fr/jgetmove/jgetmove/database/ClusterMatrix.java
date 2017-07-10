package fr.jgetmove.jgetmove.database;

import fr.jgetmove.jgetmove.debug.Debug;
import fr.jgetmove.jgetmove.debug.PrettyPrint;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Used in ItemsetDetector, holds a {@link Cluster}-{@link Time} matrix and a {@link Cluster}-{@link Transaction} matrix
 */
public class ClusterMatrix implements PrettyPrint {

    private HashMap<Integer, Integer> clusterTimeMatrix;
    private HashMap<Integer, TreeSet<Integer>> clusterTransactionsMatrix;


    /**
     * Initializes the cluster by tacking the relations of the block as a reference
     *
     * @param base Initial base reference
     */
    public ClusterMatrix(Base base) {
        clusterTimeMatrix = new HashMap<>();
        clusterTransactionsMatrix = new HashMap<>();

        for (Transaction transaction : base.getTransactions().values()) {
            for (int clusterId : transaction.getClusterIds()) {

                if (!clusterTransactionsMatrix.containsKey(clusterId)) {
                    clusterTransactionsMatrix.put(clusterId, new TreeSet<>());
                    clusterTimeMatrix.put(clusterId, base.getClusterTimeId(clusterId));
                }

                clusterTransactionsMatrix.get(clusterId).add(transaction.getId());
            }
        }
    }

    /**
     * @param clusterId the identifier of the cluster
     * @return the time id of the given cluster
     */
    public int getClusterTimeId(int clusterId) {
        return clusterTimeMatrix.get(clusterId);
    }

    /**
     * @param clusterId cluster's identifier
     * @return returns all the transactions of a cluster as a treeset
     */
    public TreeSet<Integer> getClusterTransactionIds(int clusterId) {
        return clusterTransactionsMatrix.get(clusterId);
    }

    /**
     * Rebinds the cluster-transaction matrix by removing the transactions not present in transactionIds and adding the one which are
     * <p>
     * <pre>
     * Lcm::UpdateOccurenceDeriver(const DataBase &database, const vector<int> &transactionList, ClusterMatrix &occurence)
     * </pre>
     *
     * @param base           Reference database to retrieve the bindings
     * @param transactionIds Transactions which need to be present in the matrix
     */
    public void optimizeMatrix(Base base, Set<Integer> transactionIds) {
        clusterTransactionsMatrix.forEach((clusterId, transactions) -> transactions.clear());
        for (int transactionId : transactionIds) {
            Transaction transaction = base.getTransaction(transactionId);
            Set<Integer> clusterIds = transaction.getClusterIds();

            for (int clusterId : clusterIds) {
                if (!clusterTransactionsMatrix.containsKey(clusterId)) {
                    clusterTransactionsMatrix.put(clusterId, new TreeSet<>());
                    clusterTimeMatrix.put(clusterId, base.getClusterTimeId(clusterId));
                }

                clusterTransactionsMatrix.get(clusterId).add(transactionId);
            }
        }
    }

    @Override
    public String toPrettyString() {
        return "\n|-- ClusterMatrix :" + clusterTransactionsMatrix +
                "\n`-- TimeMatrix :" + clusterTimeMatrix;
    }

    @Override
    public String toString() {
        return Debug.indent(toPrettyString());
    }
}
