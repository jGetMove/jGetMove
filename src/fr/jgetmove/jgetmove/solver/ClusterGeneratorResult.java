package fr.jgetmove.jgetmove.solver;

import fr.jgetmove.jgetmove.config.Config;
import fr.jgetmove.jgetmove.database.Database;

import java.util.ArrayList;

public class ClusterGeneratorResult {

    private Database database;
    private ArrayList<ArrayList<Integer>> clusters;
    private ArrayList<ArrayList<Integer>> lvl2TimeIds;
    private ArrayList<ArrayList<Integer>> lvl2ClusterIds;

    ClusterGeneratorResult(Database database, ArrayList<ArrayList<Integer>> clusters,
                           ArrayList<ArrayList<Integer>> lvl2TimeIds, ArrayList<ArrayList<Integer>> lvl2ClusterIds) {
        this.database = database;
        this.clusters = clusters;
        this.lvl2ClusterIds = lvl2ClusterIds;
        this.lvl2TimeIds = lvl2TimeIds;
    }

    public Database getDatabase() {
        return database;
    }

    public ArrayList<ArrayList<Integer>> getClusters() {
        return clusters;
    }

    public ArrayList<ArrayList<Integer>> getLvl2TimeIds() {
        return lvl2TimeIds;
    }

    public ArrayList<ArrayList<Integer>> getLvl2ClusterIds() {
        return lvl2ClusterIds;
    }

    @Override
    public String toString() {
        return "Database : " + database + "\n"
                + "Clusters : " + clusters + "\n"
                + "Lvl2Clusters" + lvl2ClusterIds + "\n"
                + "Lvl2Times" + lvl2TimeIds;
    }
}