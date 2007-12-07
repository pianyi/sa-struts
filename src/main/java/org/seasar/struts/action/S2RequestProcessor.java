/*
 * Copyright 2004-2006 the Seasar Foundation and the Others.
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
package org.seasar.struts.action;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.upload.MultipartRequestHandler;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.seasar.framework.beans.BeanDesc;
import org.seasar.framework.beans.IllegalPropertyRuntimeException;
import org.seasar.framework.beans.ParameterizedClassDesc;
import org.seasar.framework.beans.PropertyDesc;
import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.util.ArrayUtil;
import org.seasar.framework.util.ClassUtil;
import org.seasar.framework.util.EnumerationIterator;
import org.seasar.framework.util.ModifierUtil;
import org.seasar.struts.config.S2ActionMapping;
import org.seasar.struts.exception.NoParameterizedListRuntimeException;

/**
 * Seasar2用のリクエストプロセッサです。
 * 
 * @author higa
 */
public class S2RequestProcessor extends RequestProcessor {

    private static final char NESTED_DELIM = '.';

    private static final char INDEXED_DELIM = '[';

    private static final char INDEXED_DELIM2 = ']';

    @Override
    public HttpServletRequest processMultipart(HttpServletRequest request) {
        HttpServletRequest result = super.processMultipart(request);
        SingletonS2ContainerFactory.getContainer().getExternalContext()
                .setRequest(result);
        return result;
    }

    @Override
    protected ActionForm processActionForm(HttpServletRequest request,
            HttpServletResponse response, ActionMapping mapping) {

        String name = mapping.getName();
        if (name == null) {
            return null;
        }
        FormBeanConfig formConfig = moduleConfig.findFormBeanConfig(name);
        if (formConfig == null) {
            return null;
        }
        ActionForm actionForm = null;
        try {
            actionForm = formConfig.createActionForm(servlet);
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            return null;
        }
        if ("request".equals(mapping.getScope())) {
            request.setAttribute(mapping.getAttribute(), actionForm);
        } else {
            HttpSession session = request.getSession();
            session.setAttribute(mapping.getAttribute(), actionForm);
        }
        return actionForm;

    }

    @Override
    protected Action processActionCreate(HttpServletRequest request,
            HttpServletResponse response, ActionMapping mapping)
            throws IOException {

        Action action = null;
        try {
            action = new ActionWrapper(((S2ActionMapping) mapping));
        } catch (Exception e) {
            log.error(getInternal().getMessage("actionCreate",
                    mapping.getPath()), e);
            response
                    .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            getInternal().getMessage("actionCreate",
                                    mapping.getPath()));
            return null;
        }
        action.setServlet(servlet);
        return action;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processPopulate(HttpServletRequest request,
            HttpServletResponse response, ActionForm form, ActionMapping mapping)
            throws ServletException {

        if (form == null) {
            return;
        }
        form.setServlet(servlet);
        form.reset(mapping, request);
        Iterator<String> names = null;
        String contentType = request.getContentType();
        String method = request.getMethod();
        boolean isMultipart = false;
        form.setMultipartRequestHandler(null);
        MultipartRequestHandler multipartHandler = null;
        if (contentType != null
                && contentType.startsWith("multipart/form-data")
                && method.equalsIgnoreCase("POST")) {
            multipartHandler = getMultipartHandler(mapping.getMultipartClass());
            if (multipartHandler != null) {
                isMultipart = true;
                multipartHandler.setServlet(servlet);
                multipartHandler.setMapping(mapping);
                multipartHandler.handleRequest(request);
                Boolean maxLengthExceeded = (Boolean) request
                        .getAttribute(MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED);
                if ((maxLengthExceeded != null)
                        && (maxLengthExceeded.booleanValue())) {
                    form.setMultipartRequestHandler(multipartHandler);
                    return;
                }
                names = getAllParameterNamesForMultipartRequest(request,
                        multipartHandler);
            }
        }
        if (!isMultipart) {
            names = new EnumerationIterator(request.getParameterNames());
        }
        while (names.hasNext()) {
            String name = names.next();
            S2ActionMapping actionMapping = (S2ActionMapping) mapping;
            try {
                setProperty(actionMapping.getActionForm(), name, request
                        .getParameterValues(name));
            } catch (Throwable t) {
                throw new IllegalPropertyRuntimeException(actionMapping
                        .getActionFormBeanDesc().getBeanClass(), name, t);
            }
        }
    }

    /**
     * プロパティの値を設定します。
     * 
     * @param bean
     *            JavaBeans
     * @param name
     *            パラメータ名
     * @param values
     *            値の配列
     * @throws ServletException
     *             何か例外が発生した場合。
     */
    protected void setProperty(Object bean, String name, String[] values) {
        if (bean == null) {
            return;
        }
        int nestedIndex = name.indexOf(NESTED_DELIM);
        int indexedIndex = name.indexOf(INDEXED_DELIM);
        if (nestedIndex < 0 && indexedIndex < 0) {
            setSimpleProperty(bean, name, values);
        } else if (nestedIndex >= 0 && indexedIndex >= 0) {
            if (nestedIndex < indexedIndex) {
                setProperty(getSimpleProperty(bean, name.substring(0,
                        nestedIndex)), name.substring(nestedIndex + 1), values);
            } else {
                IndexParsedResult result = parseIndex(name
                        .substring(indexedIndex + 1));
                bean = getIndexedProperty(bean,
                        name.substring(0, indexedIndex), result.indexes);
                setProperty(bean, result.name, values);
            }
        } else if (nestedIndex >= 0) {
            setProperty(
                    getSimpleProperty(bean, name.substring(0, nestedIndex)),
                    name.substring(nestedIndex + 1), values);
        }
    }

    /**
     * 単純なプロパティの値を設定します。
     * 
     * @param bean
     *            JavaBeans
     * @param name
     *            パラメータ名
     * @param values
     *            値の配列
     * @throws ServletException
     *             何か例外が発生した場合。
     */
    @SuppressWarnings("unchecked")
    protected void setSimpleProperty(Object bean, String name, String[] values) {
        BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return;
        }
        PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isWritable()) {
            return;
        }
        if (pd.getPropertyType().isArray()) {
            pd.setValue(bean, values);
        } else if (List.class.isAssignableFrom(pd.getPropertyType())) {
            List<String> list = ModifierUtil.isAbstract(pd.getPropertyType()) ? new ArrayList<String>()
                    : (List<String>) ClassUtil
                            .newInstance(pd.getPropertyType());
            list.addAll(Arrays.asList(values));
            pd.setValue(bean, list);
        } else if (values == null || values.length == 0) {
            pd.setValue(bean, null);
        } else {
            pd.setValue(bean, values[0]);
        }
    }

    /**
     * 単純なプロパティの値を返します。
     * 
     * @param bean
     *            JavaBeans
     * @param name
     *            プロパティ名
     * @return プロパティの値
     */
    protected Object getSimpleProperty(Object bean, String name) {
        BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return null;
        }
        PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isReadable()) {
            return null;
        }
        Object value = pd.getValue(bean);
        if (value == null && !ModifierUtil.isAbstract(pd.getPropertyType())) {
            value = ClassUtil.newInstance(pd.getPropertyType());
            if (pd.isWritable()) {
                pd.setValue(bean, value);
            }
        }
        return value;
    }

    /**
     * インデックス化されたプロパティの値を返します。
     * 
     * @param bean
     *            JavaBeans
     * @param name
     *            名前
     * @param indexes
     *            インデックスの配列
     * @return インデックス化されたプロパティの値
     * 
     */
    @SuppressWarnings("unchecked")
    protected Object getIndexedProperty(Object bean, String name, int[] indexes) {
        BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return null;
        }
        PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isReadable()) {
            return null;
        }
        if (pd.getPropertyType().isArray()) {
            Object array = pd.getValue(bean);
            Class<?> elementType = getArrayElementType(pd.getPropertyType(),
                    indexes.length);
            if (array == null) {
                int[] newIndexes = new int[indexes.length];
                newIndexes[0] = indexes[0] + 1;
                array = Array.newInstance(elementType, newIndexes);
            }
            array = expand(array, indexes, elementType);
            pd.setValue(bean, array);
            return fillArrayValue(array, indexes, elementType);
        } else if (List.class.isAssignableFrom(pd.getPropertyType())) {
            List list = (List) pd.getValue(bean);
            if (list == null) {
                list = new ArrayList(Math.max(50, indexes[0]));
                pd.setValue(bean, list);
            }
            ParameterizedClassDesc pcd = pd.getParameterizedClassDesc();
            for (int i = 0; i < indexes.length; i++) {
                if (pcd == null || !pcd.isParameterizedClass()
                        || !List.class.isAssignableFrom(pcd.getRawClass())) {
                    throw new NoParameterizedListRuntimeException(beanDesc
                            .getBeanClass(), pd.getPropertyName());
                }
                int size = list.size();
                pcd = pcd.getArguments()[0];
                for (int j = size; j <= indexes[i]; j++) {
                    if (i == indexes.length - 1) {
                        list.add(ClassUtil.newInstance(pcd.getRawClass()));
                    } else {
                        list.add(new ArrayList());
                    }
                }
                if (i < indexes.length - 1) {
                    list = (List) list.get(indexes[i]);
                }
            }
            return list.get(indexes[indexes.length - 1]);
        } else {
            return null;
        }
    }

    /**
     * 配列の要素の型を返します。
     * 
     * @param clazz
     *            配列のクラス
     * @param depth
     *            配列の深さ
     * @return 配列の要素の型
     */
    protected Class<?> getArrayElementType(Class<?> clazz, int depth) {
        for (int i = 0; i < depth; i++) {
            clazz = clazz.getComponentType();
        }
        return clazz;
    }

    /**
     * 配列を拡張します。
     * 
     * @param array
     *            配列
     * @param indexes
     *            インデックスの配列
     * @param elementType
     *            配列の要素のクラス
     * @return 拡張後の配列
     */
    protected Object expand(Object array, int[] indexes, Class<?> elementType) {
        int length = Array.getLength(array);
        if (length <= indexes[0]) {
            int[] newIndexes = new int[indexes.length];
            newIndexes[0] = indexes[0] + 1;
            Object newArray = Array.newInstance(elementType, newIndexes);
            System.arraycopy(array, 0, newArray, 0, length);
            array = newArray;
        }
        if (indexes.length > 1) {
            int[] newIndexes = new int[indexes.length - 1];
            for (int i = 1; i < indexes.length; i++) {
                newIndexes[i - 1] = indexes[i];
            }
            Array.set(array, indexes[0], expand(Array.get(array, indexes[0]),
                    newIndexes, elementType));
        }
        return array;
    }

    /**
     * 配列の値を返します。
     * 
     * @param array
     *            配列
     * @param indexes
     *            インデックスの配列
     * @param elementType
     *            配列の要素の型
     * @return 配列の値
     */
    protected Object fillArrayValue(Object array, int[] indexes,
            Class<?> elementType) {
        Object value = array;
        for (int i = 0; i < indexes.length; i++) {
            Object value2 = Array.get(value, indexes[i]);
            if (i == indexes.length - 1 && value2 == null) {
                value2 = ClassUtil.newInstance(elementType);
                Array.set(value, indexes[i], value2);
            }
            value = value2;
        }
        return value;
    }

    /**
     * インデックスを解析します。
     * 
     * @param name
     *            プロパティ名
     * @return インデックスの解析結果
     */
    protected IndexParsedResult parseIndex(String name) {
        IndexParsedResult result = new IndexParsedResult();
        while (true) {
            int index = name.indexOf(INDEXED_DELIM2);
            if (index < 0) {
                throw new IllegalArgumentException(INDEXED_DELIM2
                        + " is not found in " + name);
            }
            result.indexes = ArrayUtil.add(result.indexes, Integer.valueOf(
                    name.substring(0, index)).intValue());
            name = name.substring(index + 1);
            if (name.length() == 0) {
                break;
            } else if (name.charAt(0) == INDEXED_DELIM) {
                name = name.substring(1);
            } else if (name.charAt(0) == NESTED_DELIM) {
                name = name.substring(1);
                break;
            } else {
                throw new IllegalArgumentException(name);
            }
        }
        result.name = name;
        return result;
    }

    /**
     * マルチパートリクエストハンドラを返します。
     * 
     * @param multipartClass
     *            マルチパートリクエストハンドラのクラス名
     * @return マルチパートリクエストハンドラ
     * @throws ServletException
     *             何か例外が発生した場合。
     */
    protected MultipartRequestHandler getMultipartHandler(String multipartClass)
            throws ServletException {

        MultipartRequestHandler multipartHandler = null;
        if (multipartClass != null) {
            try {
                multipartHandler = (MultipartRequestHandler) ClassUtil
                        .newInstance(multipartClass);
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                throw new ServletException(t.getMessage(), t);
            }
            if (multipartHandler != null) {
                return multipartHandler;
            }
        }
        multipartClass = moduleConfig.getControllerConfig().getMultipartClass();
        if (multipartClass != null) {
            try {
                multipartHandler = (MultipartRequestHandler) ClassUtil
                        .newInstance(multipartClass);
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                throw new ServletException(t.getMessage(), t);
            }
            if (multipartHandler != null) {
                return multipartHandler;
            }
        }
        return null;
    }

    /**
     * マルチパート用のパラメータを返します。
     * 
     * @param request
     *            リクエスト
     * @param multipartHandler
     *            マルチパートリクエストハンドラ
     * @return マルチパート用のパラメータ
     */
    @SuppressWarnings("unchecked")
    protected Iterator<String> getAllParameterNamesForMultipartRequest(
            HttpServletRequest request, MultipartRequestHandler multipartHandler) {
        Set<String> names = new LinkedHashSet<String>();
        Hashtable elements = multipartHandler.getAllElements();
        Enumeration e = elements.keys();
        while (e.hasMoreElements()) {
            names.add((String) e.nextElement());
        }
        if (request instanceof MultipartRequestWrapper) {
            request = ((MultipartRequestWrapper) request).getRequest();
            e = request.getParameterNames();
            while (e.hasMoreElements()) {
                names.add((String) e.nextElement());
            }
        }
        return names.iterator();
    }

    /**
     * 
     */
    protected static class IndexParsedResult {
        /**
         * インデックスの配列です。
         */
        public int[] indexes = new int[0];

        /**
         * インデックス部分を除いた名前です。
         */
        public String name;
    }
}