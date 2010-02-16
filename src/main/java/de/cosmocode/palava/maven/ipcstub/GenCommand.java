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

import de.cosmocode.palava.ipc.IpcCommand;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * @author Tobias Sarnowski
 */
public class GenCommand {
    // the command to inspect
    private Class command;

    protected GenCommand(Class command) {
        this.command = command;
    }

    private Annotation getAnnotation(String annotationName) throws MojoExecutionException {
        try {
            Class annotation = command.getClassLoader().loadClass(annotationName);
            return command.getAnnotation(annotation);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("cannot load @" + annotationName + " annotation", e);
        }
    }

    public String getName() {
        return command.getSimpleName();
    }

    public String getFullName() {
        return command.getName();
    }

    public String getDescription() throws MojoExecutionException {
        Annotation annotation = getAnnotation(IpcCommand.Description.class.getName());
        return null;
    }

}
