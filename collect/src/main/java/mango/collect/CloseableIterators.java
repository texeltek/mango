package mango.collect;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 8/29/12
 * Time: 10:02 AM
 */
public class CloseableIterators {

    public static <F, T> CloseableIterator<T> transform(final CloseableIterator<F> iterator, final Function<F, T> function) {
        Iterator<T> transform = Iterators.transform(iterator, function);
        return CloseableIteratorAdapter.wrap(transform, iterator);
    }

    public static <T> PeekingCloseableIterator<T> peekingIterator(final CloseableIterator<T> iterator) {
        return new PeekingCloseableIterator<T>(iterator);
    }

    public static <T> CloseableIterator<T> limit(final CloseableIterator<T> iterator, final int limitSize) {
        checkNotNull(iterator);
        return new LimitCloseableIterator<T>(iterator, limitSize);
    }

    public static <T> CloseableIterator<T> filter(final CloseableIterator<T> iterator, final Predicate<T> filter) {
        final UnmodifiableIterator<T> filteredIter = Iterators.filter(iterator, filter);
        return CloseableIteratorAdapter.wrap(filteredIter, iterator);
    }

    public static <T> CloseableIterator<T> concat(final CloseableIterator<? extends Iterator<? extends T>> inputs) {
        final Iterator<T> concat = Iterators.concat(inputs);
        return CloseableIteratorAdapter.wrap(concat, inputs);
    }

    /**
     * Concat an Iterable of CloseableIterators. This will allow us to concat any Collection of CloseableIterators.
     *
     * @param iterable
     * @param <T>
     * @return
     */
    public static <T> CloseableIterator<T> chain(final Iterable<? extends CloseableIterator<? extends T>> iterable) {
        return new ChainedIterableCloseableIterator<T>(iterable);
    }

    public static <T> CloseableIterator<T> sortedDistinct(final CloseableIterator<T> iterator) {
        AbstractIterator<T> sortedDistinctIterator = new AbstractIterator<T>() {
            T current = null;

            @Override
            protected T computeNext() {
                if (iterator.hasNext()) {
                    if (current == null) {
                        current = iterator.next();
                        return current;
                    } else {
                        T next = iterator.next();
                        while (current.equals(next)) {
                            if (iterator.hasNext()) {
                                next = iterator.next();
                            } else {
                                return endOfData();
                            }
                        }
                        current = next;
                        return current;
                    }
                } else
                    return endOfData();
            }
        };
        return consumeClose(sortedDistinctIterator, iterator);
    }

    public static <T> CloseableIterator<T> wrap(final Iterator<T> iterator) {
        if (iterator instanceof CloseableIterator) return (CloseableIterator<T>) iterator;

        return new AbstractCloseableIterator<T>() {

            @Override
            public void close() throws IOException {
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    /**
     * Autoclose the iterator when exhausted or if an exception is thrown. It is currently set to protected, so that only
     * classes in this package can use.
     *
     * @param iterator
     * @param <T>
     * @return
     */
    static <T> CloseableIterator<T> autoClose(final CloseableIterator<? extends T> iterator) {
        return new AbstractCloseableIterator<T>() {

            private boolean closed = false;

            @Override
            public void close() throws IOException {
                if (closed)
                    return;

                closed = true;
                iterator.close();
            }

            @Override
            public boolean hasNext() {
                try {
                    if (closed)
                        return false;
                    if (!iterator.hasNext()) {
                        closeQuietly();
                        return false;
                    }
                    return true;
                } catch (RuntimeException re) {
                    closeQuietly();
                    throw re;
                }
            }

            @Override
            public T next() {
                if (hasNext()) {
                    try {
                        return iterator.next();
                    } catch (RuntimeException re) {
                        closeQuietly();
                        throw re;
                    }
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                try {
                    if (hasNext()) {
                        iterator.remove();
                    }
                } catch (RuntimeException re) {
                    closeQuietly();
                    throw re;
                }
            }
        };
    }

    static <T> CloseableIterator<T> consumeClose(final Iterator<T> iterator, final Closeable closeable) {
        return new AbstractCloseableIterator<T>() {
            @Override
            public void close() throws IOException {
                closeable.close();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }
}
