/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 The MsgCodec Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cinnober.msgcodec.io;

import java.io.IOException;

/**
 * TODO: javadoc
 *
 * @author mikael.brannstrom
 */
public interface ByteSink {
    void write(int b) throws IOException;

    default void write(byte[] b, int off, int len) throws IOException {
        for (int i=off; i<len; i++) {
            write(b[i]);
        }
    }

    default void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    default void writeIntLE(int v) throws IOException {
        write(v);
        write(v >> 8);
        write(v >> 16);
        write(v >> 24);
    }

    default void writeLongLE(long v) throws IOException {
        write((int) v);
        write((int) (v >> 8));
        write((int) (v >> 16));
        write((int) (v >> 24));
        write((int) (v >> 32));
        write((int) (v >> 40));
        write((int) (v >> 48));
        write((int) (v >> 56));
    }
}
