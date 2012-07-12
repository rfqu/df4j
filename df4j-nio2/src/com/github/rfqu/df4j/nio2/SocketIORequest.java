/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.core.Port;

public class SocketIORequest<R extends SocketIORequest<R>> extends IORequest<R, AsyncSocketChannel> {
	long timeout; // milliseconds
	boolean timed;

    public SocketIORequest(ByteBuffer buf) {
        super(buf);
    }

    @Override
    public void prepare(AsyncSocketChannel channel, boolean read, Port<R> replyTo) {
        super.prepare(channel, read, replyTo);
        timed=false;
        this.timeout=0;
    }

    public void prepare(AsyncSocketChannel channel, boolean read, 
            Port<R> replyTo, long timeout)
    {
        super.prepare(channel, read, replyTo);
        timed=false;
        this.timeout=timeout;
    }

}
 