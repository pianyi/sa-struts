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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.validator.Form;
import org.apache.commons.validator.FormSet;
import org.apache.commons.validator.ValidatorResources;
import org.apache.commons.validator.Var;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.validator.ValidatorPlugIn;
import org.seasar.extension.unit.S2TestCase;
import org.seasar.framework.beans.MethodNotFoundRuntimeException;
import org.seasar.framework.util.tiger.AnnotationUtil;
import org.seasar.struts.annotation.ActionForm;
import org.seasar.struts.annotation.Arg;
import org.seasar.struts.annotation.Execute;
import org.seasar.struts.annotation.Input;
import org.seasar.struts.annotation.Msg;
import org.seasar.struts.annotation.Required;
import org.seasar.struts.annotation.Result;
import org.seasar.struts.annotation.Results;
import org.seasar.struts.annotation.Validator;
import org.seasar.struts.annotation.Validwhen;
import org.seasar.struts.config.S2ActionMapping;
import org.seasar.struts.config.S2ExecuteConfig;
import org.seasar.struts.config.S2FormBeanConfig;
import org.seasar.struts.config.S2ModuleConfig;
import org.seasar.struts.enums.SaveType;
import org.seasar.struts.exception.ExecuteMethodNotFoundRuntimeException;
import org.seasar.struts.exception.IllegalExecuteMethodRuntimeException;
import org.seasar.struts.exception.IllegalValidateMethodRuntimeException;
import org.seasar.struts.exception.InputNotDefinedRuntimeException;
import org.seasar.struts.util.ValidatorResourcesUtil;

/**
 * @author higa
 * 
 */
public class ActionCustomizerTest extends S2TestCase {

    private ActionCustomizer customizer = new ActionCustomizer();

    private S2ModuleConfig moduleConfig = new S2ModuleConfig("");

    @Override
    public void setUp() {
        getServletContext().setAttribute(Globals.SERVLET_KEY, "/*");
        getServletContext().setAttribute(Globals.MODULE_KEY, moduleConfig);
        register(BbbAction.class, "aaa_bbbAction");
        getServletContext().setAttribute(ValidatorPlugIn.VALIDATOR_KEY,
                new ValidatorResources());
    }

    /**
     * @throws Exception
     */
    public void testCustomize_actionConfig() throws Exception {
        customizer.customize(getComponentDef("aaa_bbbAction"));
        assertNotNull(moduleConfig.findActionConfig("/aaa/bbb"));
    }

    /**
     * @throws Exception
     */
    public void testCustomize_formBeanConfig() throws Exception {
        customizer.customize(getComponentDef("aaa_bbbAction"));
        assertNotNull(moduleConfig.findFormBeanConfig("aaa_bbbActionForm"));
    }

    /**
     * @throws Exception
     */
    public void testCustomize_formSet() throws Exception {
        customizer.customize(getComponentDef("aaa_bbbAction"));
        assertNotNull(ValidatorResourcesUtil.getValidatorResources().getForm(
                Locale.getDefault(), "aaa_bbbActionForm_execute"));
    }

    /**
     * @throws Exception
     */
    public void testCreateActionMapping_path() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        assertNotNull(actionMapping);
        assertEquals("/aaa/bbb", actionMapping.getPath());
    }

    /**
     * @throws Exception
     */
    public void testCreateActionMapping_componentDef() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        assertNotNull(actionMapping.getComponentDef());
    }

    /**
     * @throws Exception
     */
    public void testCreateActionMapping_name() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        assertEquals("aaa_bbbActionForm", actionMapping.getName());
    }

    /**
     * @throws Exception
     */
    public void testCreateActionMapping_type() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        assertEquals(BbbAction.class.getName(), actionMapping.getType());
    }

    /**
     * @throws Exception
     */
    public void testSetupInput() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        assertEquals("input", actionMapping.getInput());
        ForwardConfig forwardConfig = actionMapping.findForwardConfig("input");
        assertEquals("/aaa/input.jsp", forwardConfig.getPath());
        assertFalse(forwardConfig.getRedirect());
    }

    /**
     * @throws Exception
     */
    public void testSetupResult() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        ForwardConfig forwardConfig = actionMapping
                .findForwardConfig("success");
        assertNotNull(forwardConfig);
        assertEquals("/aaa/bbb.jsp", forwardConfig.getPath());
        assertFalse(forwardConfig.getRedirect());
    }

    /**
     * @throws Exception
     */
    public void testSetupResult_results() throws Exception {
        register(CccAction.class, "aaa_cccAction");
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_cccAction"));
        ForwardConfig forwardConfig = actionMapping
                .findForwardConfig("success");
        assertNotNull(forwardConfig);
        assertEquals("/aaa/bbb.jsp", forwardConfig.getPath());
        assertFalse(forwardConfig.getRedirect());
        forwardConfig = actionMapping.findForwardConfig("success2");
        assertNotNull(forwardConfig);
        assertEquals("/aaa/bbb2.jsp", forwardConfig.getPath());
        assertFalse(forwardConfig.getRedirect());
    }

    /**
     * @throws Exception
     */
    public void testSetupMethod() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        S2ExecuteConfig executeConfig = actionMapping
                .getExecuteConfig("execute");
        assertNotNull(executeConfig);
        assertNotNull(executeConfig.getMethod());
        assertFalse(executeConfig.isValidator());
        assertNotNull(executeConfig.getValidateMethod());
        assertEquals(SaveType.REQUEST, executeConfig.getSaveErrors());
        assertEquals(1, actionMapping.getExecuteConfigSize());
    }

    /**
     * @throws Exception
     */
    public void testSetupMethod_illegalExecuteMethod() throws Exception {
        register(DddAction.class, "aaa_dddAction");
        try {
            customizer.createActionMapping(getComponentDef("aaa_dddAction"));
            fail();
        } catch (IllegalExecuteMethodRuntimeException e) {
            System.out.println(e);
            assertEquals(DddAction.class, e.getActionClass());
            assertEquals("execute", e.getExecuteMethodName());
        }
    }

    /**
     * @throws Exception
     */
    public void testSetupMethod_executeMethodEmpty() throws Exception {
        register(EeeAction.class, "aaa_eeeAction");
        try {
            customizer.createActionMapping(getComponentDef("aaa_eeeAction"));
            fail();
        } catch (ExecuteMethodNotFoundRuntimeException e) {
            System.out.println(e);
            assertEquals(EeeAction.class, e.getTargetClass());
        }
    }

    /**
     * @throws Exception
     */
    public void testSetupMethod_inputNotDefined() throws Exception {
        register(FffAction.class, "aaa_fffAction");
        try {
            customizer.createActionMapping(getComponentDef("aaa_fffAction"));
            fail();
        } catch (InputNotDefinedRuntimeException e) {
            System.out.println(e);
            assertEquals(FffAction.class, e.getActionClass());
            assertEquals("validate", e.getValidateMethodName());
        }
    }

    /**
     * @throws Exception
     */
    public void testSetupMethod_illegalValidateMethod() throws Exception {
        register(GggAction.class, "aaa_gggAction");
        try {
            customizer.createActionMapping(getComponentDef("aaa_gggAction"));
            fail();
        } catch (IllegalValidateMethodRuntimeException e) {
            System.out.println(e);
            assertEquals(GggAction.class, e.getActionClass());
            assertEquals("validate", e.getValidateMethodName());
        }
    }

    /**
     * @throws Exception
     */
    public void testSetupMethod_validateNotFound() throws Exception {
        register(HhhAction.class, "aaa_hhhAction");
        try {
            customizer.createActionMapping(getComponentDef("aaa_hhhAction"));
            fail();
        } catch (MethodNotFoundRuntimeException e) {
            System.out.println(e);
            assertEquals(HhhAction.class, e.getTargetClass());
            assertEquals("validate", e.getMethodName());
        }
    }

    /**
     * @throws Exception
     */
    public void testSetupActionForm() throws Exception {
        register(CccAction.class, "aaa_cccAction");
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_cccAction"));
        assertNotNull(actionMapping.getActionFormPropertyDesc());
    }

    /**
     * @throws Exception
     */
    public void testSetupReset_action() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        assertNotNull(actionMapping.getResetMethod());
    }

    /**
     * @throws Exception
     */
    public void testSetupReset_actionForm() throws Exception {
        register(CccAction.class, "aaa_cccAction");
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_cccAction"));
        assertNotNull(actionMapping.getResetMethod());
    }

    /**
     * @throws Exception
     */
    public void testCreateFormBeanConfig_name() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        S2FormBeanConfig formConfig = customizer
                .createFormBeanConfig(actionMapping);
        assertNotNull(formConfig);
        assertEquals("aaa_bbbActionForm", formConfig.getName());
    }

    /**
     * @throws Exception
     */
    public void testCreateFormBeanConfig_dynaClass() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        S2FormBeanConfig formConfig = customizer
                .createFormBeanConfig(actionMapping);
        DynaClass dynaClass = formConfig.getDynaClass();
        assertNotNull(dynaClass);
        assertNotNull(dynaClass.getDynaProperty("hoge"));
    }

    /**
     * @throws Exception
     */
    public void testGetValidatorName() throws Exception {
        Field field = BbbAction.class.getDeclaredField("hoge");
        Required r = field.getAnnotation(Required.class);
        Validator v = r.annotationType().getAnnotation(Validator.class);
        assertEquals("required", customizer.getValidatorName(v));
    }

    /**
     * @throws Exception
     */
    public void testIsTarget() throws Exception {
        assertTrue(customizer.isTarget("hoge", ""));
        assertTrue(customizer.isTarget("hoge", " hoge, foo"));
        assertFalse(customizer.isTarget("bar", "hoge, foo"));
    }

    /**
     * @throws Exception
     */
    public void testCreateField() throws Exception {
        Field field = BbbAction.class.getDeclaredField("hoge");
        Required r = field.getAnnotation(Required.class);
        Map<String, Object> props = AnnotationUtil.getProperties(r);
        org.apache.commons.validator.Field f = customizer.createField("hoge",
                "required", props);
        assertEquals("hoge", f.getProperty());
        assertEquals("required", f.getDepends());
        org.apache.commons.validator.Msg m = f.getMessage("required");
        assertNotNull(m);
        assertEquals("errors.required", m.getKey());
        assertEquals("required", m.getName());
        assertTrue(m.isResource());
        assertNull(m.getBundle());
        org.apache.commons.validator.Arg a = f.getArg("required", 0);
        assertNotNull(a);
        assertEquals("labels.hoge", a.getKey());
        assertEquals("required", a.getName());
        assertTrue(a.isResource());
        assertNull(a.getBundle());
    }

    /**
     * @throws Exception
     */
    public void testCreateField_var() throws Exception {
        Field field = BbbAction.class.getDeclaredField("hoge2");
        Validwhen v = field.getAnnotation(Validwhen.class);
        Map<String, Object> props = AnnotationUtil.getProperties(v);
        org.apache.commons.validator.Field f = customizer.createField("hoge2",
                "validwhen", props);
        org.apache.commons.validator.Var var = f.getVar("test");
        assertNotNull(var);
        assertEquals("test", var.getName());
        assertEquals("true", var.getValue());
        assertEquals(Var.JSTYPE_STRING, var.getJsType());
    }

    /**
     * @throws Exception
     */
    public void testRegisterValidator() throws Exception {
        Map<String, Form> forms = new HashMap<String, Form>();
        Form form = new Form();
        forms.put("execute", form);
        Form form2 = new Form();
        forms.put("execute2", form2);
        Field field = BbbAction.class.getDeclaredField("hoge");
        Required r = field.getAnnotation(Required.class);
        Map<String, Object> props = AnnotationUtil.getProperties(r);
        customizer.registerValidator(forms, "hoge", "required", props);
        assertNotNull(form.getField("hoge"));
        assertNotNull(form2.getField("hoge"));
    }

    /**
     * @throws Exception
     */
    public void testRegisterValidator_target() throws Exception {
        Map<String, Form> forms = new HashMap<String, Form>();
        Form form = new Form();
        forms.put("execute", form);
        Form form2 = new Form();
        forms.put("execute2", form2);
        Field field = BbbAction.class.getDeclaredField("hoge2");
        Validwhen v = field.getAnnotation(Validwhen.class);
        Map<String, Object> props = AnnotationUtil.getProperties(v);
        customizer.registerValidator(forms, "hoge2", "validwhen", props);
        assertNotNull(form.getField("hoge2"));
        assertNull(form2.getField("hoge2"));
    }

    /**
     * @throws Exception
     */
    public void testProcessAnnotation() throws Exception {
        Map<String, Form> forms = new HashMap<String, Form>();
        Form form = new Form();
        forms.put("execute", form);
        Field field = BbbAction.class.getDeclaredField("hoge");
        Required r = field.getAnnotation(Required.class);
        customizer.processAnnotation(forms, "hoge", r);
        assertNotNull(form.getField("hoge"));
    }

    /**
     * @throws Exception
     */
    public void testCreateFormSet() throws Exception {
        S2ActionMapping actionMapping = customizer
                .createActionMapping(getComponentDef("aaa_bbbAction"));
        FormSet formSet = customizer.createFormSet(actionMapping);
        assertNotNull(formSet);
        Form form = formSet.getForm("aaa_bbbActionForm_execute");
        assertNotNull(form);
        org.apache.commons.validator.Field f = form.getField("hoge");
        assertEquals("hoge", f.getProperty());
        assertEquals("required", f.getDepends());
    }

    /**
     * 
     */
    @Input(path = "/aaa/input.jsp")
    @Result(path = "/aaa/bbb.jsp")
    public static class BbbAction {

        /**
         * 
         */
        @Required(args = @Arg(key = "labels.hoge"))
        public String hoge;

        /**
         * 
         */
        @Validwhen(test = "true", msg = @Msg(key = "errors.validwhen"), target = "execute")
        public boolean hoge2;

        /**
         * 
         */
        public List<String> hoge3;

        /**
         * @return
         */
        @Execute(validator = false, validate = "validate")
        public String execute() {
            return "success";
        }

        /**
         * @return
         */
        public ActionMessages validate() {
            return null;
        }

        /**
         * 
         */
        public void reset() {
        }
    }

    /**
     * 
     */
    @Input(path = "/aaa/input.jsp")
    @Results( { @Result(name = "success", path = "/aaa/bbb.jsp"),
            @Result(name = "success2", path = "/aaa/bbb2.jsp") })
    public static class CccAction {
        /**
         * 
         */
        @ActionForm
        public CccActionForm cccActionForm;

        /**
         * @return
         */
        @Execute
        public String execute() {
            return "success";
        }
    }

    /**
     * 
     */
    public static class DddAction {
        /**
         * @return
         */
        @Execute
        public void execute() {
        }
    }

    /**
     * 
     */
    public static class EeeAction {
        /**
         * @return
         */
        public void execute() {
        }
    }

    /**
     * 
     */
    public static class FffAction {
        /**
         * @return
         */
        @Execute(validate = "validate")
        public String execute() {
            return "success";
        }

        /**
         * @return
         */
        public ActionMessages validate() {
            return null;
        }
    }

    /**
     * 
     */
    public static class GggAction {
        /**
         * @return
         */
        @Execute(validate = "validate")
        public String execute() {
            return "success";
        }

        /**
         * @return
         */
        public String validate() {
            return null;
        }
    }

    /**
     * 
     */
    @Input(path = "/aaa/input.jsp")
    @Result(path = "/aaa/bbb.jsp")
    public static class HhhAction {
        /**
         * @return
         */
        @Execute(validate = "validate")
        public String execute() {
            return "success";
        }

        /**
         * @return
         */
        public ActionMessages validate2() {
            return null;
        }
    }

    /**
     * 
     */
    public static class CccActionForm {
        /**
         * 
         */
        public void reset() {
        }
    }
}