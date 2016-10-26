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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

@Mojo(name = "publish", defaultPhase = LifecyclePhase.DEPLOY, requiresProject = true)
public class BintrayPublishMojo extends AbstractMojo {

    private static final String SETTINGS_SECURITY_FILE = "settings-security.xml";
    private static final String MAVEN_HOME = "env.M2_HOME";
    private static final String M2 = ".m2";
    //Checks password is encrypted or not.
    private static final Pattern ENCRYPTED_PATTERN = Pattern.compile("\\{.*\\}");

    /**
     * Bintray user for authentication.
     */
    @Parameter(property = "bintray.user")
    private String user;
    /**
     * Bintray API key for authentication.
     */
    @Parameter(property = "bintray.key")
    private String key;
    /**
     * The id can be used to pick up the correct credentials from the settings.xml.
     */
    @Parameter(property = "bintray.repository.id", required = true)
    private String id;
    /**
     * The location of maven repository in Bintray.
     */
    @Parameter(property = "bintray.repository.url", required = true)
    private String url;
    /**
     * Set this to {@code true} to bypass publishing.
     */
    @Parameter(property = "bintray.publish.skip", defaultValue = "false", readonly = true)
    private boolean skip;

    @Parameter(property = "maven.deploy.skip", defaultValue = "false", readonly = true)
    private boolean deploySkip;

    /**
     * Maven project to be processed.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    /**
     * Used to get credentials from settings.xml.
     */
    @Parameter(defaultValue = "${settings}", required = true, readonly = true)
    private Settings settings;
    /**
     * Maven session to be executed.
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || deploySkip)
            return;
        // Create url within Bintray REST API.
        // Replace HOST/maven/.. with HOST/content/.. and add publish command for specified version.
        int i = url.indexOf("://");
        if (i == -1)
            i = 0;
        String commandUrlString = url.substring(0, i) + url.substring(i).replaceFirst("maven", "content")
                + "/" + project.getVersion() + "/publish";
        try {
            // Create url.
            URL commandUrl = new URL(commandUrlString);
            // Log.
            getLog().info("Publishing: " + commandUrl);
            // Create connection.
            HttpURLConnection connection = (HttpURLConnection) commandUrl.openConnection();
            connection.setRequestMethod("POST");
            addAuthorization(connection);
            // Check that executed successfully.
            if (connection.getResponseCode() == 200) {
                getLog().info("Published: " + commandUrl);
            } else { // Failure.
                throw new MojoFailureException(connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            throw new MojoFailureException("Invalid URL: " + commandUrlString, e);
        } catch (IOException e) {
            throw new MojoFailureException("I/O exception occurred", e);
        }
    }

    private void addAuthorization(HttpURLConnection connection) {
        String username = user;
        String password = key;
        if (username == null || password == null) {
            Server server = settings.getServer(id);
            if (server != null) {
                username = server.getUsername();
                password = server.getPassword();
            }
        }
        // Decrypt password if needed.
        if (password != null && ENCRYPTED_PATTERN.matcher(password).matches()) {
            try {
                try {
                    PlexusCipher plexusCipher = new DefaultPlexusCipher();
                    String plainTextMasterPassword = plexusCipher.decryptDecorated(
                            getSettingsSecurity().getMaster(), DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
                    password = plexusCipher.decryptDecorated(password, plainTextMasterPassword);
                } catch (PlexusCipherException e) {
                    getLog().error("Error decrypting password", e);
                }
            } catch (SecDispatcherException e) {
                e.printStackTrace();
            }
        }
        // Add credentials as connection property.
        String credentials = username + ":" + password;
        String authorization = "Basic " + new BASE64Encoder().encode(credentials.getBytes());
        connection.setRequestProperty("Authorization", authorization);
    }

    private SettingsSecurity getSettingsSecurity() throws SecDispatcherException {
        // Try to load settings-security.xml from the system properties.
        File settingsSecurity = getSettingsSecurityFile(
                session.getUserProperties().getProperty(DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION)
        );
        if (settingsSecurity == null) {
            // Try to load settings-security.xml from the user home.
            settingsSecurity = getSettingsSecurityFile(System.getProperty("user.home") + File.separator + M2);
            if (settingsSecurity == null) {
                // Try to load settings-security.xml from the maven home.
                settingsSecurity = getSettingsSecurityFile(
                        session.getUserProperties().getProperty(MAVEN_HOME) + File.separator + M2
                );
            }
        }
        // Check that file is found.
        if (settingsSecurity == null) {
            throw new IllegalStateException("Failed to load " + SETTINGS_SECURITY_FILE);
        }
        // Read SettingsSecurity and return it.
        return SecUtil.read(settingsSecurity.getAbsolutePath(), true);
    }

    private File getSettingsSecurityFile(String path) {
        if (StringUtils.isBlank(path))
            return null;
        File file = new File(path);
        if (file.canExecute() && file.isDirectory())
            file = new File(file, SETTINGS_SECURITY_FILE);
        if (!(file.exists() && file.isFile()))
            return null;
        return file;
    }
}