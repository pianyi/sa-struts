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
package org.seasar.struts.exception;

import org.seasar.framework.exception.SRuntimeException;

/**
 * 実行メソッドが見つからない場合の例外です。
 * 
 * @author higa
 * 
 */
public class ExecuteMethodNotFoundRuntimeException extends SRuntimeException {

    private static final long serialVersionUID = 1L;

    private Class<?> targetClass;

    /**
     * インスタンスを構築します。
     * 
     * @param targetClass
     *            対象クラス
     */
    public ExecuteMethodNotFoundRuntimeException(Class<?> targetClass) {
        super("ESAS0004", new Object[] { targetClass.getName() });
        this.targetClass = targetClass;
    }

    /**
     * 対象クラスを返します。
     * 
     * @return 対象クラス
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }
}