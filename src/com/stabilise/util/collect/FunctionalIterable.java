package com.stabilise.util.collect;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Extends on {@code Iterator} for functional-ish niceness.
 * 
 * <p>Note that for a FunctionalIterable it is generally preferable to use
 * {@link #iterate(Predicate)} or {@link #forEach(Consumer)} in preference to
 * a typical iterator as implementors may be able to make optimisations which
 * would otherwise be difficult or impossible using a standard iterator
 * implementation (e.g. fast iteration of an ArrayList - see the documentation
 * for {@link java.util.RandomAccess}).
 */
@FunctionalInterface
public interface FunctionalIterable<E> extends Iterable<E> {
    
    /**
     * Iterates over all elements, removing those for which
     * <tt>pred.{@link Predicate#test(Object) test}()</tt> returns {@code
     * true}.
     * 
     * <p>The default implementation behaves as if by:
     * 
     * <pre>
     * for(Iterator<E> i = iterator(); i.hasNext();) {
     *     if(pred.test(i.next()))
     *         i.remove();
     * }</pre>
     * 
     * However, implementors are encouraged to override this if a faster
     * implementation is possible.
     * 
     * <p>This method is equivalent to {@link Collection#removeIf(Predicate)}
     * (though without the return value) - but frankly {@code removeIf} should
     * have been added to {@code Iterable} rather than {@code Collection}.
     * 
     * @throws NullPointerException if {@code pred} is {@code null}.
     */
    default void iterate(Predicate<? super E> pred) {
        Objects.requireNonNull(pred); // fail-fast
        for(Iterator<E> i = iterator(); i.hasNext();) {
            if(pred.test(i.next()))
                i.remove();
        }
    }
    
    //--------------------==========--------------------
    //------------=====Static Functions=====------------
    //--------------------==========--------------------
    
    /**
     * Wraps an {@code Iterable} object in a {@code FunctionalIterable}.
     * 
     * @throws NullPointerException if {@code itr} is {@code null}.
     */
    public static <T> FunctionalIterable<T> wrap(final Iterable<T> itr) {
        Objects.requireNonNull(itr); // fail-fast
        return () -> itr.iterator();
    }
    
}
