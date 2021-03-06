/*-------------------------------------------------------------------------------------------------------------------*\
|  Copyright (C) 2014 eBay Software Foundation                                                                        |
|                                                                                                                     |
|  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance     |
|  with the License.                                                                                                  |
|                                                                                                                     |
|  You may obtain a copy of the License at                                                                            |
|                                                                                                                     |
|       http://www.apache.org/licenses/LICENSE-2.0                                                                    |
|                                                                                                                     |
|  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed   |
|  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for  |
|  the specific language governing permissions and limitations under the License.                                     |
\*-------------------------------------------------------------------------------------------------------------------*/

package com.paypal.selion.utils.process;

import java.io.IOException;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.apache.commons.lang3.StringUtils;

import com.paypal.selion.pojos.ProcessInfo;
import com.paypal.selion.pojos.ProcessNames;

/**
 * This class provides for a simple implementation that aims at providing the logic to fetch processes as
 * represented by {@link ProcessNames} and also in forcibly killing them on a Non Windows like environment.
 *
 */
public class NonWindowsProcessHandler extends AbstractProcessHandler implements ProcessHandler {
    public NonWindowsProcessHandler() {
        log.info("You have chosen to use a NON Windows Process Handler.");
    }

    private static final String DELIMITER = "<#>";

    @Override
    public List<ProcessInfo> potentialProcessToBeKilled() throws ProcessHandlerException {
        try {
            // Java has no direct way to get our pid.
            int ourProcessPID = CLibrary.INSTANCE.getpid();

            // Find all processes that are our direct children using our PID as the parent pid to pgrep.
            // The pgrep command is basically getting all child processes and we are interested only in
            // process name and PID with "<#>" as a delimiter.
            String cmd = String.format("pgrep -P %s -l | awk '{ print $2\"%s\"$1 }'",
                    Integer.toString(ourProcessPID), DELIMITER);
            return getProcessInfo(new String[] { "sh", "-c", cmd }, DELIMITER, OSPlatform.NONWINDOWS);
        } catch (IOException | InterruptedException e) {
            throw new ProcessHandlerException(e);
        }
    }

    @Override
    public void killProcess(List<ProcessInfo> processes) throws ProcessHandlerException {
        super.killProcess(new String[] {"kill", "-9"}, processes);
    }

    /**
     * @param image
     *            - The image name of the process
     * @return - <code>true</code> if the image name matches one of the below conditions when compared to the list of
     *         image names that are part of {@link ProcessNames} enum.
     *         <ul>
     *         <li>Begins with
     *         <li>Ends with
     *         <li>contains
     *         </ul>
     */
    @Override
    protected boolean matches(String image) {
        if (StringUtils.isEmpty(image)) {
            return false;
        }
        // On Non Windows the image name can either be at the beginning of the command
        // as in the case of chromedriver process (or) it can be at the end of the command
        // as in the case of binaries such as firefox
        // or in the middle as in the case of chrome browser
        // so we need to look at all the places places
        for (ProcessNames eachImage : ProcessNames.values()) {
            String img = eachImage.getNonWindowsImageName();
            if (image.startsWith(img) || image.contains(img) || image.endsWith(img)) {
                return true;
            }
        }
        return false;
    }

    private interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);
        int getpid();
    }

}
