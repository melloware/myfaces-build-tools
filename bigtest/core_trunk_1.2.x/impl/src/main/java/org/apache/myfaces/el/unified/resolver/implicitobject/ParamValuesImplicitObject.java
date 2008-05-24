/*
 * Copyright 2006 The Apache Software Foundation.
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

package org.apache.myfaces.el.unified.resolver.implicitobject;

import java.beans.FeatureDescriptor;
import java.util.Map;
import javax.el.ELContext;

/**
 * Encapsulates information needed by the ImplicitObjectResolver
 *
 * @author Stan Silvert
 */
public class ParamValuesImplicitObject extends ImplicitObject {
    
    private static final String NAME = "paramValues".intern();
    
    /** Creates a new instance of ParamValuesImplicitObject */
    public ParamValuesImplicitObject() {
    }

    public Object getValue(ELContext context) {
        return externalContext(context).getRequestParameterValuesMap();
    }

    public String getName() {
        return NAME;
    }
    
    public Class getType() {
        return null;
    }

    public FeatureDescriptor getDescriptor() {
        return makeDescriptor(NAME, 
                             "Map whose keys are a set of request param names and whose values (type String[]) are all values for each name in the request", 
                             Map.class);
    }
    
}
