/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cosmocode.palava.maven.ipcstub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.collect.Maps;

import de.cosmocode.palava.ipc.IpcCommand;

/**
 * A configured stub generator.
 * @author Tobias Sarnowski
 */
public class Generator implements LogChute {
    
    private Log log; 

    /**
     * A configuration unique identifier.
     * @parameter
     * @required
     */
    private String name;

    /**
     * The scheme to use, e.g. "php"
     * @paramter
     * @required
     */
    private String scheme;

    /**
     * List of all packages to search commands in.
     * @parameter
     * @required
     */
    private List<String> packages;

    /**
     * Map of aliases to generate within the stub.
     * @parameter
     */
    private Map<String, String> aliases;

    /**
     * Where to store the outputed files.
     * @parameter
     */
    private String target;

    /**
     * A coypright notice or something else to include in the stub.
     * @parameter
     */
    private String legalText;

    // use to know the common generation date;
    private Date generationDate;

    // the engine instance;
    private VelocityEngine engine;

    // will be set on generate()
    private File targetDirectory;

    // will be generated on generate()
    private Set<GenPackage> rootPackages;

    public String getName() {
        return name;
    }

    public String getScheme() {
        return scheme;
    }

    public List<String> getPackages() {
        return packages;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public String getTarget() {
        return target;
    }

    public String getLegalText() {
        return legalText;
    }

    public Set<GenPackage> getRootPackages() {
        return rootPackages;
    }

    private String getResourcePath(String resource) {
        return "/ipcstub/" + scheme + "/" + resource + ".vm";
    }

    public String getGenerationDate() {
        return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(generationDate);
    }
    
    /**
     * Checks whether this generator contains a valid name, scheme and packages.
     *
     * @throws MojoFailureException if check failed
     */
    public void check() throws MojoFailureException {
        if (StringUtils.isBlank(getName())) {
            throw new MojoFailureException("no classifier configured for configuration");
        }
        if (StringUtils.isBlank(getScheme())) {
            throw new MojoFailureException("no template configured for configuration '" + getName() + "'");
        }
        if (getPackages() == null || getPackages().size() == 0) {
            throw new MojoFailureException("no packages configured for configuration '" + getName() + "'");
        }
    }

    /**
     * Generates the stub files with the given list of IpcCommand classes.
     * 
     * @param currentLog the maven logger
     * @param classes all requested IpcCommands
     * @param directory the target directory
     * @throws MojoExecutionException if execution failed
     * @throws MojoFailureException if any fatal error occured
     */
    protected void generate(Log currentLog, Set<Class<? extends IpcCommand>> classes, File directory) 
        throws MojoExecutionException, MojoFailureException {
        
        this.log = currentLog;
        this.targetDirectory = target == null ? directory : new File(target); 
        this.generationDate = new Date();

        // initialize the Velocity engine
        engine = new VelocityEngine();
        engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);
        engine.setProperty(VelocityEngine.RESOURCE_LOADER, "class");
        engine.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        
        try {
            engine.init();
        /* CHECKSTYLE:OFF */
        } catch (Exception e) {
        /* CHECKSTYLE:ON */
            throw new MojoExecutionException("cannot initialize velocity engine", e);
        }

        // build up tree and informations
        rootPackages = GenPackage.getFirstPackages(classes, null);

        // find the scheme to use
        final String templatePath = getResourcePath("main");
        
        final Template template;
        
        try {
            template = engine.getTemplate(templatePath);
        /* CHECKSTYLE:OFF */
        } catch (Exception e) {
        /* CHECKSTYLE:ON */
            throw new MojoFailureException("cannot find scheme " + scheme, e);
        }

        // initialize the context
        final VelocityContext ctx = new VelocityContext();
        ctx.put("generator", this);

        // create the target directory
        if (!directory.exists() && !directory.mkdirs()) {
            throw new MojoExecutionException("cannot create stub directory: " + directory);
        }

        // start the generation process within the scheme
        final StringWriter writer = new StringWriter();
        try {
            // scheme have to be in UTF-8
            template.merge(ctx, writer);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot merge template", e);
        }
    }

    @Override
    public void init(RuntimeServices runtimeServices) throws Exception {
        // nothing to do
    }

    @Override
    public void log(int i, String s) {
        switch (i) {
            case LogChute.TRACE_ID:
            case LogChute.DEBUG_ID:
                log.debug(s);
                break;
            case LogChute.INFO_ID:
                log.info(s);
                break;
            case LogChute.WARN_ID:
                log.warn(s);
                break;
            case LogChute.ERROR_ID:
                log.error(s);
                break;
            default: 
        }
    }

    @Override
    public void log(int i, String s, Throwable throwable) {
        switch (i) {
            case LogChute.TRACE_ID:
            case LogChute.DEBUG_ID:
                log.debug(s, throwable);
                break;
            case LogChute.INFO_ID:
                log.info(s, throwable);
                break;
            case LogChute.WARN_ID:
                log.warn(s, throwable);
                break;
            case LogChute.ERROR_ID:
                log.error(s, throwable);
                break;
            default: 
        }
    }

    @Override
    public boolean isLevelEnabled(int i) {
        switch (i) {
            case LogChute.TRACE_ID:
            case LogChute.DEBUG_ID:
                return log.isDebugEnabled();
            case LogChute.INFO_ID:
                return log.isInfoEnabled();
            case LogChute.WARN_ID:
                return log.isWarnEnabled();
            case LogChute.ERROR_ID:
                return log.isErrorEnabled();
            default:
                return false;
        }
    }

    /**
     * Generates a file.
     *
     * @param generatedFileName the file name
     * @param templateFile the template file
     * @param args the arguments
     * @throws MojoExecutionException if execution failed
     * @throws MojoFailureException if any fatal error occured
     */
    // used by templates
    public void generateFile(String generatedFileName, String templateFile, Object args) 
        throws MojoExecutionException, MojoFailureException {
        
        final File generatedFile = new File(targetDirectory, generatedFileName);
        final File parent = new File(generatedFile.getParent());
        parent.mkdirs();

        Template tpl = null;
        try {
            tpl = engine.getTemplate(getResourcePath(templateFile));
        /* CHECKSTYLE:OFF */
        } catch (Exception e) {
        /* CHECKSTYLE:ON */
            throw new MojoExecutionException("cannot load template " + templateFile, e);
        }

        final VelocityContext ctx = new VelocityContext();
        ctx.put("generator", this);
        ctx.put("args", args);

        final FileWriter w;
        try {
            w = new FileWriter(generatedFile);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot create file " + generatedFile, e);
        }
        try {
            log.info("Generating " + generatedFile + "...");
            tpl.merge(ctx, w);
            w.close();
        } catch (IOException e) {
            throw new MojoExecutionException("cannot merge template" + templateFile, e);
        }
    }

    /**
     * Includes another file.
     *
     * @param templateFile the template file
     * @return the included file
     * @throws MojoExecutionException if execution failed
     */
    // used by templates
    public String includeFile(String templateFile) throws MojoExecutionException {
        final Template template;
        
        try {
            template = engine.getTemplate(getResourcePath(templateFile));
        /* CHECKSTYLE:OFF */
        } catch (Exception e) {
        /* CHECKSTYLE:ON */
            throw new MojoExecutionException("cannot load template " + templateFile, e);
        }

        final VelocityContext ctx = new VelocityContext();
        ctx.put("generator", this);

        final StringWriter writer = new StringWriter();
        
        try {
            template.merge(ctx, writer);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot merge template", e);
        }
        
        return writer.toString();
    }
    
}
