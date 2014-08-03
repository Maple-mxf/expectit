package net.sf.expectit;

/*
 * #%L
 * ExpectIt
 * %%
 * Copyright (C) 2014 Alexey Gavrilov and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.util.Properties;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.*;

/**
 * An example of interacting with the local SSH server
 */
public class SshLocalhostExample {
    public static void main(String[] args) throws JSchException, IOException {
        JSch jSch = new JSch();
        Session session = jSch.getSession(System.getenv("USER"), "localhost");
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        jSch.addIdentity(System.getProperty("user.home") + "/.ssh/id_rsa");
        session.setConfig(config);
        session.connect();
        Channel channel = session.openChannel("shell");

        Expect expect = new ExpectBuilder()
                .withOutput(channel.getOutputStream())
                .withInputs(channel.getInputStream(), channel.getExtInputStream())
                .withInputFilters(removeColors(), removeNonPrintable())
                .build();
        try {
            channel.connect();
            expect.expect(contains("$"));
            expect.sendLine("pwd");
            // a regexp which captures the output of pwd
            System.out.println(expect.expect(regexp("(?m)\\n([^\\n]*)\\n")).group(1));
            expect.sendLine("ls -l");
            // skipping the echo command
            expect.expect(contains("\n"));
            // getting the output of ls
            System.out.println(expect.expect(regexp(".*\\$")).getBefore().trim());
            expect.sendLine("exit");
        } finally {
            channel.disconnect();
            session.disconnect();
            expect.close();
        }
    }
}
