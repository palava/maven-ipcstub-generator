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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.cosmocode.palava.ipc.IpcCommand;

import de.cosmocode.palava.ipc.inspector.Inspector;
import de.cosmocode.palava.ipc.inspector.InspectorException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @description Generates stub files for all found IpcCommands in the classpath.
 * @goal generate-ipcstub
 * @requiresDependencyResolution runtime
 * @author Tobias Sarnowski
 */
public class GeneratorModule extends AbstractMojo {
    private final Log LOG = getLog();

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * List of all generators the use with their configuration.
     * @parameter
     * @required
     */
    private List<Generator> generators;

    /**
     * @return the configured generators
     */
    public List<Generator> getGenerators() {
        return generators;
    }


    /**
     * Generates stub files for all found IpcCommands in the classpath.
     * @throws MojoExecutionException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File targetDirectory = new File(project.getBuild().getOutputDirectory(), "ipcstub");

        // check configurations and aggregate all required packages
        Set<String> allPackages = Sets.newHashSet();
        for (Generator generator: generators) {
            if (generator.getName() == null || generator.getName().equals("")) {
                throw new MojoFailureException("no classifier configured for configuration");
            }
            if (generator.getScheme() == null || generator.getScheme().equals("")) {
                throw new MojoFailureException("no template configured for configuration '" + generator.getName() + "'");
            }
            if (generator.getPackages() == null || generator.getPackages().size() == 0) {
                throw new MojoFailureException("no packages configured for configuration '" + generator.getName() + "'");
            }
            allPackages.addAll(generator.getPackages());
        }
        LOG.info("Searching for IpcCommands in:");
        for (String pkg: allPackages) {
            LOG.info("    " + pkg);
        }

        // search for IpcCommands in all required packages
        Set<Class<? extends IpcCommand>> foundClasses = generateCommandList(allPackages);
        LOG.info("Found " + foundClasses.size() + " IpcCommands; generating stubs...");

        // filter classes and let the generators do their work
        for (Generator generator: generators) {
            Set<Class<? extends IpcCommand>> filteredClasses = Sets.newHashSet();
            for (Class foundClass: foundClasses) {
                boolean found = false;
                for (String requiredPackage: generator.getPackages()) {
                    if (foundClass.getName().startsWith(requiredPackage + ".")) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    filteredClasses.add(foundClass);
                }
            }

            // whats the target directory?
            File stubTargetDirectory = new File(targetDirectory, generator.getName());

            // now call the generator
            generator.generate(LOG, filteredClasses, stubTargetDirectory);
        }
    }

    private Set<Class<? extends IpcCommand>> generateCommandList(Set<String> packages) throws MojoExecutionException {
        // create the classpath to use
        List<URL> locations = Lists.newArrayList();
        try {
            for (Object element : project.getRuntimeClasspathElements()) {
                final File e = new File((String)element);
                locations.add(e.toURI().toURL());
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("dependencies not resolved", e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("dependency with malformed url", e);
        }
        URL[] urls = locations.toArray(new URL[locations.size()]);

        // hack: add the required files to the classloader
        Inspector.boostrapInspector(urls);

        try {
            return Inspector.generateCommandList(packages, urls);
        } catch (InspectorException e) {
            throw new MojoExecutionException("cannot generate list of commands", e);
        }
    }
}
