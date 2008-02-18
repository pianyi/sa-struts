/*
 * Copyright 2004-2007 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.struts.taglib;

import junit.framework.TestCase;

/**
 * @author higa
 * 
 */
public class S2FunctionsTest extends TestCase {

    /**
     * @throws Exception
     */
    public void testHForCharArray() throws Exception {
        assertEquals("[1]", S2Functions.h(new char[] { '1' }));
    }

    /**
     * @throws Exception
     */
    public void testHForByteArray() throws Exception {
        assertEquals("[1]", S2Functions.h(new byte[] { 1 }));
    }

    /**
     * @throws Exception
     */
    public void testHForShortArray() throws Exception {
        assertEquals("[1]", S2Functions.h(new short[] { 1 }));
    }

    /**
     * @throws Exception
     */
    public void testHForIntArray() throws Exception {
        assertEquals("[1]", S2Functions.h(new int[] { 1 }));
    }

    /**
     * @throws Exception
     */
    public void testHForFloatArray() throws Exception {
        assertEquals("[1.0]", S2Functions.h(new float[] { 1 }));
    }

    /**
     * @throws Exception
     */
    public void testHForDoubleArray() throws Exception {
        assertEquals("[1.0]", S2Functions.h(new double[] { 1 }));
    }

    /**
     * @throws Exception
     */
    public void testHForBooleanArray() throws Exception {
        assertEquals("[true]", S2Functions.h(new boolean[] { true }));
    }

    /**
     * @throws Exception
     */
    public void testHForStringArray() throws Exception {
        assertEquals("[1]", S2Functions.h(new String[] { "1" }));
    }

    /**
     * @throws Exception
     */
    public void testHForObjectArray() throws Exception {
        assertEquals("[1]", S2Functions.h(new Integer[] { Integer.valueOf(1) }));
    }

    /**
     * @throws Exception
     */
    public void testDate() throws Exception {
        assertNotNull(S2Functions.date("20080131", "yyyyMMdd"));
    }

    /**
     * @throws Exception
     */
    public void testDate_valueIsNull() throws Exception {
        assertNull(S2Functions.date(null, "yyyyMMdd"));
    }

    /**
     * @throws Exception
     */
    public void testDate_patternIsNull() throws Exception {
        try {
            S2Functions.date("20080131", null);
            fail();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
    }

    /**
     * @throws Exception
     */
    public void testNumber() throws Exception {
        assertEquals("1000", S2Functions.number("1000", "####").toString());
    }

    /**
     * @throws Exception
     */
    public void testNumber_valueIsNull() throws Exception {
        assertNull(S2Functions.number(null, "####"));
    }

    /**
     * @throws Exception
     */
    public void testNumber_patternIsNull() throws Exception {
        try {
            S2Functions.number("1000", null);
            fail();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
    }

    /**
     * @throws Exception
     */
    public void testBrForCRLF() throws Exception {
        assertEquals("<br />", S2Functions.br("\r\n"));
    }

    /**
     * @throws Exception
     */
    public void testBrForCR() throws Exception {
        assertEquals("<br />", S2Functions.br("\r"));
    }

    /**
     * @throws Exception
     */
    public void testBrForLF() throws Exception {
        assertEquals("<br />", S2Functions.br("\n"));
    }

    /**
     * @throws Exception
     */
    public void testBrForNull() throws Exception {
        assertEquals("", S2Functions.br(null));
    }
}