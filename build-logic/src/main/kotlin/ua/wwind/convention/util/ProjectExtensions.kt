package ua.wwind.convention.util

import groovy.transform.CompileDynamic
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

@CompileDynamic
fun getDependencyProject(project: Project, dep: ProjectDependency): Project = project.project(dep.path)
