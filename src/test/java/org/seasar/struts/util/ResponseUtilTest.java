/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
package org.seasar.struts.util;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.seasar.extension.unit.S2TestCase;
import org.seasar.framework.mock.servlet.MockHttpServletResponse;

/**
 * @author higa
 * 
 */
public class ResponseUtilTest extends S2TestCase {
    /**
     * @throws Exception
     */
    public void testGetResponse() throws Exception {
        assertNotNull(ResponseUtil.getResponse());
    }

    /**
     * ストリームをダウンロードさせるケース
     * 
     * @throws Exception
     */
    public void testDownload_InputStream() throws Exception {
        byte[] b = new byte[] { 0x1, 0x2, (byte) 0xfe, (byte) 0xff };
        MockHttpServletResponse response = getResponse();
        CheckedInputStream is = new CheckedInputStream(
                new ByteArrayInputStream(b));
        ResponseUtil.download("test.txt", is);
        assertTrue("input stream must be closed", is.isClosed());
        assertEquals("application/octet-stream", response.getContentType());
        assertEquals(Arrays.toString(b), Arrays.toString(response
                .getResponseBytes()));
    }

    /**
     * ストリームをダウンロードさせるケース(Content-Lengthフィールド指定あり)
     * 
     * @throws Exception
     */
    public void testDownload_InputStreamWithContentLength() throws Exception {
        byte[] b = new byte[] { 0x1, 0x2, (byte) 0xfe, (byte) 0xff };
        MockHttpServletResponse response = getResponse();
        CheckedInputStream is = new CheckedInputStream(
                new ByteArrayInputStream(b));
        ResponseUtil.download("test.txt", is, b.length);
        assertTrue("input stream must be closed", is.isClosed());
        assertEquals("application/octet-stream", response.getContentType());
        assertEquals(Arrays.toString(b), Arrays.toString(response
                .getResponseBytes()));
        assertEquals(b.length, response.getContentLength());
    }

    /**
     * 閉じわすれをチェックするInputStream
     * 
     * @author ooharak
     * 
     */
    static class CheckedInputStream extends FilterInputStream {
        private boolean isClosed = false;

        /**
         * @param in
         */
        protected CheckedInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.isClosed = true;
        }

        /**
         * @return
         */
        public boolean isClosed() {
            return this.isClosed;
        }

    }
}