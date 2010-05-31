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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import de.cosmocode.classpath.ClassPath;
import de.cosmocode.palava.ipc.IpcCommand;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @description Generates stub files for all found IpcCommands in the classpath.
 * @goal generate-ipcstub
 * @requiresDependencyResolution runtime
 * @author Tobias Sarnowski
 */
public class GeneratorModule extends AbstractMojo {
    private final Log LOG = getLog();

    private static final Comparator<Class<?>> CLASS_COMPARATOR = Ordering.natural().onResultOf(new Function<Class<?>,String>() {
        @Override
        public String apply(Class<?> aClass) {
            return aClass.getName();
        }
    });

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
        Set<Class<? extends IpcCommand>> foundClasses = Sets.newTreeSet(CLASS_COMPARATOR);
        foundClasses.addAll(generateCommandList(allPackages));

        LOG.info("Found " + foundClasses.size() + " IpcCommands; generating stubs...");

        // filter classes and let the generators do their work
        for (Generator generator: generators) {
            Set<Class<? extends IpcCommand>> filteredClasses = Sets.newLinkedHashSet();
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
        boostrapClassloader(urls);

        try {
            ClassPath classPath = new ClassPath(urls);
            return classPath.getImplementationsOf(IpcCommand.class, packages);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot generate list of commands", e);
        }
    }

    /**
     * WARNING: dirtiest maven hack ever!
     * @param classpath elements to add to the current classpath
     */
    @OhNoYouDidnt
    private static void boostrapClassloader(URL[] classpath) {
        Method m = null;
        try {
            m = Thread.currentThread().getContextClassLoader().getClass().getSuperclass().getDeclaredMethod("addURL", new Class[]{URL.class});
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("used classloader has no addURL method", e);
        }
        final boolean a = m.isAccessible();
        m.setAccessible(true);
        try {
            for (URL url: classpath) {
                m.invoke(Thread.currentThread().getContextClassLoader(), url);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot call classloader's addURL method", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("cannot call classloader's addURL method", e);
        } finally {
            m.setAccessible(a);
        }
    }
}
