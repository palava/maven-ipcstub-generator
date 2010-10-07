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

import java.lang.annotation.Annotation;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import de.cosmocode.palava.ipc.IpcCommand;
import de.cosmocode.palava.ipc.IpcCommand.Description;
import de.cosmocode.palava.ipc.IpcCommand.Meta;
import de.cosmocode.palava.ipc.IpcCommand.Param;
import de.cosmocode.palava.ipc.IpcCommand.Params;
import de.cosmocode.palava.ipc.IpcCommand.Return;
import de.cosmocode.palava.ipc.IpcCommand.Returns;
import de.cosmocode.palava.ipc.IpcCommand.Throw;
import de.cosmocode.palava.ipc.IpcCommand.Throws;

/**
 * A wrapper around an {@link IpcCommand} class which provides easy retrieval of
 * meta information.
 * 
 * @author Tobias Sarnowski
 */
public final class InspectedCommand {

    private final Class<? extends IpcCommand> command;

    private InspectedCommand(Class<? extends IpcCommand> command) {
        this.command = Preconditions.checkNotNull(command, "Command");
    }

    public Class<? extends IpcCommand> getCommand() {
        return command;
    }

    /**
     * Provides the description of this command.
     *
     * @return the description
     */
    public String getDescription() {
        final Description description = command.getAnnotation(Description.class);
        if (description == null) {
            return "";
        } else {
            return description.value();
        }
    }

    /**
     * Checks whether this command is deprecated.
     *
     * @return true if deprecated, false otherwise
     */
    public boolean isDeprecated() {
        return command.isAnnotationPresent(Deprecated.class);
    }

    /**
     * Checks whether this command has any meta information.
     *
     * @return true if meta information are present, false otherwise.
     */
    public boolean hasMetaInformations() {
        for (Annotation annotation : command.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Meta.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this command has {@link Param}s defined.
     *
     * @return a list of all {@link Param}s.
     */
    public List<Param> getParams() {
        final List<Param> parameters = Lists.newArrayList();

        final Param param = command.getAnnotation(Param.class);
        if (param != null) {
            parameters.add(param);
        }

        final Params params = command.getAnnotation(Params.class);
        if (params != null) {
            for (Param p : params.value()) {
                parameters.add(p);
            }
        }

        return parameters;
    }

    /**
     * Checks whether this command has {@link Throw}s defined.
     *
     * @return a list of all {@link Throw}s.
     */
    public List<Throw> getThrows() {
        final List<Throw> throwables = Lists.newArrayList();

        final Throw throwAnnotation = command.getAnnotation(Throw.class);
        if (throwAnnotation != null) {
            throwables.add(throwAnnotation);
        }

        final Throws throwsAnnotation = command.getAnnotation(Throws.class);
        if (throwsAnnotation != null) {
            for (Throw t : throwsAnnotation.value()) {
                throwables.add(t);
            }
        }

        return throwables;
    }

    /**
     * Checks whether this command has {@link Return}s defined.
     *
     * @return a list of all {@link Return}s.
     */
    public List<Return> getReturns() {
        final List<Return> returns = Lists.newArrayList();

        final Return returnAnnotation = command.getAnnotation(Return.class);
        if (returnAnnotation != null) {
            returns.add(returnAnnotation);
        }

        final Returns returnsAnnotation = command.getAnnotation(Returns.class);
        if (returnsAnnotation != null) {
            for (Return r : returnsAnnotation.value()) {
                returns.add(r);
            }
        }

        return returns;
    }

    /**
     * Static factory method for {@link InspectedCommand}s.
     *
     * @param command the command being inspected
     * @return an {@link InspectedCommand}
     * @throws NullPointerException if command is null
     */
    public static InspectedCommand inspectCommand(Class<? extends IpcCommand> command) {
        return new InspectedCommand(command);
    }
    
}
