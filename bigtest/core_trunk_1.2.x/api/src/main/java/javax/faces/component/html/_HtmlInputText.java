// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.

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
package javax.faces.component.html;

import javax.faces.component.UIInput;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 *
 * Renders a HTML input element.
 * Unless otherwise specified, all attributes accept static values
 * or EL expressions.
 *
 * <h4>Events:</h4>
 * <table border="1" width="100%" cellpadding="3" summary="">
 * <tr bgcolor="#CCCCFF" class="TableHeadingColor">
 * <th align="left">Type</th>
 * <th align="left">Phases</th>
 * <th align="left">Description</th>
 * </tr>
 * <tr class="TableRowColor">
 * <td valign="top"><code>javax.faces.event.ValueChangeEvent</code></td>
 * <td valign="top" nowrap></td>
 * <td valign="top">The valueChange event is delivered when the value
                attribute is changed.</td>
 * </tr>
 * </table>
 */
@JSFComponent
(name = "h:inputText",
clazz = "javax.faces.component.html.HtmlInputText",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlInputTextTag",
defaultRendererType = "javax.faces.Text"
)
abstract class _HtmlInputText extends UIInput
    implements _AccesskeyProperty,
    _AltProperty, _UniversalProperties, _Disabled_ReadonlyProperties,
    _Focus_BlurProperties, _Change_SelectProperties, _EventProperties,
    _StyleProperties, _TabindexProperty, _LabelProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Input";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlInputText";

  /**
   * HTML: The maximum number of characters allowed to be entered.
   * 
   * @JSFProperty
   *   defaultValue = "Integer.MIN_VALUE"
   */
  public abstract int getMaxlength();

  /**
   * HTML: The initial width of this control, in characters.
   * 
   * @JSFProperty
   *   defaultValue = "Integer.MIN_VALUE"
   */
  public abstract int getSize();

  /**
   * If the value of this attribute is "off", render "off" as the value of the attribute.
   * This indicates that the browser should disable its autocomplete feature for this component.
   * This is useful for components that perform autocompletion and do not want the browser interfering.
   * If this attribute is not set or the value is "on", render nothing.
   *
   * @return  the new autocomplete value
   */
  @JSFProperty
  public abstract String getAutocomplete();

}
