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

import com.google.common.collect.Lists;

import de.cosmocode.palava.ipc.IpcCommand;

/**
 * @author Tobias Sarnowski
 */
public class InspectedCommand {

    private Class<? extends IpcCommand> command;

    public static InspectedCommand inspectCommand(Class<? extends IpcCommand> command) {
        return new InspectedCommand(command);
    }

    protected InspectedCommand(Class<? extends IpcCommand> command) {
        this.command = command;
    }


    public Class<? extends IpcCommand> getCommand() {
        return command;
    }

    public String getDescription() {
        final IpcCommand.Description description = command.getAnnotation(IpcCommand.Description.class);
        if (description == null) {
            return "";
        } else {
            return description.value();
        }
    }

    public boolean isDeprecated() {
        final Deprecated deprecated = command.getAnnotation(Deprecated.class);
        return Boolean.valueOf(deprecated != null);
    }


    public boolean hasMetaInformations() {
        for (Annotation a : command.getAnnotations()) {
            if (a.annotationType().isAnnotationPresent(IpcCommand.Meta.class)) {
                return true;
            }
        }
        return false;
    }


    public List<IpcCommand.Param> getParams() {
        final List<IpcCommand.Param> parameters = Lists.newArrayList();

        final IpcCommand.Param param = command.getAnnotation(IpcCommand.Param.class);
        if (param != null) {
            parameters.add(param);
        }

        final IpcCommand.Params params = command.getAnnotation(IpcCommand.Params.class);
        if (params != null) {
            for (IpcCommand.Param p : params.value()) {
                parameters.add(p);
            }
        }

        return parameters;
    }

    public List<IpcCommand.Throw> getThrows() {
        final List<IpcCommand.Throw> throwables = Lists.newArrayList();

        final IpcCommand.Throw throwAnnotation = command.getAnnotation(IpcCommand.Throw.class);
        if (throwAnnotation != null) {
            throwables.add(throwAnnotation);
        }

        final IpcCommand.Throws throwsAnnotation = command.getAnnotation(IpcCommand.Throws.class);
        if (throwsAnnotation != null) {
            for (IpcCommand.Throw t : throwsAnnotation.value()) {
                throwables.add(t);
            }
        }

        return throwables;
    }

    public List<IpcCommand.Return> getReturns() {
        final List<IpcCommand.Return> returns = Lists.newArrayList();

        final IpcCommand.Return returnAnnotation = command.getAnnotation(IpcCommand.Return.class);
        if (returnAnnotation != null) {
            returns.add(returnAnnotation);
        }

        final IpcCommand.Returns returnsAnnotation = command.getAnnotation(IpcCommand.Returns.class);
        if (returnsAnnotation != null) {
            for (IpcCommand.Return r : returnsAnnotation.value()) {
                returns.add(r);
            }
        }

        return returns;
    }
}