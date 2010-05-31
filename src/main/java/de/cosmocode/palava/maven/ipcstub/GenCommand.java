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

import de.cosmocode.palava.ipc.IpcCommand;

/**
 * @author Tobias Sarnowski
 */
public class GenCommand {
    // the command to inspect
    private Class<? extends IpcCommand> command;

    protected GenCommand(Class<? extends IpcCommand> command) {
        this.command = command;
    }

    public String getName() {
        return command.getSimpleName();
    }

    public String getFullName() {
        return command.getName();
    }

    public InspectedCommand getMeta() {
        return InspectedCommand.inspectCommand(command);
    }
}
