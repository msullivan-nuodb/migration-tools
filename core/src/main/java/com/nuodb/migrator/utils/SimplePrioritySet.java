/**
 * Copyright (c) 2012, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.migrator.utils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nuodb.migrator.utils.Priority.NORMAL;

public class SimplePrioritySet<T> extends AbstractCollection<T> implements PrioritySet<T>, Serializable {

    public SimplePrioritySet() {
    }

    public SimplePrioritySet(PrioritySet<T> prioritySet) {
        addAll(prioritySet);
    }

    private AtomicInteger id = new AtomicInteger();
    private Set<Item<T>> items = new TreeSet<Item<T>>();

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl<T>(items.iterator());
    }

    @Override
    public boolean add(T t) {
        return add(t, NORMAL);
    }

    public boolean add(T t, int priority) {
        return items.add(new SimpleItem<T>(t, id.incrementAndGet(), priority));
    }

    public boolean addAll(PrioritySet<? extends T> list) {
        boolean modified = false;
        for (Item<? extends T> value : list.items()) {
            if (add(value.getValue(), value.getPriority())) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public Collection<Item<T>> items() {
        return items;
    }

    class IteratorImpl<T> implements Iterator<T> {

        private Iterator<Item<T>> iterator;

        public IteratorImpl(Iterator<Item<T>> iterator) {
            this.iterator = iterator;
        }

        public void remove() {
            iterator.remove();
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public T next() {
            return iterator.next().getValue();
        }
    }

    @SuppressWarnings("unchecked")
    class SimpleItem<T> implements Item<T>, Comparable<Item<T>>, Serializable {

        private final T value;
        private final int id;
        private final int priority;

        public SimpleItem(T value, int id, int priority) {
            this.value = value;
            this.id = id;
            this.priority = priority;
        }

        public int getId() {
            return id;
        }

        public T getValue() {
            return value;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(Item<T> item) {
            if (priority != item.getPriority()) {
                return item.getPriority() - priority;
            } else {
                if (value instanceof Comparable) {
                    return ((Comparable) value).compareTo(item.getValue());
                } else {
                    return ((SimpleItem)item).getId() - id;
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleItem item = (SimpleItem) o;

            if (id != item.id) return false;
            if (priority != item.priority) return false;
            if (value != null ? !value.equals(item.value) : item.value != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }
}


