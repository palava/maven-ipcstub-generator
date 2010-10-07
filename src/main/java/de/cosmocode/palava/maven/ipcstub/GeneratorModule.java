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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gag.annotation.remark.OhNoYouDidnt;

import de.cosmocode.commons.reflect.Classpath;
import de.cosmocode.commons.reflect.Reflection;
import de.cosmocode.palava.ipc.IpcCommand;

/* CHECKSTYLE:OFF */
/**
 * Generates stub files for all found IpcCommands in the classpath.
 * 
 * @description Generates stub files for all found IpcCommands in the classpath.
 * @goal generate-ipcstub
 * @requiresDependencyResolution runtime
 * @author Tobias Sarnowski
 */
public class GeneratorModule extends AbstractMojo {
/* CHECKSTYLE:ON */
    
    private final Log log = getLog();

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * List of all generators the use with their configuration.
     * 
     * @parameter
     * @required
     */
    private List<Generator> generators;

    /**
     * The generators.
     * 
     * @return the configured generators
     */
    public List<Generator> getGenerators() {
        return generators;
    }

    /**
     * Generates stub files for all found IpcCommands in the classpath.
     *
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final File targetDirectory = new File(project.getBuild().getOutputDirectory(), "ipcstub");

        // check configurations and aggregate all required packages
        final Set<String> allPackages = Sets.newHashSet();
        for (Generator generator : generators) {
            generator.check();
            allPackages.addAll(generator.getPackages());
        }

        log.info("Searching for IpcCommands in:");
        for (String pkg : allPackages) {
            log.info("    " + pkg);
        }

        // search for IpcCommands in all required packages
        final Set<Class<? extends IpcCommand>> foundClasses = Sets.newTreeSet(Reflection.orderByName());
        foundClasses.addAll(generateCommandList(allPackages));

        log.info("Found " + foundClasses.size() + " IpcCommands; generating stubs...");

        // filter classes and let the generators do their work
        for (Generator generator : generators) {
            final Set<Class<? extends IpcCommand>> filteredClasses = Sets.newLinkedHashSet();
            for (Class<? extends IpcCommand> foundClass : foundClasses) {
                for (String requiredPackage : generator.getPackages()) {
                    if (foundClass.getName().startsWith(requiredPackage + ".")) {
                        filteredClasses.add(foundClass);
                        break;
                    }
                }
            }

            // whats the target directory?
            final File stubTargetDirectory = new File(targetDirectory, generator.getName());

            // now call the generator
            generator.generate(log, filteredClasses, stubTargetDirectory);
        }
    }

    private Collection<Class<? extends IpcCommand>> generateCommandList(Set<String> packages) 
        throws MojoExecutionException {
        
        // create the classpath to use
        final List<File> locations = Lists.newArrayList();
        try {
            for (Object element : project.getRuntimeClasspathElements()) {
                log.debug("Adding runtime classpath element: " + element);
                locations.add(new File((String) element));
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("dependencies not resolved", e);
        }
        
        // hack: add the required files to the classloader
        boostrapClassloader(Iterables.transform(locations, new Function<File, URL>() {
            
            @Override
            public URL apply(File from) {
                try {
                    return from.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            
        }));

        final String value = Joiner.on(File.pathSeparator).join(locations);
        final Classpath cp = Reflection.classpathOf(value);
        final Predicate<Class<?>> predicate = Reflection.isConcreteClass();
        return Sets.newLinkedHashSet(cp.restrictTo(packages).filter(IpcCommand.class, predicate));
    }

    /**
     * WARNING: dirtiest maven hack ever!
     * @param classpath elements to add to the current classpath
     */
    @OhNoYouDidnt
    private static void boostrapClassloader(Iterable<URL> classpath) {
        final Method method;
        
        try {
            final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            final Class<? extends ClassLoader> classloaderClass = classloader.getClass();
            method = classloaderClass.getSuperclass().getDeclaredMethod("addURL", URL.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("used classloader has no addURL method", e);
        }
        
        final boolean accessible = method.isAccessible();
        method.setAccessible(true);
        
        try {
            for (URL url : classpath) {
                method.invoke(Thread.currentThread().getContextClassLoader(), url);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot call classloader's addURL method", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("cannot call classloader's addURL method", e);
        } finally {
            method.setAccessible(accessible);
        }
    }
    
}
