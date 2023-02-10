package jadescript.util;

import java.util.Iterator;


public class IntegerRange implements Iterable<Integer> {

    private Integer start;
    private Integer end;
    private boolean descending;


    public IntegerRange(
        Integer start,
        Integer end,
        boolean startIncluded,
        boolean endIncluded
    ) {
        descending = start > end;
        if (startIncluded) {
            this.start = start;
        } else {
            if (descending) {
                this.start = start - 1;
            } else {
                this.start = start + 1;
            }
        }
        if (endIncluded) {
            this.end = end;
        } else {
            if (descending) {
                this.end = end + 1;
            } else {
                this.end = end - 1;
            }
        }
    }


    public Integer getStart() {
        return start;
    }


    public void setStart(Integer start) {
        this.start = start;
        descending = start > end;
    }


    public Integer getEnd() {
        return end;
    }


    public void setEnd(Integer end) {
        this.end = end;
        descending = start > end;
    }


    public boolean isDescending() {
        return descending;
    }


    @Override
    public Iterator<Integer> iterator() {
        return new IntegerRangeIterator(start, end, descending);
    }


    public static class IntegerRangeIterator implements Iterator<Integer> {

        private final Integer end;
        private final boolean descending;
        private int index;


        public IntegerRangeIterator(
            Integer start,
            Integer end,
            boolean descending
        ) {
            this.end = end;
            this.descending = descending;
            index = start;
        }


        @Override
        public boolean hasNext() {
            if (descending) {
                return index >= end;
            }
            return index <= end;
        }


        @Override
        public Integer next() {
            return (descending ? index-- : index++);
        }

    }


}
