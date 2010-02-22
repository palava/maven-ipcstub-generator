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

import com.google.common.collect.Sets;
import de.cosmocode.palava.ipc.IpcCommand;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.Set;

/**
 * @author Tobias Sarnowski
 */
public class GenPackage {
    // this package's name
    private String name;

    // this package's parent
    private GenPackage parent;

    // list of all subpackages
    private Set<GenPackage> packages = Sets.newHashSet();

    // list of all commands in this package
    private Set<GenCommand> commands = Sets.newHashSet();


    protected GenPackage(String name, GenPackage parent) {
        this.name = name;
        this.parent = parent;
    }


    public String getName() {
        int index = name.lastIndexOf(".");
        if (index > 0) {
            return name.substring(index + 1);
        } else {
            return name;
        }
    }

    public String getFullName() {
        return name;
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
     * Parses the given commands
     * @param classes all commands to process
     * @return the new tree structure
     */
    protected static Set<GenPackage> getFirstPackages(Set<Class<? extends IpcCommand>> classes, GenPackage parent) throws MojoExecutionException {
        Set<GenPackage> packages = Sets.newHashSet();

        for (Class<? extends IpcCommand> command: classes) {
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
            int index = className.indexOf(".");
            if (index == 0) {
                throw new MojoExecutionException("invalid class definition found: " + command.getName());
            } else if (index == -1) {
                // found a command
                if (parent == null) {
                    throw new MojoExecutionException("found an IpcCommand without a package: " + command.getName());
                }
                parent.addCommand(new GenCommand(command));
            } else {
                // found a package, do we have it already?
                String pkgName = command.getName().substring(0, command.getName().length() - className.length() + index);
                boolean found = false;
                for (GenPackage pkg: packages) {
                    if (pkg.getFullName().equals(pkgName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // we don't have it, create it
                    GenPackage genPackage = new GenPackage(pkgName, parent);
                    for (GenPackage pkg: getFirstPackages(classes, genPackage)) {
                        genPackage.addPackage(pkg);
                    }
                    packages.add(genPackage);
                }
            }
        }

        return packages;
    }
}
