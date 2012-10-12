/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.os890.jsf.cc.generic;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Only needed once per project (instead of one component class for each composite component).
 *
 * DON'T USE THIS CLASS AS A SHARED LIB (IF NEEDED: CHANGE THE KEYS FOR 'getters' AND 'setters')
 */
@FacesComponent("ge.co")
public class DynamicComponent extends UINamingContainer implements Map
{
    private static Map<String, Method> getters = new HashMap<String, Method>();
    private static Map<String, Method> setters = new HashMap<String, Method>();

    static
    {
        initComponentMethods();
        getters.put("attrs", getters.get("attributes"));
    }

    public Object get(Object key)
    {
        Object defaultValue = null;
        if (key instanceof String)
        {
            Method reservedGetterMethod = getters.get(key);
            if (reservedGetterMethod != null)
            {
                try
                {
                    return reservedGetterMethod.invoke(this);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            //doesn't support an updated value in the bean
            defaultValue = getAttributes().get(key); //default passed-in via cc:attribute

            Object result = getStateHelper().eval((Serializable)key);

            if (result == null && defaultValue != null)
            {
                result = defaultValue;
                setBeanValue((String)key, defaultValue); //sync bean-value
            }

            return result;
        }
        throw new IllegalArgumentException(key + " is not of type " + String.class.getName());
    }

    public void set(String property, Object value)
    {
        getStateHelper().put(property, value);
        setBeanValue(property, value);
    }

    public Object put(Object key, Object value)
    {
        if (key instanceof String)
        {
            Method reservedSetterMethod = setters.get((String)key);

            if (reservedSetterMethod != null)
            {
                {
                    try
                    {
                        reservedSetterMethod.invoke(this, value);
                        return null;
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
            set((String)key, value);
            return null;
        }

        return getStateHelper().put((Serializable)key, value);
    }

    private static void initComponentMethods()
    {
        try
        {
            Method property;
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(UINamingContainer.class).getPropertyDescriptors())
            {
                property = propertyDescriptor.getReadMethod();

                if (property != null)
                {
                    getters.put(propertyDescriptor.getName(), property);
                }

                property = propertyDescriptor.getWriteMethod();

                if (property != null)
                {
                    setters.put(propertyDescriptor.getName(), property);
                }
            }
        }
        catch (IntrospectionException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Object remove(Object key)
    {
        return getStateHelper().remove((Serializable)key);
    }

    private void setBeanValue(String property, Object value)
    {
        //needed to update bean-values passed in via cc:attribute
        ValueExpression valueExpression = getValueExpression(property);
        if (valueExpression != null)
        {
            valueExpression.setValue(FacesContext.getCurrentInstance().getELContext(), value);
        }
    }

    public int size()
    {
        return 0;
    }

    public boolean isEmpty()
    {
        return true;
    }

    public boolean containsKey(Object key)
    {
        return get(key) != null;
    }

    public boolean containsValue(Object value)
    {
        return false;
    }

    public void putAll(Map m)
    {
        throw new IllegalStateException("not implemented");
    }

    public void clear()
    {
        throw new IllegalStateException("not implemented");
    }

    public Set keySet()
    {
        throw new IllegalStateException("not implemented");
    }

    public Collection values()
    {
        throw new IllegalStateException("not implemented");
    }

    public Set entrySet()
    {
        throw new IllegalStateException("not implemented");
    }
}
