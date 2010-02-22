/**
 * palava - a java-php-bridge
 * Copyright (C) 2007-2010  CosmoCode GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package de.cosmocode.palava.maven.ipcstub;

import com.google.common.collect.Maps;
import de.cosmocode.palava.ipc.IpcCommand;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A configured stub generator.
 * @author Tobias Sarnowski
 */
public class Generator implements LogChute {
    private Log LOG;

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
    private Map<String,String> aliases;

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

    /**
     * Encoding to use.
     * @parameter
     */
    private String encoding;

    // use to know the common generation date;
    private Date generationDate;


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

    // the engine instance;
    private VelocityEngine engine;

    // will be set on generate()
    private File targetDirectory;

    // will be generated on generate()
    private Set<GenPackage> rootPackages;

    // list of temporary files to cleanup
    private Map<String,Template> templates = Maps.newHashMap();


    public Set<GenPackage> getRootPackages() {
        return rootPackages;
    }

    private String getResourcePath(String resource) {
        return "/ipcstub/" + scheme + "/" + resource + ".vm";
    }

    public String getGenerationDate() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        return format.format(generationDate);
    }

    /**
     * Generates the stub files with the given list of IpcCommand classes.
     * @param log the maven logger
     * @param classes all requested IpcCommands
     */
    protected void generate(Log log, Set<Class<? extends IpcCommand>> classes, File targetDirectory) throws MojoExecutionException, MojoFailureException {
        LOG = log;
        if (target == null) {
            this.targetDirectory = targetDirectory;
        } else {
            this.targetDirectory = new File(target);
        }
        generationDate = new Date();

        // initialize the Velocity engine
        engine = new VelocityEngine();
        engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);
        engine.setProperty(VelocityEngine.RESOURCE_LOADER, "class");
        engine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        try {
            engine.init();
        } catch (Exception e) {
            throw new MojoExecutionException("cannot initialize velocity engine", e);
        }

        // build up tree and informations
        rootPackages = GenPackage.getFirstPackages(classes, null);

        // find the scheme to use
        String templatePath = getResourcePath("main");
        Template tpl = null;
        try {
            tpl = engine.getTemplate(templatePath);
        } catch (Exception e) {
            throw new MojoFailureException("cannot find scheme " + scheme, e);
        }

        // initialize the context
        VelocityContext ctx = new VelocityContext();
        ctx.put("generator", this);

        // create the target directory
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw new MojoExecutionException("cannot create stub directory: " + targetDirectory);
        }

        // start the generation process within the scheme
        StringWriter w = new StringWriter();
        try {
            tpl.merge(ctx, w);  // scheme have to be in UTF-8
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
                LOG.debug(s);
                break;
            case LogChute.INFO_ID:
                LOG.info(s);
                break;
            case LogChute.WARN_ID:
                LOG.warn(s);
                break;
            case LogChute.ERROR_ID:
                LOG.error(s);
                break;
        }
    }

    @Override
    public void log(int i, String s, Throwable throwable) {
        switch (i) {
            case LogChute.TRACE_ID:
            case LogChute.DEBUG_ID:
                LOG.debug(s, throwable);
                break;
            case LogChute.INFO_ID:
                LOG.info(s, throwable);
                break;
            case LogChute.WARN_ID:
                LOG.warn(s, throwable);
                break;
            case LogChute.ERROR_ID:
                LOG.error(s, throwable);
                break;
        }
    }

    @Override
    public boolean isLevelEnabled(int i) {
        switch (i) {
            case LogChute.TRACE_ID:
            case LogChute.DEBUG_ID:
                return LOG.isDebugEnabled();
            case LogChute.INFO_ID:
                return LOG.isInfoEnabled();
            case LogChute.WARN_ID:
                return LOG.isWarnEnabled();
            case LogChute.ERROR_ID:
                return LOG.isErrorEnabled();
            default:
                return false;
        }
    }

    public void generateFile(String generatedFileName, String templateFile, Object args) throws MojoExecutionException, MojoFailureException {
        File generatedFile = new File(targetDirectory, generatedFileName);
        File parent = new File(generatedFile.getParent());
        if (!parent.exists()) {
            parent.mkdirs();
        }

        Template tpl = null;
        try {
            tpl = engine.getTemplate(getResourcePath(templateFile));
        } catch (Exception e) {
            throw new MojoExecutionException("cannot load template " + templateFile, e);
        }

        VelocityContext ctx = new VelocityContext();
        ctx.put("generator", this);
        ctx.put("args", args);

        final FileWriter w;
        try {
            w = new FileWriter(generatedFile);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot create file " + generatedFile, e);
        }
        try {
            LOG.info("Generating " + generatedFile + "...");
            tpl.merge(ctx, w);
            w.close();
        } catch (IOException e) {
            throw new MojoExecutionException("cannot merge template" + templateFile, e);
        }
    }

    public String includeFile(String templateFile) throws MojoExecutionException {
        Template tpl = null;
        try {
            tpl = engine.getTemplate(getResourcePath(templateFile));
        } catch (Exception e) {
            throw new MojoExecutionException("cannot load template " + templateFile, e);
        }

        VelocityContext ctx = new VelocityContext();
        ctx.put("generator", this);

        StringWriter w = new StringWriter();
        try {
            tpl.merge(ctx, w);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot merge template", e);
        }
        return w.toString();
    }
}
