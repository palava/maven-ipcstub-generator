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

package de.cosmocode.palava.ipc.connector.json.php.maven;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import de.cosmocode.palava.ipc.IpcCommand;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @description Generates PHP stub files for all found IpcCommands in the classpath.
 * @goal generate-php-stubs
 * @requiresDependencyResolution runtime
 * @author Tobias Sarnowski
 */
public class PhpStubGenerator extends AbstractMojo {
    private final Log LOG = getLog();

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * List of all packages to search for {@link IpcCommand}s.
     * @parameter
     * @required
     */
    private List<String> packages;

    /**
     * Map of all aliases to generate built-in.
     * @parameter
     */
    private Map<String,String> aliases;

    /**
     * Generate a zip file from generated php stub files.
     * NOT YET IMPLEMENTED
     * @parameter default-value=false
     */
    private boolean generateZip;


    /**
     * Set a legal text to mark the PHP scripts.
     * @parameter
     */
    private String legalText;

    // internal set of all found classes
    private Set<Class<?>> commands;

    // a classloader to parse the classes
    private ClassLoader classLoader;

    // used to compare the IpcCommand classes
    private Class ipcCommandClass;

    /**
     * Generate PHP stubs from IpcCommand definitions.
     * @throws MojoExecutionException
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (packages == null || packages.size() == 0) {
            throw new MojoExecutionException("no packages configured to search in");
        }
        LOG.info("Packages to search in:");
        for (String pkg: packages) {
            LOG.info("    " + pkg);
        }

        try {
            LOG.info("Searching for IpcCommands...");
            findCommands();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("cannot resolve dependencies", e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("cannot create classloader", e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("cannot find basic IpcCommand class", e);
        }

        if (aliases == null || aliases.size() == 0) {
            LOG.info("No aliases configured for the stub generation.");
        } else {
            LOG.info("Aliases to generate built-in:");
            for (Map.Entry<String,String> alias: aliases.entrySet()) {
                LOG.info("    " + alias.getKey() + " -> " + alias.getValue());
            }
        }

        if (generateZip) {
            LOG.error("generating zip file from generated source not yet implemented!");
        }
    }

    public void findCommands() throws DependencyResolutionRequiredException, MalformedURLException, ClassNotFoundException {
        LOG.info("Searching for IpcCommands in classpath...");

        // create the classpath to use
        List<URL> locations = Lists.newArrayList();
        for (Object element : project.getRuntimeClasspathElements()) {
            final File e = new File((String)element);
            locations.add(e.toURI().toURL());
        }
        URL[] urls = locations.toArray(new URL[locations.size()]);

        classLoader = new URLClassLoader(urls);
        ipcCommandClass = classLoader.loadClass(IpcCommand.class.getName());

        // split the classpath elements
        commands = Sets.newHashSet();
        for (URL url: urls) {
            File e = new File(url.getFile());
            if (e.isFile()) {
                parseJar(e, commands);
            } else {
                parseDir(e, e, commands);
            }
        }
    }

    private void parseDir(File root, File dir, Set commands) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                parseDir(root, file, commands);
            } else {
                final String path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
                if (path.endsWith(".class")) {
                    final String className = path.substring(0, path.length() - ".class".length()).replace('/', '.');
                    checkAndAdd(className, commands);
                }
            }
        }
    }

    private void parseJar(File jarFile, Set commands) {
        try {
            final JarInputStream jar = new JarInputStream(new FileInputStream(jarFile));

            while (true) {
                final JarEntry jarEntry = jar.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                if (jarEntry.getName().endsWith(".class")) {
                    final String className = jarEntry.getName().substring(
                        0, jarEntry.getName().length() - ".class".length()
                    ).replace('/', '.');
                    checkAndAdd(className, commands);
                }
            }
        } catch (IOException e) {
            LOG.warn("failed to read jar file", e);
        }
    }

    private void checkAndAdd(String clsName, Set commands) {
        boolean found = false;
        for (String pkg : packages) {
            if (clsName.startsWith(pkg + ".")) {
                found = true;
                break;
            }
        }
        if (!found) {
            return;
        }
        final Class cls;
        try {
            cls = classLoader.loadClass(clsName);
        } catch (ClassNotFoundException e) {
            return;
        }
        if (ipcCommandClass.isAssignableFrom(cls)) {
            if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()) || cls.isArray() || cls.isEnum()) {
                return;
            }
            LOG.info("    " + cls.getName());
            commands.add(cls);
        }
    }
}
