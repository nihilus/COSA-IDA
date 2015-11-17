package com.tosnos.cosa.android;

import android.content.res.AXmlResourceParser;
import org.xmlpull.v1.XmlPullParser;
import test.AXMLPrinter;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public class AndroidManifest {
    public static final String ATTRIBUTE_PACKAGE = "package";
    public static final String ATTRIBUTE_VERSION_CODE = "versionCode";
    public static final String ATTRIBUTE_VERSION_NAME = "versionName";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_REQUIRED = "required";
    public static final String ATTRIBUTE_GLESVERSION = "glEsVersion";
    public static final String ATTRIBUTE_PROCESS = "process";
    public static final String ATTRIBUTE_DEBUGGABLE = "debuggable";
    public static final String ATTRIBUTE_LABEL = "label";
    public static final String ATTRIBUTE_ICON = "icon";
    public static final String ATTRIBUTE_MIN_SDK_VERSION = "minSdkVersion";
    public static final String ATTRIBUTE_TARGET_SDK_VERSION = "targetSdkVersion";
    public static final String ATTRIBUTE_TARGET_PACKAGE = "targetPackage";
    public static final String ATTRIBUTE_TARGET_ACTIVITY = "targetActivity";
    public static final String ATTRIBUTE_MANAGE_SPACE_ACTIVITY = "manageSpaceActivity";
    public static final String ATTRIBUTE_EXPORTED = "exported";
    public static final String ATTRIBUTE_RESIZEABLE = "resizeable";
    public static final String ATTRIBUTE_ANYDENSITY = "anyDensity";
    public static final String ATTRIBUTE_SMALLSCREENS = "smallScreens";
    public static final String ATTRIBUTE_NORMALSCREENS = "normalScreens";
    public static final String ATTRIBUTE_LARGESCREENS = "largeScreens";
    public static final String ATTRIBUTE_REQ_5WAYNAV = "reqFiveWayNav";
    public static final String ATTRIBUTE_REQ_NAVIGATION = "reqNavigation";
    public static final String ATTRIBUTE_REQ_HARDKEYBOARD = "reqHardKeyboard";
    public static final String ATTRIBUTE_REQ_KEYBOARDTYPE = "reqKeyboardType";
    public static final String ATTRIBUTE_REQ_TOUCHSCREEN = "reqTouchScreen";

    private int sdkTargetVersion;
    private int minSdkVersion;

    private String packageName = null;
    private String versionName = null;
    private String apkFileName = null;
    private Component application = null;
    private Component mainActivity = null;
    private Set<Component> components = new LinkedHashSet<Component>();

    public AndroidManifest(InputStream is) {
        try {
            AXmlResourceParser parser = new AXmlResourceParser();
            parser.open(is);
            boolean inApplication = false;
            Component component = null;

            while (true) {
                int type = parser.next();

                if (type == XmlPullParser.END_DOCUMENT) {
                    break;
                }

                String name = parser.getName();

                switch (type) {
                    case XmlPullParser.START_TAG: {
                        if ("manifest".equals(name)) {
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                if (ATTRIBUTE_VERSION_NAME.equals(parser.getAttributeName(i))) {
                                    versionName = parser.getAttributeValue(i);
                                } else if (ATTRIBUTE_PACKAGE.equals(parser.getAttributeName(i))) {
                                    packageName = parser.getAttributeValue(i);
                                }
                            }
                        } else if ("application".equals(name)) {
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                if (ATTRIBUTE_NAME.equals(parser.getAttributeName(i))) {
                                    String applicationName = parser.getAttributeValue(i);
                                    if (applicationName.charAt(0) == '.') {
                                        applicationName = packageName + applicationName;
                                    } else if (!applicationName.contains(".")) {
                                        applicationName = packageName + "." + applicationName;
                                    }
                                    component = new Component(name);
                                    addComponent(component);
                                    component.setComponentClassName(applicationName);
                                    break;
                                }
                            }
                            inApplication = true;
                        } else if (inApplication && parser.getDepth() == 3) { // for component

                            if ("supports-screens".equals(name) || "compatible-screens".equals(name) || "intent-filter".equals(name)) {
                                continue;
                            }

                            component = new Component(name);
                            addComponent(component);

                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                String attributeName = parser.getAttributeName(i);
                                String attributeValue = AXMLPrinter.getAttributeValue(parser, i);

                                if (ATTRIBUTE_NAME.equals(attributeName)) {
                                    String componentName = attributeValue;
                                    if (componentName.charAt(0) == '.') {
                                        componentName = packageName + componentName;
                                    } else if (!componentName.contains(".")) {
                                        componentName = packageName + "." + componentName;
                                    }
                                    component.setComponentClassName(componentName);
                                } else if (ATTRIBUTE_TARGET_ACTIVITY.equals(attributeName)) {
                                    String componentName = attributeValue;
                                    if (componentName.charAt(0) == '.') {
                                        componentName = packageName + componentName;
                                    } else if (!componentName.contains(".")) {
                                        componentName = packageName + "." + componentName;
                                    }
                                    component.setTargetActivityName(componentName);
                                } else if ("".equals(attributeName)) {
                                    String componentName = attributeValue;
                                    if (component.getName() == null && componentName.length() > 0) {
                                        if (componentName.charAt(0) == '.') {
                                            componentName = packageName + componentName;
                                        } else if (!componentName.contains(".")) {
                                            componentName = packageName + "." + componentName;
                                        }
                                        component.setComponentClassName(componentName);
                                    }
                                }
                            }

                            if (component.getName() == null) {
                                System.out.println(name);
                            }
                        } else if (component != null && "action".equals(name)) {
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                if (ATTRIBUTE_NAME.equals(parser.getAttributeName(i)) && "android.intent.action.MAIN".equals(parser.getAttributeValue(i))) {
                                    mainActivity = component;
                                }
                            }
                        } else if (parser.getDepth() == 2 && "uses-sdk".equals(name)) {
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                String attributeName = parser.getAttributeName(i);
                                String attributeValue = AXMLPrinter.getAttributeValue(parser, i);
                                if (ATTRIBUTE_TARGET_SDK_VERSION.equals(attributeName)) {
                                    sdkTargetVersion = Integer.parseInt(attributeValue);
                                } else if (ATTRIBUTE_MIN_SDK_VERSION.equals(attributeName)) {
                                    minSdkVersion = Integer.parseInt(attributeValue);
                                }
                            }
                        }
                        break;
                    }

                    case XmlPullParser.END_TAG: {
                        if ("application".equals(name)) {
                            inApplication = false;
                        } else if (inApplication && parser.getDepth() == 3) {
                            component = null;
                        }
                        break;
                    }

                    case XmlPullParser.TEXT:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error when looking for manifest in apk: " + e);
        }
    }

    public int getSdkTargetVersion() {
        return sdkTargetVersion;
    }

    public int getMinSdkVersion() {
        return minSdkVersion;
    }

    public Component getApplication() {
        return application;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getApkFileName() {
        return apkFileName;
    }

    public Component getMainActivity() {
        return mainActivity;
    }

    public Set<Component> getComponents() {
        return components;
    }

    public void addComponent(Component component) {
        if (component != null) {
            components.add(component);
        }
    }
}


