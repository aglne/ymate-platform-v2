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

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


/**
 * <p>
 * RequestMap
 * </p>
 * <p>
 * A simple implementation of the {@link java.util.Map} interface to handle a collection of request attributes.
 * </p>
 */
@SuppressWarnings("rawtypes")
public class RequestMap extends AbstractMap implements Serializable {

    private static final long serialVersionUID = -7675640869293787926L;

    private Set<Object> entries;
    private final HttpServletRequest request;


    /**
     * Saves the request to use as the backing for getting and setting values
     *
     * @param request the http servlet request.
     */
    public RequestMap(final HttpServletRequest request) {
        this.request = request;
    }


    /**
     * Removes all attributes from the request as well as clears entries in this map.
     */
    @Override
    public void clear() {
        entries = null;
        Enumeration keys = request.getAttributeNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            request.removeAttribute(key);
        }
    }

    /**
     * Returns a Set of attributes from the http request.
     *
     * @return a Set of attributes from the http request.
     */
    @Override
    public Set entrySet() {
        if (entries == null) {
            entries = new HashSet<Object>();
            Enumeration enumeration = request.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                final String key = enumeration.nextElement().toString();
                entries.add(new WebContext.AbstractEntry<String, Object>(key, request.getAttribute(key)) {
                    @Override
                    public Object setValue(Object value) {
                        request.setAttribute(key, value);
                        return value;
                    }
                });
            }
        }
        return entries;
    }

    /**
     * Returns the request attribute associated with the given key or <tt>null</tt> if it doesn't exist.
     *
     * @param key the name of the request attribute.
     * @return the request attribute or <tt>null</tt> if it doesn't exist.
     */
    @Override
    public Object get(Object key) {
        return request.getAttribute(key.toString());
    }

    /**
     * Saves an attribute in the request.
     *
     * @param key   the name of the request attribute.
     * @param value the value to set.
     * @return the object that was just set.
     */
    @Override
    public Object put(Object key, Object value) {
        Object oldValue = get(key);
        entries = null;
        request.setAttribute(key.toString(), value);
        return oldValue;
    }

    /**
     * Removes the specified request attribute.
     *
     * @param key the name of the attribute to remove.
     * @return the value that was removed or <tt>null</tt> if the value was not found (and hence, not removed).
     */
    @Override
    public Object remove(Object key) {
        entries = null;
        Object value = get(key);
        request.removeAttribute(key.toString());
        return value;
    }
}
