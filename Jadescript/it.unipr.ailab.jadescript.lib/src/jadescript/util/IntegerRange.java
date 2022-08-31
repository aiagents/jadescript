package jadescript.util;

import java.util.Iterator;


public class IntegerRange implements Iterable<Integer>{
    private Integer start;
    private Integer end;
    private boolean descending;

    public IntegerRange(Integer start, Integer end, boolean startIncluded, boolean endIncluded) {
        descending = start>end;
        this.start = startIncluded ? start : (!descending ? start+1 : start-1);
        this.end = endIncluded ? end : (!descending ? end-1 : end+1);
    }

    public Integer getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setStart(Integer start) {
        this.start = start;
        descending = start>end;
    }

    public void setEnd(Integer end) {
        this.end = end;
        descending = start>end;
    }

    public boolean isDescending() {
        return descending;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new IntegerRangeIterator(start, end, descending);
    }

    public static class IntegerRangeIterator implements Iterator<Integer>{

        private final Integer end;
        private final boolean descending;
        private int index;

        public IntegerRangeIterator(Integer start, Integer end, boolean descending) {
            this.end = end;
            this.descending = descending;
            index = start;
        }

        @Override
        public boolean hasNext() {
            return (!descending) ? (index<=end) : (index>=end);
        }

        @Override
        public Integer next() {
            return (descending?index--:index++);
        }

    }


}
