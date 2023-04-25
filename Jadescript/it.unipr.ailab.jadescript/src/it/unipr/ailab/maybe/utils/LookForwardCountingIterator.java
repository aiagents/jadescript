package it.unipr.ailab.maybe.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;


public abstract class LookForwardCountingIterator<T, R> implements Iterator<R> {

    private final Iterator<T> wrappedIterator;

    @Nullable
    private T extractedNext = null;
    private int sourceCount = 0;
    private int acceptedCount = 0;
    private int extractCount = 0;

    protected LookForwardCountingIterator(@NotNull Iterable<T> it) {
        this.wrappedIterator = it.iterator();
    }


    /**
     * Counts the number of elements consumed by the source iterator.
     */
    protected int getSourceCount(){
        return sourceCount;
    }


    /**
     * Counts the number of elements considered accepted by
     * {@link LookForwardCountingIterator#isAcceptable(Object)}.
     */
    protected int getAcceptedCount(){
        return acceptedCount;
    }


    /**
     * Counts the number of elements effectively transformed and extracted by
     * {@link LookForwardCountingIterator#next()}.
     */
    protected int getExtractCount(){
        return extractCount;
    }

    protected abstract boolean isAcceptable(T t);

    protected abstract R transform(T t);

    @Override
    public boolean hasNext() {
        if(this.extractedNext != null){
            return true;
        }

        while (this.wrappedIterator.hasNext()) {
            T t = this.wrappedIterator.next();
            sourceCount++;
            if (isAcceptable(t)) {
                acceptedCount++;
                this.extractedNext = t;
                return true;
            }
        }

        return false;
    }


    @Override
    public R next() {
        if(this.extractedNext == null) {
            throw new NoSuchElementException();
        }
        final R result = transform(this.extractedNext);
        extractCount++;
        this.extractedNext = null;
        return result;
    }


}
