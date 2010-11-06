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
package org.apache.myfaces.buildtools.maven2.plugin.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.myfaces.buildtools.maven2.plugin.builder.model.Model;
import org.apache.myfaces.buildtools.maven2.plugin.builder.model.ConverterMeta;
import org.apache.myfaces.buildtools.maven2.plugin.builder.utils.BuildException;
import org.apache.myfaces.buildtools.maven2.plugin.builder.utils.MavenPluginConsoleLogSystem;
import org.apache.myfaces.buildtools.maven2.plugin.builder.utils.MyfacesUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.thoughtworks.qdox.JavaDocBuilder;

/**
 * Maven goal to generate java source code for Converter classes.
 * 
 * <p>It uses velocity to generate templates, and has the option to define custom templates.</p>
 * <p>The executed template has the following variables available to it:</p>
 * <ul>
 *  <li>utils : Returns an instance of 
 *  org.apache.myfaces.buildtools.maven2.plugin.builder.utils.MyfacesUtils, 
 *  it contains some useful methods.</li>
 *  <li>converter : Returns the current instance of
 *   org.apache.myfaces.buildtools.maven2.plugin.builder.model.ConverterMeta</li>
 * </ul>
 * 
 * 
 * @since 1.0.8
 * @version $Id: MakeConvertersMojo.java 942970 2010-05-11 00:36:14Z lu4242 $
 * @requiresDependencyResolution compile
 * @goal make-converters
 * @phase generate-sources
 */
public class MakeConvertersMojo extends AbstractMojo
{
    /**
     * Injected Maven project.
     * 
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * Defines the directory where the metadata file (META-INF/myfaces-metadata.xml) is loaded.
     * 
     * @parameter expression="${project.build.directory}/generated-resources/myfaces-builder-plugin"
     * @readonly
     */
    private File buildDirectory;

    /**
     * Injected name of file generated by earlier run of BuildMetaDataMojo goal.
     * 
     * @parameter
     */
    private String metadataFile = "META-INF/myfaces-metadata.xml";

    /**
     * The directory used to load templates into velocity environment.
     * 
     * @parameter expression="src/main/resources/META-INF"
     */
    private File templateSourceDirectory;

    /**
     * The directory where all generated files are created. This directory is added as a
     * compile source root automatically like src/main/java is. 
     * 
     * @parameter expression="${project.build.directory}/generated-sources/myfaces-builder-plugin"
     */
    private File generatedSourceDirectory;

    /**
     * Only generate tag classes that contains that package prefix
     * 
     * @parameter
     */
    private String packageContains;

    /**
     *  Log and continue execution when generating converter classes.
     *  <p>
     *  If this property is set to false (default), errors when a converter class is generated stops
     *  execution immediately.
     *  </p>
     * 
     * @parameter
     */
    private boolean force;

    /**
     * Defines the jsf version (1.1 or 1.2), used to take the default templates for each version.
     * <p> 
     * If version is 1.1, the default templateConverterName is 'converterClass11.vm' and if version
     * is 1.2 the default templateConverterName is 'converterClass12.vm'.
     * </p>
     * 
     * @parameter
     */
    private String jsfVersion;
    
    /**
     * Define the models that should be included when generate converter classes. If not set, the
     * current model identified by the artifactId is used.
     * <p>
     * Each model built by build-metadata goal has a modelId, that by default is the artifactId of
     * the project. Setting this property defines which objects tied in a specified modelId should
     * be taken into account.  
     * </p>
     * <p>In this case, limit converter tag generation only to the components defined in the models 
     * identified by the modelId defined. </p>
     * <p>This is useful when you need to generate files that take information defined on other
     * projects.</p>
     * <p>Example:</p>
     * <pre>
     *    &lt;modelIds&gt;
     *        &lt;modelId>model1&lt;/modelId&gt;
     *        &lt;modelId>model2&lt;/modelId&gt;
     *    &lt;/modelIds&gt;
     * </pre>
     * 
     * @parameter
     */
    private List modelIds;

    /**
     * The name of the template used to generate converter classes. According to the value on 
     * jsfVersion property the default if this property is not set could be converterClass11.vm (1.1) or
     * converterClass12.vm (1.2)
     * 
     * @parameter 
     */
    private String templateConverterName;
    
    /**
     * This param is used to search in this folder if some file to
     * be generated exists and avoid generation and duplicate exception.
     * 
     * @parameter expression="src/main/java"
     */    
    private File mainSourceDirectory;
    
    /**
     * This param is used to search in this folder if some file to
     * be generated exists and avoid generation and duplicate exception.
     * 
     * @parameter
     */        
    private File mainSourceDirectory2;

    /**
     * Execute the Mojo.
     */
    public void execute() throws MojoExecutionException
    {
        // This command makes Maven compile the generated source:
        // getProject().addCompileSourceRoot( absoluteGeneratedPath.getPath() );
        
        try
        {
            project.addCompileSourceRoot( generatedSourceDirectory.getCanonicalPath() );
            
            if (modelIds == null)
            {
                modelIds = new ArrayList();
                modelIds.add(project.getArtifactId());
            }
            Model model = IOUtils.loadModel(new File(buildDirectory,
                    metadataFile));
            new Flattener(model).flatten();
            generateConverters(model);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error generating converters", e);
        }
        catch (BuildException e)
        {
            throw new MojoExecutionException("Error generating converters", e);
        }
    }
    
    
    private VelocityEngine initVelocity() throws MojoExecutionException
    {
        File template = new File(templateSourceDirectory, _getTemplateName());
        
        if (template.exists())
        {
            getLog().info("Using template from file loader: "+template.getPath());
        }
        else
        {
            getLog().info("Using template from class loader: META-INF/"+_getTemplateName());
        }
                
        VelocityEngine velocityEngine = new VelocityEngine();
                
        try
        {
            velocityEngine.setProperty( "resource.loader", "file, class" );
            velocityEngine.setProperty( "file.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            velocityEngine.setProperty( "file.resource.loader.path", templateSourceDirectory.getPath());
            velocityEngine.setProperty( "class.resource.loader.class",
                    "org.apache.myfaces.buildtools.maven2.plugin.builder.utils.RelativeClasspathResourceLoader" );
            velocityEngine.setProperty( "class.resource.loader.path", "META-INF");            
            velocityEngine.setProperty( "velocimacro.library", "converterClassMacros11.vm");
            velocityEngine.setProperty( "velocimacro.permissions.allow.inline","true");
            velocityEngine.setProperty( "velocimacro.permissions.allow.inline.local.scope", "true");
            velocityEngine.setProperty( "directive.foreach.counter.initial.value","0");
            //velocityEngine.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
            //    "org.apache.myfaces.buildtools.maven2.plugin.builder.utils.ConsoleLogSystem" );
            
            velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM,
                    new MavenPluginConsoleLogSystem(this.getLog()));

            velocityEngine.init();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error creating VelocityEngine", e);
        }
        
        return velocityEngine;
    }
    

    /**
     * Generates parsed converters.
     */
    private void generateConverters(Model model) throws IOException,
            MojoExecutionException
    {
        // Make sure generated source directory 
        // is added to compilation source path 
        //project.addCompileSourceRoot(generatedSourceDirectory.getCanonicalPath());
        
        //Init Qdox for extract code 
        JavaDocBuilder builder = new JavaDocBuilder();
        
        List sourceDirs = project.getCompileSourceRoots();
        
        // need a File object representing the original source tree
        for (Iterator i = sourceDirs.iterator(); i.hasNext();)
        {
            String srcDir = (String) i.next();
            builder.addSourceTree(new File(srcDir));
        }        
        
        //Init velocity
        VelocityEngine velocityEngine = initVelocity();

        VelocityContext baseContext = new VelocityContext();
        baseContext.put("utils", new MyfacesUtils());
        
        for (Iterator it = model.getConverters().iterator(); it.hasNext();)
        {
            ConverterMeta converter = (ConverterMeta) it.next();
            
            if (converter.getClassName() != null)
            {
                File f = new File(mainSourceDirectory, StringUtils.replace(
                    converter.getClassName(), ".", "/")+".java");
                                
                if (!f.exists() && canGenerateConverter(converter))
                {
                    if (mainSourceDirectory2 != null)
                    {
                        File f2 = new File(mainSourceDirectory2, StringUtils.replace(
                                converter.getClassName(), ".", "/")+".java");
                        if (f2.exists())
                        {
                            //Skip
                            continue;
                        }
                    }
                    getLog().info("Generating converter class:"+converter.getClassName());
                    try
                    {
                        _generateConverter(velocityEngine, builder,converter,baseContext);
                    }
                    catch(MojoExecutionException e)
                    {
                        if (force)
                        {
                            getLog().error(e.getMessage());
                        }
                        else
                        {
                            //Stop execution throwing exception
                            throw e;
                        }
                    }
                }
            }
        }        
    }
    
    public boolean canGenerateConverter(ConverterMeta converter)
    {
        if ( modelIds.contains(converter.getModelId())
                && includePackage(converter))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public boolean includePackage(ConverterMeta converter)
    {
        if (packageContains != null)
        {
            if (MyfacesUtils.getPackageFromFullClass(converter.getClassName()).startsWith(packageContains))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return true;
        }        
    }
    
    
    /**
     * Generates a parsed converter.
     * 
     * @param converter
     *            the parsed converter metadata
     */
    private void _generateConverter(VelocityEngine velocityEngine,
            JavaDocBuilder builder,
            ConverterMeta converter, VelocityContext baseContext)
            throws MojoExecutionException
    {
        Context context = new VelocityContext(baseContext);
        context.put("converter", converter);
        
        Writer writer = null;
        File outFile = null;

        try
        {
            outFile = new File(generatedSourceDirectory, StringUtils.replace(
                    converter.getClassName(), ".", "/")+".java");

            if ( !outFile.getParentFile().exists() )
            {
                outFile.getParentFile().mkdirs();
            }

            writer = new OutputStreamWriter(new FileOutputStream(outFile));

            Template template = velocityEngine.getTemplate(_getTemplateName());
                        
            template.merge(context, writer);

            writer.flush();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException(
                    "Error merging velocity templates: " + e.getMessage(), e);
        }
        finally
        {
            IOUtil.close(writer);
            writer = null;
        }
    }
                
    private String _getTemplateName()
    {
        if (templateConverterName == null)
        {
            if (_is12() || _is20())
            {
                return "converterClass12.vm";
            }
            else
            {
                return "converterClass11.vm";
            }
        }
        else
        {
            return templateConverterName;
        }
    }
    
    private boolean _is12()
    {
        return "1.2".equals(jsfVersion) || "12".equals(jsfVersion);
    }


    private boolean _is20()
    {
        return "2.0".equals(jsfVersion) || "20".equals(jsfVersion);
    }

}
