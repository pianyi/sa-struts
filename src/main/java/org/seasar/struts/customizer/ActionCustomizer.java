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
package org.seasar.struts.customizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.validator.Form;
import org.apache.commons.validator.FormSet;
import org.apache.commons.validator.ValidatorResources;
import org.apache.commons.validator.Var;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMessages;
import org.seasar.framework.beans.BeanDesc;
import org.seasar.framework.beans.PropertyDesc;
import org.seasar.framework.container.ComponentCustomizer;
import org.seasar.framework.container.ComponentDef;
import org.seasar.framework.util.ClassUtil;
import org.seasar.framework.util.MethodUtil;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.util.tiger.AnnotationUtil;
import org.seasar.struts.action.ActionFormWrapperClass;
import org.seasar.struts.action.S2DynaProperty;
import org.seasar.struts.annotation.ActionForm;
import org.seasar.struts.annotation.Arg;
import org.seasar.struts.annotation.Execute;
import org.seasar.struts.annotation.Input;
import org.seasar.struts.annotation.Msg;
import org.seasar.struts.annotation.Result;
import org.seasar.struts.annotation.Results;
import org.seasar.struts.annotation.Validator;
import org.seasar.struts.config.S2ActionMapping;
import org.seasar.struts.config.S2ExecuteConfig;
import org.seasar.struts.config.S2FormBeanConfig;
import org.seasar.struts.config.S2ModuleConfig;
import org.seasar.struts.exception.ExecuteMethodNotFoundRuntimeException;
import org.seasar.struts.exception.IllegalExecuteMethodRuntimeException;
import org.seasar.struts.exception.IllegalValidateMethodRuntimeException;
import org.seasar.struts.exception.InputNotDefinedRuntimeException;
import org.seasar.struts.util.ActionUtil;
import org.seasar.struts.util.ModuleConfigUtil;
import org.seasar.struts.util.ValidatorResourcesUtil;

/**
 * Actionのカスタマイザです。
 * 
 * @author higa
 * 
 */
public class ActionCustomizer implements ComponentCustomizer {

    public void customize(ComponentDef componentDef) {
        S2ActionMapping actionMapping = createActionMapping(componentDef);
        S2FormBeanConfig formConfig = createFormBeanConfig(actionMapping);
        S2ModuleConfig moduleConfig = ModuleConfigUtil.getModuleConfig();
        moduleConfig.addActionConfig(actionMapping);
        moduleConfig.addFormBeanConfig(formConfig);
        FormSet formSet = createFormSet(actionMapping);
        ValidatorResources validatorResources = ValidatorResourcesUtil
                .getValidatorResources();
        validatorResources.addFormSet(formSet);
    }

    /**
     * アクションマッピングを作成します。
     * 
     * @param componentDef
     *            コンポーネント定義
     * @return アクションマッピング
     */
    protected S2ActionMapping createActionMapping(ComponentDef componentDef) {
        S2ActionMapping actionMapping = new S2ActionMapping();
        actionMapping.setPath(ActionUtil.fromActionNameToPath(componentDef
                .getComponentName()));
        actionMapping.setComponentDef(componentDef);
        actionMapping.setName(componentDef.getComponentName() + "Form");
        Class<?> actionClass = componentDef.getComponentClass();
        setupInput(actionMapping, actionClass);
        setupResult(actionMapping, actionClass);
        setupMethod(actionMapping, actionClass);
        setupActionForm(actionMapping, actionClass);
        setupReset(actionMapping, actionClass);
        return actionMapping;
    }

    /**
     * 入力元の情報をセットアップします。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @param actionClass
     *            アクションクラス
     */
    protected void setupInput(S2ActionMapping actionMapping,
            Class<?> actionClass) {
        Input input = actionClass.getAnnotation(Input.class);
        if (input == null) {
            return;
        }
        actionMapping.setInput(input.name());
        ActionForward forward = new ActionForward();
        forward.setName(input.name());
        forward.setPath(input.path());
        forward.setRedirect(input.redirect());
        actionMapping.addForwardConfig(forward);
    }

    /**
     * 遷移先の情報をセットアップします。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @param actionClass
     *            アクションクラス
     */
    protected void setupResult(S2ActionMapping actionMapping,
            Class<?> actionClass) {
        Result result = actionClass.getAnnotation(Result.class);
        if (result != null) {
            setupResult(actionMapping, result);
            return;
        }
        Results results = actionClass.getAnnotation(Results.class);
        if (results != null) {
            for (Result r : results.value()) {
                setupResult(actionMapping, r);
            }
        }
    }

    /**
     * 遷移先の情報をセットアップします。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @param result
     *            遷移先
     */
    protected void setupResult(S2ActionMapping actionMapping, Result result) {
        ActionForward forward = new ActionForward();
        forward.setName(result.name());
        forward.setPath(result.path());
        forward.setRedirect(result.redirect());
        actionMapping.addForwardConfig(forward);
    }

    /**
     * メソッドの情報をセットアップします。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @param actionClass
     *            アクションクラス
     */
    protected void setupMethod(S2ActionMapping actionMapping,
            Class<?> actionClass) {
        for (Method m : actionClass.getMethods()) {
            Execute execute = m.getAnnotation(Execute.class);
            if (execute != null) {
                if (m.getParameterTypes().length > 0
                        || m.getReturnType() != String.class) {
                    throw new IllegalExecuteMethodRuntimeException(actionClass,
                            m.getName());
                }
                Method validateMethod = null;
                String validate = execute.validate();
                if (!StringUtil.isEmpty(validate)) {
                    validateMethod = actionMapping.getActionBeanDesc()
                            .getMethod(validate);
                    if (validateMethod.getParameterTypes().length > 0
                            || !ActionMessages.class
                                    .isAssignableFrom(validateMethod
                                            .getReturnType())) {
                        throw new IllegalValidateMethodRuntimeException(
                                actionClass, validateMethod.getName());

                    }
                    if (actionMapping.getInput() == null) {
                        throw new InputNotDefinedRuntimeException(actionClass,
                                validateMethod.getName());
                    }
                }

                S2ExecuteConfig executeConfig = new S2ExecuteConfig(m, execute
                        .validator(), validateMethod, execute.saveErrors());
                actionMapping.addExecuteConfig(executeConfig);
            }
        }
        if (actionMapping.getExecuteConfigSize() == 0) {
            throw new ExecuteMethodNotFoundRuntimeException(actionClass);
        }
    }

    /**
     * アクションフォームの情報をセットアップします。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @param actionClass
     *            アクションクラス
     */
    protected void setupActionForm(S2ActionMapping actionMapping,
            Class<?> actionClass) {
        BeanDesc beanDesc = actionMapping.getActionBeanDesc();
        for (int i = 0; i < beanDesc.getPropertyDescSize(); i++) {
            PropertyDesc pd = beanDesc.getPropertyDesc(i);
            Field field = pd.getField();
            if (field == null) {
                continue;
            }
            if (field.getAnnotation(ActionForm.class) != null) {
                actionMapping.setActionFormPropertyDesc(pd);
                return;
            }
        }
    }

    /**
     * リセットの情報をセットアップします。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @param actionClass
     *            アクションクラス
     */
    protected void setupReset(S2ActionMapping actionMapping,
            Class<?> actionClass) {
        BeanDesc beanDesc = actionMapping.getActionFormBeanDesc();
        Method method = beanDesc.getMethodNoException("reset");
        if (method != null) {
            actionMapping.setResetMethod(method);
        }
    }

    /**
     * アクションフォーム設定を作成します。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @return アクションフォーム設定
     */
    protected S2FormBeanConfig createFormBeanConfig(
            S2ActionMapping actionMapping) {

        S2FormBeanConfig formConfig = new S2FormBeanConfig();
        formConfig.setName(actionMapping.getName());
        ActionFormWrapperClass wrapperClass = new ActionFormWrapperClass(
                actionMapping);
        BeanDesc beanDesc = actionMapping.getActionFormBeanDesc();
        for (int i = 0; i < beanDesc.getPropertyDescSize(); i++) {
            PropertyDesc pd = beanDesc.getPropertyDesc(i);
            S2DynaProperty property = new S2DynaProperty(pd);
            wrapperClass.addDynaProperty(property);
        }
        formConfig.setDynaClass(wrapperClass);
        return formConfig;
    }

    /**
     * フォームセットを作成します。
     * 
     * @param actionMapping
     *            アクションマッピング
     * @return フォームセット
     */
    protected FormSet createFormSet(S2ActionMapping actionMapping) {
        FormSet formSet = new FormSet();
        Map<String, Form> forms = new HashMap<String, Form>();
        for (String methodName : actionMapping.getExecuteMethodNames()) {
            Form form = new Form();
            form.setName(actionMapping.getName() + "_" + methodName);
            formSet.addForm(form);
            forms.put(methodName, form);
        }
        BeanDesc beanDesc = actionMapping.getActionFormBeanDesc();
        for (int i = 0; i < beanDesc.getPropertyDescSize(); i++) {
            PropertyDesc pd = beanDesc.getPropertyDesc(i);
            Field field = pd.getField();
            if (field == null) {
                continue;
            }
            for (Annotation anno : field.getDeclaredAnnotations()) {
                processAnnotation(forms, pd.getPropertyName(), anno);
            }
        }
        return formSet;
    }

    /**
     * アノテーションを処理します。
     * 
     * @param forms
     *            メソッド名をキーにしたフォームのマップ
     * @param propertyName
     *            プロパティ名
     * @param annotation
     *            アノテーション
     */
    protected void processAnnotation(Map<String, Form> forms,
            String propertyName, Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation
                .annotationType();
        Annotation metaAnnotation = annotationType
                .getAnnotation(Validator.class);
        if (metaAnnotation == null) {
            return;
        }
        String validatorName = getValidatorName(metaAnnotation);
        Map<String, Object> props = AnnotationUtil.getProperties(annotation);
        registerValidator(forms, propertyName, validatorName, props);
    }

    /**
     * バリデータ名を返します。
     * 
     * @param annotation
     *            検証アノテーション
     * @return バリデータ名
     */
    protected String getValidatorName(Annotation annotation) {
        Class<? extends Annotation> annoType = annotation.annotationType();
        Method m = ClassUtil.getMethod(annoType, "value", null);
        return (String) MethodUtil.invoke(m, annotation, null);
    }

    /**
     * バリデータを登録します。
     * 
     * @param forms
     *            メソッド名をキーにしたフォームのマップ
     * @param propertyName
     *            プロパティ名
     * @param validatorName
     *            バリデータ名
     * @param props
     *            バリデータのプロパティ
     */
    protected void registerValidator(Map<String, Form> forms,
            String propertyName, String validatorName, Map<String, Object> props) {
        org.apache.commons.validator.Field field = createField(propertyName,
                validatorName, props);
        for (Iterator<String> i = forms.keySet().iterator(); i.hasNext();) {
            String methodName = i.next();
            if (!isTarget(methodName, (String) props.get("target"))) {
                continue;
            }
            Form form = forms.get(methodName);
            form.addField(field);
        }
    }

    /**
     * 検証用のフィールドを作成します。
     * 
     * @param propertyName
     *            プロパティ名
     * @param validatorName
     *            バリデータ名
     * @param props
     *            バリデータのプロパティ
     * @return 検証用のフィールド
     */
    protected org.apache.commons.validator.Field createField(
            String propertyName, String validatorName, Map<String, Object> props) {
        org.apache.commons.validator.Field field = new org.apache.commons.validator.Field();
        field.setDepends(validatorName);
        field.setProperty(propertyName);
        Msg msg = (Msg) props.remove("msg");
        if (msg != null) {
            org.apache.commons.validator.Msg m = new org.apache.commons.validator.Msg();
            m.setName(validatorName);
            m.setKey(msg.key());
            String bundle = msg.bundle();
            if (!StringUtil.isEmpty(bundle)) {
                m.setBundle(bundle);
            }
            m.setResource(msg.resource());
            field.addMsg(m);
        }
        Arg[] args = (Arg[]) props.remove("args");
        if (args != null) {
            for (Arg arg : args) {
                org.apache.commons.validator.Arg a = new org.apache.commons.validator.Arg();
                a.setName(validatorName);
                a.setKey(arg.key());
                String bundle = arg.bundle();
                if (!StringUtil.isEmpty(bundle)) {
                    a.setBundle(bundle);
                }
                a.setResource(arg.resource());
                a.setPosition(arg.position());
                field.addArg(a);
            }
        }
        for (Iterator<String> i = props.keySet().iterator(); i.hasNext();) {
            String name = i.next();
            if (name.equals("target")) {
                continue;
            }
            Object value = props.get(name);
            String jsType = Var.JSTYPE_STRING;
            if (value instanceof Number) {
                jsType = Var.JSTYPE_INT;
            } else if (name.equals("mask")) {
                jsType = Var.JSTYPE_REGEXP;
            }
            field.addVar(name, value.toString(), jsType);
        }
        return field;
    }

    /**
     * 対象のメソッドかどうかを返します。
     * 
     * @param methodName
     *            メソッド名
     * @param target
     *            メソッド名がカンマ区切りで指定されたもの
     * @return 対象のメソッドかどうか
     */
    protected boolean isTarget(String methodName, String target) {
        if (StringUtil.isEmpty(target)) {
            return true;
        }
        String[] names = StringUtil.split(target, ", ");
        for (String name : names) {
            if (methodName.equals(name.trim())) {
                return true;
            }
        }
        return false;
    }
}