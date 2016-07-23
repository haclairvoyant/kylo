package com.thinkbiganalytics.policy;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.thinkbiganalytics.annotations.AnnotatedFieldProperty;
import com.thinkbiganalytics.annotations.AnnotationFieldNameResolver;
import com.thinkbiganalytics.policy.rest.model.BaseUiPolicyRule;
import com.thinkbiganalytics.policy.rest.model.FieldRuleProperty;
import com.thinkbiganalytics.policy.rest.model.FieldRulePropertyBuilder;
import com.thinkbiganalytics.policy.validation.PolicyPropertyTypes;
import com.thinkbiganalytics.rest.model.LabelValue;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by sr186054 on 4/21/16.
 */
public abstract class BasePolicyAnnotationTransformer<U extends BaseUiPolicyRule, P extends Object, A extends Annotation>
    implements PolicyTransformer<U, P, A> {

    private static final Logger log = LoggerFactory.getLogger(BasePolicyAnnotationTransformer.class);

    private List<FieldRuleProperty> getUiProperties(P policy) {
        AnnotationFieldNameResolver annotationFieldNameResolver = new AnnotationFieldNameResolver(PolicyProperty.class);
        List<AnnotatedFieldProperty> list = annotationFieldNameResolver.getProperties(policy.getClass());
        List<FieldRuleProperty> properties = new ArrayList<>();
        Map<String, Integer> groupOrder = new HashMap<>();
        Map<String, List<FieldRuleProperty>> groupedProperties = new HashMap<>();
        if (hasConstructor(policy.getClass())) {

            for (AnnotatedFieldProperty<PolicyProperty> annotatedFieldProperty : list) {
                PolicyProperty prop = annotatedFieldProperty.getAnnotation();
                String value = null;
                try {
                    Object fieldValue = FieldUtils.readField(annotatedFieldProperty.getField(), policy, true);
                    if (fieldValue != null) {
                        value = fieldValue.toString();
                    }
                } catch (IllegalAccessException e) {

                }
                String group = prop.group();
                Integer order = 0;
                if (!groupOrder.containsKey(group)) {
                    groupOrder.put(group, order);
                }
                order = groupOrder.get(group);
                order++;
                groupOrder.put(group, order);
                FieldRuleProperty rule = new FieldRulePropertyBuilder(prop.name()).displayName(
                    StringUtils.isNotBlank(prop.displayName()) ? prop.displayName() : prop.name()).hint(prop.hint())
                    .type(PolicyPropertyTypes.PROPERTY_TYPE.valueOf(prop.type().name()))
                    .objectProperty(annotatedFieldProperty.getName())
                    .placeholder(prop.placeholder())
                    .value(value)
                    .required(prop.required())
                    .group(group)
                    .groupOrder(order)
                    .addSelectableValues(convertToLabelValue(prop.selectableValues()))
                    .addSelectableValues(convertToLabelValue(prop.labelValues())).build();
                properties.add(rule);
                if (!group.equals("")) {
                    if (!groupedProperties.containsKey(group)) {
                        groupedProperties.put(group, new ArrayList<FieldRuleProperty>());
                    }
                    groupedProperties.get(group).add(rule);
                }
            }
            //update layout property
            for (Collection<FieldRuleProperty> groupProps : groupedProperties.values()) {
                for (FieldRuleProperty property : groupProps) {
                    property.setLayout("row");
                }
            }

        }
        return properties;
    }


    public List<FieldRuleProperty> getUiProperties(Class<P> policyClass) {
        AnnotationFieldNameResolver annotationFieldNameResolver = new AnnotationFieldNameResolver(PolicyProperty.class);
        List<AnnotatedFieldProperty> list = annotationFieldNameResolver.getProperties(policyClass);
        List<FieldRuleProperty> properties = new ArrayList<>();
        Map<String, List<FieldRuleProperty>> groupedProperties = new HashMap<>();
        if (hasConstructor(policyClass)) {
            Map<String, Integer> groupOrder = new HashMap<>();
            for (AnnotatedFieldProperty<PolicyProperty> annotatedFieldProperty : list) {
                PolicyProperty prop = annotatedFieldProperty.getAnnotation();
                String value = StringUtils.isBlank(prop.value()) ? null : prop.value();
                String group = prop.group();
                Integer order = 0;
                if (!groupOrder.containsKey(group)) {
                    groupOrder.put(group, order);
                }
                order = groupOrder.get(group);
                order++;
                groupOrder.put(group, order);

                FieldRuleProperty rule = new FieldRulePropertyBuilder(prop.name()).displayName(
                    StringUtils.isNotBlank(prop.displayName()) ? prop.displayName() : prop.name()).hint(prop.hint())
                    .type(PolicyPropertyTypes.PROPERTY_TYPE.valueOf(prop.type().name()))
                    .objectProperty(annotatedFieldProperty.getName())
                    .placeholder(prop.placeholder())
                    .value(value)
                    .required(prop.required())
                    .group(group)
                    .groupOrder(order)
                    .addSelectableValues(convertToLabelValue(prop.selectableValues()))
                    .addSelectableValues(convertToLabelValue(prop.labelValues())).build();
                properties.add(rule);
                if (!group.equals("")) {
                    if (!groupedProperties.containsKey(group)) {
                        groupedProperties.put(group, new ArrayList<FieldRuleProperty>());
                    }
                    groupedProperties.get(group).add(rule);
                }
            }
            //update layout property
            for (Collection<FieldRuleProperty> groupProps : groupedProperties.values()) {
                for (FieldRuleProperty property : groupProps) {
                    property.setLayout("row");
                }
            }
        }
        return properties;
    }

    public List<FieldRuleProperty> findPropertiesMatchingRenderType(List<FieldRuleProperty> properties, final String type) {
        if (StringUtils.isNotBlank(type)) {
            return findPropertiesMatchingRenderTypes(properties, new String[]{type});
        }
        return null;
    }

    public List<FieldRuleProperty> findPropertiesMatchingRenderTypes(List<FieldRuleProperty> properties, final String[] types) {
        final List list = Arrays.asList(types);
        return Lists.newArrayList(Iterables.filter(properties, new Predicate<FieldRuleProperty>() {
            @Override
            public boolean apply(FieldRuleProperty fieldRuleProperty) {
                return list.contains(fieldRuleProperty.getType());
            }
        }));
    }

    public List<FieldRuleProperty> findPropertiesForRulesetMatchingRenderType(List<? extends BaseUiPolicyRule> rules, final String type) {

        List<FieldRuleProperty> properties = new ArrayList<>();
        for (BaseUiPolicyRule rule : rules) {
            properties.addAll(rule.getProperties());
        }
        return findPropertiesMatchingRenderType(properties, type);
    }

    public List<FieldRuleProperty> findPropertiesForRulesetMatchingRenderTypes(List<? extends BaseUiPolicyRule> rules, final String[] types) {

        List<FieldRuleProperty> properties = new ArrayList<>();
        for (BaseUiPolicyRule rule : rules) {
            properties.addAll(rule.getProperties());
        }
        return findPropertiesMatchingRenderTypes(properties, types);
    }


    public abstract U buildUiModel(A annotation, P policy, List<FieldRuleProperty> properties);

    public abstract Class<A> getAnnotationClass();

    @Override
    public U toUIModel(P standardizationPolicy) {
        Annotation annotation = standardizationPolicy.getClass().getAnnotation(getAnnotationClass());
        List<FieldRuleProperty> properties = getUiProperties(standardizationPolicy);
        U rule = buildUiModel((A) annotation, standardizationPolicy, properties);
        return rule;
    }

    /**
     * override to do anything special to the resulting obj from the UI
     */
    public void afterFromUiModel(P policy, U uiModel) {

    }

    @Override
    public P fromUiModel(U rule)
        throws PolicyTransformException {
        try {
            P standardizationPolicy = createClass(rule);

            if (hasConstructor(standardizationPolicy.getClass())) {

                for (FieldRuleProperty property : rule.getProperties()) {
                    String field = property.getObjectProperty();
                    String value = property.getStringValue();
                    Field f = FieldUtils.getField(standardizationPolicy.getClass(), field, true);
                    Object objectValue = convertStringToObject(value, f.getType());
                    BeanUtils.setProperty(standardizationPolicy, field, objectValue);
                }
            }
            afterFromUiModel(standardizationPolicy, rule);
            return standardizationPolicy;
        } catch (Exception e) {
            throw new PolicyTransformException(e);
        }
    }

    private Object getPropertyValue(BaseUiPolicyRule rule, Class<P> standardizationPolicyClass, PolicyPropertyRef reference) {
        for (FieldRuleProperty property : rule.getProperties()) {
            String name = property.getName();
            if (name.equalsIgnoreCase(reference.name())) {
                String field = property.getObjectProperty();
                String value = property.getStringValue();
                Field f = FieldUtils.getField(standardizationPolicyClass, field, true);
                Object objectValue = convertStringToObject(value, f.getType());
                return objectValue;
            }
        }
        return null;
    }


    private boolean hasConstructor(Class<?> policyClass) {
        return policyClass.getConstructors().length > 0;
    }

    private P createClass(BaseUiPolicyRule rule)
        throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException,
               IllegalAccessException {
        P standardizationPolicy = null;
        String classType = rule.getObjectClassType();
        Class<P> standardizationPolicyClass = (Class<P>) Class.forName(classType);

        Constructor constructor = null;
        Object[] paramValues = null;
        boolean hasConstructor = false;
        for (Constructor con : standardizationPolicyClass.getConstructors()) {
            hasConstructor = true;
            int parameterSize = con.getParameterTypes().length;
            paramValues = new Object[parameterSize];
            for (int p = 0; p < parameterSize; p++) {
                Type pgtype = con.getGenericParameterTypes()[p];
                Annotation[] annotations = con.getParameterAnnotations()[p];
                Object paramValue = null;
                for (Annotation a : annotations) {
                    if (a instanceof PolicyPropertyRef) {
                        // this is the one we want
                        if (constructor == null) {
                            constructor = con;
                        }
                        //find the value associated to this property
                        paramValue = getPropertyValue(rule, standardizationPolicyClass, (PolicyPropertyRef) a);

                    }
                }
                paramValues[p] = paramValue;
            }
            if (constructor != null) {
                //exit once we find a constructor with @PropertyRef
                break;
            }

        }

        if (constructor != null) {
            //call that constructor
            standardizationPolicy = ConstructorUtils.invokeConstructor(standardizationPolicyClass, paramValues);
        } else {
            //if the class has no public constructor then attempt to call the static instance method
            if (!hasConstructor) {
                //if the class has a static "instance" method on it then call that
                try {
                    standardizationPolicy = (P) MethodUtils.invokeStaticMethod(standardizationPolicyClass, "instance", null);
                } catch (NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    standardizationPolicy = standardizationPolicyClass.newInstance();
                }
            } else {
                //attempt to create a new instance
                standardizationPolicy = standardizationPolicyClass.newInstance();
            }
        }

        return standardizationPolicy;

    }


    private List<LabelValue> convertToLabelValue(String[] values) {
        if (values != null) {
            List<LabelValue> list = new ArrayList<>();
            for (String value : values) {
                LabelValue labelValue = new LabelValue();
                labelValue.setLabel(value);
                labelValue.setValue(value);
                list.add(labelValue);
            }
            return list;
        }
        return null;
    }


    private LabelValue convertToLabelValue(PropertyLabelValue propertyLabelValue) {
        if (propertyLabelValue != null) {
            LabelValue labelValue = new LabelValue();
            labelValue.setLabel(propertyLabelValue.label());
            labelValue.setValue(propertyLabelValue.value());
            return labelValue;
        }
        return null;
    }

    private List<LabelValue> convertToLabelValue(PropertyLabelValue[] propertyLabelValues) {
        List<LabelValue> labelValues = null;
        if (propertyLabelValues != null) {
            for (PropertyLabelValue propertyLabelValue : propertyLabelValues) {
                if (labelValues == null) {
                    labelValues = new ArrayList<>();
                }
                LabelValue labelValue = convertToLabelValue((propertyLabelValue));
                if (labelValue != null) {
                    labelValues.add(labelValue);
                }
            }
        }
        return labelValues;
    }


    public Object convertStringToObject(String value, Class type) {
        if (type.isEnum()) {
            return Enum.valueOf(type, value);
        } else if (StringUtils.isBlank(value)) {
            return null;
        }
        if (String.class.equals(type)) {
            return value;
        } else if (Number.class.equals(type)) {
            return NumberUtils.createNumber(value);
        } else if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return new Integer(value);
        } else if (Long.class.equals(type) || Long.TYPE.equals(type)) {
            return Long.valueOf(value);
        } else if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return Double.valueOf(value);
        } else if (Float.class.equals(type) || Float.TYPE.equals(type)) {
            return Float.valueOf(value);
        } else if (Pattern.class.equals(type)) {
            return Pattern.compile(value);
        } else if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return BooleanUtils.toBoolean(value);
        } else {
            throw new IllegalArgumentException("Unable to convert the value " + value + " to an object of type " + type.getName());
        }

    }

}
