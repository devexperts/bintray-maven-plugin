package com.devexperts.bintray;

/*
 * #%L
 * bintray-maven-plugin
 * %%
 * Copyright (C) 2015 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.deploy.AbstractDeployMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, requiresProject = true)
public class BintrayDeployMojo extends AbstractDeployMojo {

    /**
     * Bintray user for authentication.
     */
    @Parameter(property = "bintray.user", readonly = true)
    private String user;
    /**
     * Bintray API key for authentication.
     */
    @Parameter(property = "bintray.key", readonly = true)
    private String key;
    /**
     * The id can be used to pick up the correct credentials from the settings.xml.
     */
    @Parameter(property = "bintray.repository.id", required = true, readonly = true)
    private String id;
    /**
     * The location of maven repository in Bintray.
     */
    @Parameter(property = "bintray.repository.url", required = true, readonly = true)
    private String url;
    /**
     * Set this to {@code true} to bypass artifact deploy.
     */
    @Parameter(property = "maven.deploy.skip", defaultValue = "false", readonly = true)
    private boolean skip;
    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing.
     * If a value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     */
    @Parameter(property = "retryFailedDeploymentCount", defaultValue = "1", readonly = true)
    private int retryFailedDeploymentCount;


    /**
     * Maven project to be processed.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    /**
     * Factory used to create a repository.
     */
    @Component
    private ArtifactRepositoryFactory repositoryFactory;
    /**
     * Map that contains the repository layouts.
     */
    @Component(role = ArtifactRepositoryLayout.class)
    private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip)
            return;
        // Create list of artifacts to be deployed.
        List<Artifact> artifacts = new ArrayList<>();
        artifacts.add(project.getArtifact());
        artifacts.addAll(project.getAttachedArtifacts());
        // Create artifact repository.
        ArtifactRepository r = repositoryFactory.createDeploymentArtifactRepository(
                id, url, repositoryLayouts.get("default"), false
        );
        // Set authentication if user and key are passed to properties.
        if (user != null && key != null)
            r.setAuthentication(new Authentication(user, key));
        // Deploy artifacts.
        for (Artifact a : artifacts) {
            try {
                deploy(a.getFile(), a, r, getLocalRepository(), retryFailedDeploymentCount);
            } catch (ArtifactDeploymentException e) {
                throw new MojoFailureException("Error occurred deploying the artifact", e);
            }
        }
    }
}