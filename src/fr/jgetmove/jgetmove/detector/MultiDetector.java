/*
 * Copyright 2019 jGetMove
 *
 * Cette œuvre est mise à disposition sous licence Attribution - Pas d’Utilisation Commerciale - Partage dans les Mêmes Conditions 3.0 France. Pour voir une copie de cette licence, visitez http://creativecommons.org/licenses/by-nc-sa/3.0/fr/ ou écrivez à Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package fr.jgetmove.jgetmove.detector;

import fr.jgetmove.jgetmove.database.DataBase;
import fr.jgetmove.jgetmove.database.Itemset;
import fr.jgetmove.jgetmove.pattern.Pattern;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Defines the structure of a Detector which need all the found itemsets to work
 *
 * @author stardisblue
 * @version 1.1.0
 * @implNote please use {@link SingleDetector} when possible as manipulating a collection can be more memory consuming
 * @see SingleDetector
 * @since 0.2.0
 */
public interface MultiDetector extends Detector {
    ArrayList<Pattern> detect(DataBase defaultDataBase, Collection<Itemset> itemset);
}
