/*
 * Copyright (c) 2013 Cinnober Financial Technology AB, Stockholm,
 * Sweden. All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Cinnober Financial Technology AB, Stockholm, Sweden. You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Cinnober.
 * 
 * Cinnober makes no representations or warranties about the suitability
 * of the software, either expressed or implied, including, but not limited
 * to, the implied warranties of merchantibility, fitness for a particular
 * purpose, or non-infringement. Cinnober shall not be liable for any
 * damages suffered by licensee as a result of using, modifying, or
 * distributing this software or its derivatives.
 */

package com.cinnober.msgcodec.blink;

import com.cinnober.msgcodec.util.ConcurrentBufferPool;
import com.cinnober.msgcodec.util.Pool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mikael.brannstrom
 */
public class InternalBlinkBufferTest {

    public InternalBlinkBufferTest() {
    }

    @Test
    public void testCopyToStream() throws Exception {
        Pool<byte[]> pool = new ConcurrentBufferPool(10, 10);
        InternalBlinkBuffer ibb = new InternalBlinkBuffer(pool);
        final int length = 100;

        for (int i=0; i<length; i++) {
            ibb.write(i);
        }

        for (int start=0; start<length; start++) {
            for (int end=start; end<length; end++) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ibb.copyTo(out, start, end);

                    byte[] array = out.toByteArray();
                    assertEquals("array.length (start="+start+",end="+end+")", end-start, array.length);
                    byte[] expArray = new byte[end-start];
                    for (int i=0; i<expArray.length; i++) {
                        expArray[i] = (byte) (i+start);
                    }
                    assertArrayEquals("array (start="+start+",end="+end+")", expArray, array);
                } catch (Exception e) {
                    System.err.println("Exception (start="+start+",end="+end+")");
                    throw e;
                }
            }
        }
    }
}
