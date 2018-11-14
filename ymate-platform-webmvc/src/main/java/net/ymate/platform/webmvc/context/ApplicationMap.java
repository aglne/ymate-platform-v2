/*
 * Copyright 2007-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ymate.platform.webmvc.context;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.*;


/**
 * <p>
 * ApplicationMap
 * </p>
 * <p>
 * A simple implementation of the {@link Map} interface to handle a collection of attributes and
 * init parameters in a {@link ServletContext} object. The {@link #entrySet()} method
 * enumerates over all servlet context attributes and init parameters and returns a collection of both.
 * Note, this will occur lazily - only when the entry set is asked for.
 * </p>
 */
@SuppressWarnings("rawtypes")
public class ApplicationMap extends AbstractMap implements Serializable {

    private static final long serialVersionUID = 9136809763083228202L;

    private final ServletContext context;
    private Set<Object> entries;


    /**
     * Creates a new map object given the servlet context.
     *
     * @param ctx the servlet context
     */
    public ApplicationMap(ServletContext ctx) {
        this.context = ctx;
    }


    /**
     * Removes all entries from the Map and removes all attributes from the servlet context.
     */
    @Override
    public void clear() {
        entries = null;
        Enumeration e = context.getAttributeNames();
        while (e.hasMoreElements()) {
            context.removeAttribute(e.nextElement().toString());
        }
    }

    /**
     * Creates a Set of all servlet context attributes as well as context init parameters.
     *
     * @return a Set of all servlet context attributes as well as context init parameters.
     */
    @Override
    public Set entrySet() {
        if (entries == null) {
            entries = new HashSet<Object>();
            // Add servlet context attributes
            Enumeration enumeration = context.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                final String key = enumeration.nextElement().toString();
                final Object value = context.getAttribute(key);
                entries.add(new Entry() {
                    @Override
                    public boolean equals(Object obj) {
                        if (!(obj instanceof Entry)) {
                            return false;
                        }
                        Entry entry = (Entry) obj;
                        return ((key == null) ? (entry.getKey() == null) : key.equals(entry.getKey())) && ((value == null) ? (entry.getValue() == null) : value.equals(entry.getValue()));
                    }

                    @Override
                    public int hashCode() {
                        return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
                    }

                    @Override
                    public Object getKey() {
                        return key;
                    }

                    @Override
                    public Object getValue() {
                        return value;
                    }

                    @Override
                    public Object setValue(Object obj) {
                        context.setAttribute(key, obj);
                        return value;
                    }
                });
            }
            // Add servlet context init params
            enumeration = context.getInitParameterNames();
            while (enumeration.hasMoreElements()) {
                final String key = enumeration.nextElement().toString();
                final Object value = context.getInitParameter(key);
                entries.add(new WebContext.AbstractEntry<String, Object>(key, value) {
                    @Override
                    public Object setValue(Object value) {
                        context.setAttribute(key, value);
                        return value;
                    }
                });
            }
        }
        return entries;
    }

    /**
     * Returns the servlet context attribute or init parameter based on the given key. If the
     * entry is not found, <tt>null</tt> is returned.
     *
     * @param key the entry key.
     * @return the servlet context attribute or init parameter or <tt>null</tt> if the entry is not found.
     */
    @Override
    public Object get(Object key) {
        // Try context attributes first, then init params
        // This gives the proper shadowing effects
        String keyString = key.toString();
        Object value = context.getAttribute(keyString);
        return (value == null) ? context.getInitParameter(keyString) : value;
    }

    /**
     * Sets a servlet context attribute given a attribute name and value.
     *
     * @param key   the name of the attribute.
     * @param value the value to set.
     * @return the attribute that was just set.
     */
    @Override
    public Object put(Object key, Object value) {
        Object oldValue = get(key);
        entries = null;
        context.setAttribute(key.toString(), value);
        return oldValue;
    }

    /**
     * Removes the specified servlet context attribute.
     *
     * @param key the attribute to remove.
     * @return the entry that was just removed.
     */
    @Override
    public Object remove(Object key) {
        entries = null;
        Object value = get(key);
        context.removeAttribute(key.toString());
        return value;
    }
}
