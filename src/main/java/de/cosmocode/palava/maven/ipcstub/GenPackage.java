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

import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.cosmocode.palava.ipc.IpcCommand;

/**
 * A small value object for commands and packages.
 * 
 * @author Tobias Sarnowski
 */
public class GenPackage {
    
    // this package's name
    private final String fullName;

    private final String name;
    
    // this package's parent
    private final GenPackage parent;

    // list of all subpackages
    private Set<GenPackage> packages = Sets.newLinkedHashSet();

    // list of all commands in this package
    private Set<GenCommand> commands = Sets.newLinkedHashSet();

    protected GenPackage(String fullName, GenPackage parent) {
        this.fullName = Preconditions.checkNotNull(fullName, "FullName");
        final int index = fullName.lastIndexOf(".");
        if (index > 0) {
            this.name = fullName.substring(index + 1);
        } else {
            this.name = fullName;
        }
        
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public GenPackage getParent() {
        return parent;
    }

    public Set<GenPackage> getPackages() {
        return packages;
    }

    public Set<GenCommand> getCommands() {
        return commands;
    }

    private void addPackage(GenPackage pkg) {
        packages.add(pkg);
    }

    private void addCommand(GenCommand genCommand) {
        commands.add(genCommand);
    }


    /**
     * Parses the given commands.
     * 
     * @param classes all commands to process
     * @param parent the parent package
     * @return the new tree structure
     * @throws MojoExecutionException if execution failed
     */
    protected static Set<GenPackage> getFirstPackages(Set<Class<? extends IpcCommand>> classes, 
        GenPackage parent) throws MojoExecutionException {
        
        final Set<GenPackage> packages = Sets.newLinkedHashSet();

        for (Class<? extends IpcCommand> command : classes) {
            // within the right package?
            String className;
            if (parent != null) {
                if (!command.getName().startsWith(parent.getFullName() + ".")) {
                    // not within the requested package
                    continue;
                } else {
                    // strip the parents package
                    className = command.getName().substring(parent.getFullName().length() + 1);
                }
            } else {
                className = command.getName();
            }
            // we just need the first element
            final int index = className.indexOf(".");
            if (index == 0) {
                throw new MojoExecutionException("invalid class definition found: " + command.getName());
            } else if (index == -1) {
                Preconditions.checkNotNull(parent, "Parent");
                parent.addCommand(new GenCommand(command));
            } else {
                // found a package, do we have it already?
                final String pkgName = command.getName().substring(0, 
                    command.getName().length() - className.length() + index);
                boolean found = false;
                for (GenPackage pkg : packages) {
                    if (pkg.getFullName().equals(pkgName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // we don't have it, create it
                    final GenPackage genPackage = new GenPackage(pkgName, parent);
                    for (GenPackage pkg : getFirstPackages(classes, genPackage)) {
                        genPackage.addPackage(pkg);
                    }
                    packages.add(genPackage);
                }
            }
        }

        return packages;
    }
}
