/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.myfaces.trinidadbuild.plugin.xrts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
//import java.util.StringTokenizer;


/**
 * The <code>ListRTSWriter</code> class is an implementation of the
 * <code>RTSWriter</code> interface used to create a Sun
 * <code>ListResourceBundle</code> file.
 *
 * @version $Name:  $ ($Revision: 1.11 $) $Date: 2002/02/27 17:18:47 $
 * @since RTS 2.0
 */
public class ListRTSWriter implements RTSWriter
{

  /**
   * Empty constructor for the <code>ListResourceBundle</code> implementation
   *
   */
  public ListRTSWriter()
  {
  }

  /**
   * <code>RTSWriter</code> method implementation to write the header of the
   * <code>ListResourceBundle</code> file.
   *
   * @param parms a <code>Map</code> of command line parameters.
   * @param meta a <code>Map</code> of parsed non-resource data
   * (e.g., authors).
   */
  public void startBundle(Map parms, Map meta)
    throws Throwable
  {
    File outFile = (File)parms.get("outFile");
    String outName = (String)parms.get("outName");
    String srcName = (String)parms.get("srcName");

    Boolean b = (Boolean)parms.get("quietMode");
    boolean quietMode = b.booleanValue();
    if (!quietMode)
    {
      System.out.println("  " + outFile);
    }

    _pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));

    _pw.println("// Do not edit this file!");
    _pw.println("// This file has been automatically generated.");

    String rtsFileType = "xrts";
    if (meta != null)
      rtsFileType = (String)meta.get("fileType");
    _pw.println("// Edit " + srcName + "." + rtsFileType + " and run the " +
      rtsFileType.toUpperCase() + "MakeBundle tool instead.");
    _pw.println("// ");

    String packageName = (String)parms.get("pkgName");
    boolean validPackage = false;
    if (packageName == null)
      if (meta != null)
        if (meta.get("package") != null)
          packageName = (String)meta.get("package");

    if (packageName != null &&
      (!packageName.equals("") || !packageName.trim().equals("")))
    {
      _pw.println("package " + packageName + ";");
      _pw.println("");
      validPackage = true;
    }

    _pw.println("import java.util.ListResourceBundle;");

    writeImports(parms, meta);

    _pw.println("");
    _pw.println("public class " + outName + " extends ListResourceBundle {");
    _pw.println("  @Override");
    _pw.println("  public Object[][] getContents() {");
    _pw.println("    return new Object[][] {");

    if (outName.indexOf('_') < 0)
    {
      // This is the base bundle. If any default locales are requested,
      // they'll extend this bundle. Create them here because we have all
      // the needed info.
      String[] locales = (String[]) parms.get("defaultLocales");
      File targDir = outFile.getParentFile();
      if ((locales != null) && (targDir != null))
      {
        for (int l = 0; l < locales.length; l++)
        {
          String cName = outName + '_' + locales[l];
          File locF = new File(targDir, cName + ".java");
          
          PrintWriter locWri; 
          locWri = new PrintWriter(new BufferedWriter(new FileWriter(locF)));

          locWri.println("// Do not edit this file!");
          locWri.println("// This file has been automatically generated.");

          if (validPackage)
          {
            locWri.println("package " + packageName + ";");
            locWri.println("");
          }
          locWri.println("public class " + cName + " extends " + outName);
          locWri.println("{");
          locWri.println("}");
          locWri.close();
        }
      }
    }
    
  }

  protected void writeImports(Map parms, Map meta)
     throws Throwable
  {
  }

  public void writeString(Map parms, Map meta, String key,
    String value) throws Throwable
  {
    _pw.println("    {\"" + UnicodeEscapes.convert(key) + "\", \"" +
                UnicodeEscapes.convert(value) + "\"},");
  }


  /**
   * <code>RTSWriter</code> method implementation to close the file stream
   * required for the <code>ListResourceBundle</code>.  Before closing, this
   * method also writes the footer portions of the file.  The footer portions
   * consist of little more than closing braces.
   *
   * @param meta a <code>Map</code> of parsed non-resource data
   * (e.g., authors).
   */
  public void endBundle(Map parms, Map meta) throws Throwable
  {
    _pw.println("    };");
    _pw.println("  }");
    _pw.println("}");
    _pw.close();
  }

  protected PrintWriter getOut()
  {
    return _pw;
  }

  private PrintWriter _pw;
}
