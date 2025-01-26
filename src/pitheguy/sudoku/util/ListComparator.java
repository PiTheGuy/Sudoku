package pitheguy.sudoku.util;

import java.util.Comparator;
import java.util.List;

public class ListComparator<T extends Comparable<T>> implements Comparator<List<T>> {
    @Override
    public int compare(List<T> list1, List<T> list2) {
        int sizeComparison = Integer.compare(list1.size(), list2.size());
        if (sizeComparison != 0) return sizeComparison;
        for (int i = 0; i < list1.size(); i++) {
            int elementComparison = list1.get(i).compareTo(list2.get(i));
            if (elementComparison != 0) return elementComparison;
        }
        return 0;
    }
}
