package com.novoda.buildproperties

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class BuildPropertiesPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        boolean isAndroidApp = project.plugins.hasPlugin('com.android.application')
        if (!isAndroidApp) {
            throw new GradleException("This plugin can be applied only to an Android application project")
        }

        project.extensions.add('buildProperties', project.container(BuildProperties))

        def android = project.extensions.findByName("android")
        android.defaultConfig.with {
            addBuildConfigSupportTo(it, project)
            addResValueSupportTo(it, project)
        }

        android.productFlavors.all {
            addBuildConfigSupportTo(it, project)
            addResValueSupportTo(it, project)
        }

        android.buildTypes.all {
            addBuildConfigSupportTo(it, project)
            addResValueSupportTo(it, project)
        }

        android.signingConfigs.all {
            addSigningConfigSupportTo(it, project)
        }
    }

    private void addBuildConfigSupportTo(target, Project project) {
        def buildConfigField = { String type, String name, Closure<String> value ->
            target.buildConfigField type, name, value()
        }

        target.ext.buildConfigBoolean = { String name, Boolean value ->
            buildConfigField('boolean', name, { Boolean.toString(value) })
        }
        target.ext.buildConfigInt = { String name, Integer value ->
            buildConfigField('int', name, { Integer.toString(value) })
        }
        target.ext.buildConfigLong = { String name, Long value ->
            buildConfigField('long', name, { "${Long.toString(value)}L" })
        }
        target.ext.buildConfigDouble = { String name, Double value ->
            buildConfigField('double', name, { Double.toString(value) })
        }
        target.ext.buildConfigString = { String name, String value ->
            buildConfigField('String', name, { "\"$value\"" })
        }
        target.ext.buildConfigProperty = { String name = null, BuildProperties.Entry entry ->
            project.afterEvaluate {
                target.buildConfigField 'String', name ?: formatBuildConfigField(entry.key), "\"${entry.string}\""
            }
        }
        target.ext.buildConfigProperties = { BuildProperties buildProperties ->
            buildProperties.allEntries().each { BuildProperties.Entry entry ->
                target.ext.buildConfigProperty entry
            }
        }
    }

    private String formatBuildConfigField(String name) {
        return splitTokens(name)
                .toUpperCase()
    }

    private String splitTokens(String name) {
        return name
                .replace('.', '_')
                .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
                .join('_')
    }

    private void addResValueSupportTo(target, Project project) {
        def resValue = { String type, String name, Closure<String> value ->
            target.resValue type, name, value()
        }
        target.ext.resValueBoolean = { String name, Boolean value ->
            resValue('bool', name, { Boolean.toString(value) })
        }
        target.ext.resValueInt = { String name, Integer value ->
            resValue('integer', name, { Integer.toString(value) })
        }
        target.ext.resValueString = { String name, String value ->
            resValue('string', name, { "\"$value\"" })
        }
        target.ext.resValueProperty = { String name = null, BuildProperties.Entry entry ->
            project.afterEvaluate {
                target.resValue 'string', name ?: formatResValueName(entry.key), "\"${entry.string}\""
            }
        }
        target.ext.resValueProperties = { BuildProperties buildProperties ->
            buildProperties.allEntries().each { BuildProperties.Entry entry ->
                target.ext.resValueProperty entry
            }
        }
    }

    private String formatResValueName(String name) {
        return splitTokens(name)
                .toLowerCase()
    }

    private void addSigningConfigSupportTo(target, Project project) {
        target.ext.signingConfigProperties = { BuildProperties buildProperties ->
            project.afterEvaluate {
                target.storeFile new File(buildProperties.parentFile, buildProperties['storeFile'].string)
                target.storePassword buildProperties['storePassword'].string
                target.keyAlias buildProperties['keyAlias'].string
                target.keyPassword buildProperties['keyPassword'].string
            }
        }
    }

}