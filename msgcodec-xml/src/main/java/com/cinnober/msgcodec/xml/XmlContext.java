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
package com.cinnober.msgcodec.xml;

import java.util.BitSet;
import java.util.Stack;

/**
 * @author mikael.brannstrom
 *
 */
class XmlContext {
    private final Stack<Object> context = new Stack<>();
    private final Stack<BitSet> requiredFields = new Stack<>();

    public void pushValue(Object value) {
        context.push(value);
    }
    public BitSet pushRequiredFields(int numRequiredFields) {
        BitSet bits = new BitSet(numRequiredFields);
        bits.set(0, numRequiredFields);
        requiredFields.push(bits);
        return bits;
    }

    public void clearRequiredFieldSlot(int requiredFieldSlot) {
        peekRequiredFields().clear(requiredFieldSlot);
    }

    public BitSet peekRequiredFields() {
        return requiredFields.peek();
    }

    public BitSet popRequiredFields() {
        return requiredFields.pop();
    }

    public Object popValue() {
        return context.pop();
    }

    public Object peekValue() {
        return context.peek();
    }

    public void clear() {
        context.clear();
        requiredFields.clear();
    }

    @Override
    public String toString() {
        return context.toString();
    }
}
