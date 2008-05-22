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
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.myfaces.buildtools.maven2.plugin.builder.model.Model;
import org.apache.myfaces.buildtools.maven2.plugin.builder.utils.BuildException;
import org.apache.myfaces.buildtools.maven2.plugin.builder.utils.MyfacesUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

//import com.sun.org.apache.xerces.internal.parsers.SAXParser;

/**
 * Creates taglib (tld) and faces-config files.
 * 
 * @requiresDependencyResolution compile
 * @goal make-config
 * @phase generate-sources
 */
public class MakeConfigMojo extends AbstractMojo
{
    final Logger log = Logger.getLogger(MakeConfigMojo.class.getName());

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.directory}"
     * @readonly
     */
    private File buildDirectory;

    /**
     * Injected name of file generated by earlier run of BuildMetaDataMojo goal.
     * 
     * @parameter
     */
    private String metadataFile = "classes/META-INF/myfaces-metadata.xml";

    /**
     * @parameter
     */
    private String xmlFile = "classes/META-INF/faces-config.xml";
    
    /**
     * modelIds to be applied this goal
     * 
     * @parameter
     */
    private List modelIds;
            
    /**
     * @parameter expression="src/main/resources/META-INF"
     */
    private File templateSourceDirectory;
    
    /**
     * @parameter expression="src/main/conf/META-INF/faces-config-base.xml"
     */    
    private File xmlBaseFile;
    
    
    /**
     * @parameter expression="faces-config11.vm"
     */
    private String templateFile;
    
    /**
     * @parameter
     */
    private Map params;
        
    /**
     * Execute the Mojo.
     */
    public void execute() throws MojoExecutionException
    {
        try
        {
            if (modelIds == null){
                modelIds = new ArrayList();
                modelIds.add(project.getArtifactId());
            }
                                               
            Model model = IOUtils.loadModel(new File(buildDirectory,
                    metadataFile));
            new Flattener(model).flatten();
            generateConfigFromVelocity(model);
            //throw new MojoExecutionException("Error during config generation");
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error during config generation", e);
        }
        catch (BuildException e)
        {
            throw new MojoExecutionException("Error during config generation", e);
        }
    }
    
    private void generateConfigFromVelocity(Model model) throws IOException,
        MojoExecutionException
    {    
        VelocityEngine velocityEngine = initVelocity();

        VelocityContext baseContext = new VelocityContext();
        baseContext.put("utils", new MyfacesUtils());
        
        String baseContent = "";
        
        if (xmlBaseFile != null && xmlBaseFile.exists())
        {
            getLog().info("using base content file: "+xmlBaseFile.getPath());
            
            Reader reader = null;
            try {
                reader = new FileReader(xmlBaseFile);
                Xpp3Dom root = Xpp3DomBuilder.build(reader);
                
                StringWriter writer = new StringWriter();
                
                Xpp3Dom [] children = root.getChildren();
                
                for (int i = 0; i< children.length; i++){
                    Xpp3Dom dom = children[i];
                    Xpp3DomWriter.write(writer, dom);
                    writer.write('\n');
                }
                baseContent = writer.toString();
                writer.close();
            }
            catch (XmlPullParserException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }finally{
                reader.close();
            }
        }
        
        baseContext.put("baseContent", baseContent);
        
        baseContext.put("model", model);
        
        baseContext.put("modelIds", modelIds);
        
        if (params != null)
        {
            //Load all parameters to the context, so the template can
            //load it. This allow to generate any config file we want
            //(faces-config, tld, facelet,....)
            for (Iterator it = params.keySet().iterator(); it.hasNext();)
            {
                String key = (String) it.next();
                baseContext.put(key,params.get(key));
            }
        }
        
        Writer writer = null;
        File outFile = null;
        
        try {
            outFile = new File(buildDirectory, xmlFile);
            
            if ( !outFile.getParentFile().exists() )
            {
                outFile.getParentFile().mkdirs();
            }
            
            writer = new OutputStreamWriter(new FileOutputStream(outFile));
            
            Template template = velocityEngine.getTemplate(templateFile);
            
            template.merge(baseContext, writer);

            writer.flush();
        }
        catch (ResourceNotFoundException e)
        {
            throw new MojoExecutionException(
                    "Error merging velocity templates: " + e.getMessage(), e);
        }
        catch (ParseErrorException e)
        {
            throw new MojoExecutionException(
                    "Error merging velocity templates: " + e.getMessage(), e);
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

    private VelocityEngine initVelocity() throws MojoExecutionException
    {

        Properties p = new Properties();

        p.setProperty( "resource.loader", "file, class" );
        p.setProperty( "file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        p.setProperty( "file.resource.loader.path", templateSourceDirectory.getPath());
        p.setProperty( "class.resource.loader.class", "org.apache.myfaces.buildtools.maven2.plugin.builder.utils.RelativeClasspathResourceLoader" );
        p.setProperty( "class.resource.loader.path", "META-INF");            
        p.setProperty( "velocimacro.library", "xmlMacros.vm");
        p.setProperty( "velocimacro.permissions.allow.inline","true");
        p.setProperty( "velocimacro.permissions.allow.inline.local.scope", "true");
        p.setProperty( "directive.foreach.counter.initial.value","0");
        p.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
        "org.apache.myfaces.buildtools.maven2.plugin.builder.utils.ConsoleLogSystem" );
                        
        VelocityEngine velocityEngine = new VelocityEngine();
                
        try
        {
            velocityEngine.init(p);
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error creating VelocityEngine", e);
        }
        
        return velocityEngine;
    }
    
}
